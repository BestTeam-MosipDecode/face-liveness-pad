/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.demo;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;
import java.util.function.BiConsumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * SBI client for a face device on this workstation, with the calls of the Registration Client: MOSIPDISC to find the
 * device, STREAM for the preview frames, RCAPTURE for the signed capture. SBI uses its own HTTP methods, which
 * {@code HttpURLConnection} refuses, so requests are written on a plain socket.
 */
final class SbiClient {

	static final String HOST = "127.0.0.1";
	private static final int CONNECT_TIMEOUT_MS = 2000;
	private static final int READ_TIMEOUT_MS = 15000;
	/** The device closes the stream after this time; one attempt never comes close to it. */
	private static final String STREAM_TIMEOUT_MS = "300000";
	private static final DateTimeFormatter CAPTURE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
			.withZone(ZoneOffset.UTC);
	private static final ObjectMapper JSON = new ObjectMapper();

	private final int port;
	private final String deviceId;
	private final String specVersion;
	private volatile Socket streamSocket;

	private SbiClient(int port, String deviceId, String specVersion) {
		this.port = port;
		this.deviceId = deviceId;
		this.specVersion = specVersion;
	}

	/** The first face device that answers MOSIPDISC on a port of the range, as the client scans it. */
	static SbiClient discover(int firstPort, int lastPort) throws IOException {
		ObjectNode request = JSON.createObjectNode().put("type", "Face");
		for (int port = firstPort; port <= lastPort; port++) {
			try {
				JsonNode devices = call(port, "MOSIPDISC", "/device", request);
				if (devices.isArray() && devices.size() > 0) {
					JsonNode device = devices.get(0);
					JsonNode versions = device.path("specVersion");
					String version = versions.isArray() && versions.size() > 0 ? versions.get(0).asText() : "0.9.5";
					return new SbiClient(port, device.path("deviceId").asText(), version);
				}
			} catch (IOException noDeviceOnThisPort) {
				// next port
			}
		}
		throw new IOException("No face device answers on ports " + firstPort + " to " + lastPort);
	}

	int port() {
		return port;
	}

	String specVersion() {
		return specVersion;
	}

	/**
	 * Reads the stream until it ends or {@link #stopStream()} is called, and hands each JPEG frame to
	 * {@code onFrame} on the calling thread, with the passive liveness score the device sent with it, or null.
	 */
	void stream(BiConsumer<byte[], Double> onFrame) throws IOException {
		ObjectNode request = JSON.createObjectNode().put("deviceId", deviceId).put("deviceSubId", "0")
				.put("timeout", STREAM_TIMEOUT_MS);
		try (Socket socket = open()) {
			streamSocket = socket;
			send(socket, "STREAM", "/stream", request);
			InputStream in = new BufferedInputStream(socket.getInputStream());
			Frame frame;
			while ((frame = readScoredFrame(in)) != null) {
				onFrame.accept(frame.image, frame.livenessScore);
			}
		} finally {
			streamSocket = null;
		}
	}

	/** Ends a stream in progress; {@link #stream} then returns or throws. */
	void stopStream() {
		Socket socket = streamSocket;
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException ignored) {
				// already closed
			}
		}
	}

	/** RCAPTURE of one face: the ISO/IEC 19794-5 record from the signed response. */
	Capture capture() throws IOException {
		String transactionId = UUID.randomUUID().toString();
		ObjectNode request = JSON.createObjectNode().put("env", "Developer").put("purpose", "Registration")
				.put("specVersion", specVersion).put("timeout", 10000)
				.put("captureTime", CAPTURE_TIME.format(Instant.now())).put("transactionId", transactionId);
		ObjectNode bio = request.putArray("bio").addObject().put("type", "Face").put("count", 1)
				.put("requestedScore", 40).put("deviceId", deviceId).put("deviceSubId", "0").put("previousHash", "");
		bio.putArray("exception");
		JsonNode biometric = call(port, "RCAPTURE", "/capture", request).path("biometrics").path(0);
		String errorCode = biometric.path("error").path("errorCode").asText();
		if (!"0".equals(errorCode)) {
			throw new IOException("The device answered the capture with error " + errorCode);
		}
		JsonNode data = jwsPayload(biometric.path("data").asText());
		byte[] record = Base64.getUrlDecoder().decode(data.path("bioValue").asText());
		return new Capture(record, data.path("qualityScore").asText(), !biometric.path("hash").asText().isEmpty());
	}

	/** Result of a capture: the face record, its quality score and whether the response carries its hash chain. */
	static final class Capture {
		private final byte[] record;
		private final String qualityScore;
		private final boolean hashed;

		Capture(byte[] record, String qualityScore, boolean hashed) {
			this.record = record;
			this.qualityScore = qualityScore;
			this.hashed = hashed;
		}

		int size() {
			return record.length;
		}

		/** An ISO/IEC 19794-5 face record starts with "FAC", a zero byte, then the version "030". */
		boolean isFaceRecord() {
			return record.length > 8 && record[0] == 'F' && record[1] == 'A' && record[2] == 'C' && record[3] == 0;
		}

		String qualityScore() {
			return qualityScore;
		}

		boolean hashed() {
			return hashed;
		}
	}

	/** One frame of the stream, with the passive liveness score the device sent in its header, or null. */
	static final class Frame {
		final byte[] image;
		final Double livenessScore;

		Frame(byte[] image, Double livenessScore) {
			this.image = image;
			this.livenessScore = livenessScore;
		}
	}

	/** Next frame of the stream without its score, or null at the end of the stream. */
	static byte[] readFrame(InputStream in) throws IOException {
		Frame frame = readScoredFrame(in);
		return frame == null ? null : frame.image;
	}

	/**
	 * Next frame of the stream, with the steps of {@code Streamer.readFrame} of the client: the part header up to
	 * {@code Content-Length:}, with the {@code X-Liveness-Score} of a device that scores its frames, the length, the
	 * first 0xFF byte, then the frame. Null at the end of the stream.
	 */
	static Frame readScoredFrame(InputStream in) throws IOException {
		byte[] marker = "Content-Length:".getBytes(StandardCharsets.US_ASCII);
		StringBuilder header = new StringBuilder();
		int matched = 0;
		while (matched < marker.length) {
			int b = in.read();
			if (b < 0) {
				return null;
			}
			if (header.length() < 1024) {
				header.append((char) b);
			}
			matched = b == marker[matched] ? matched + 1 : (b == marker[0] ? 1 : 0);
		}
		StringBuilder digits = new StringBuilder();
		int b;
		while ((b = in.read()) >= 0 && b != '\r' && b != '\n') {
			digits.append((char) b);
		}
		if (b < 0) {
			return null;
		}
		int length = Integer.parseInt(digits.toString().trim());
		while ((b = in.read()) >= 0 && b != 0xFF) {
			// header lines and the blank line before the frame
		}
		if (b < 0) {
			return null;
		}
		byte[] frame = new byte[length];
		frame[0] = (byte) 0xFF;
		int read = 1;
		while (read < length) {
			int n = in.read(frame, read, length - read);
			if (n < 0) {
				return null;
			}
			read += n;
		}
		return new Frame(frame, livenessScore(header.toString()));
	}

	/** The value of {@code X-Liveness-Score} in a frame header, or null, as {@code Streamer.livenessScore} reads it. */
	static Double livenessScore(String frameHeader) {
		String name = "X-Liveness-Score:";
		int start = frameHeader.indexOf(name);
		if (start < 0) {
			return null;
		}
		start += name.length();
		int end = start;
		while (end < frameHeader.length() && frameHeader.charAt(end) != '\r' && frameHeader.charAt(end) != '\n') {
			end++;
		}
		try {
			double score = Double.parseDouble(frameHeader.substring(start, end).trim());
			return Double.isFinite(score) ? score : null;
		} catch (NumberFormatException exception) {
			return null;
		}
	}

	/** Payload of a compact JWS, without checking the signature: the client checks it against the device trust. */
	static JsonNode jwsPayload(String jws) throws IOException {
		String[] parts = jws.split("\\.");
		if (parts.length != 3) {
			throw new IOException("The capture data is not a compact JWS");
		}
		return JSON.readTree(Base64.getUrlDecoder().decode(parts[1]));
	}

	private Socket open() throws IOException {
		return open(port);
	}

	private static Socket open(int port) throws IOException {
		Socket socket = new Socket();
		socket.connect(new InetSocketAddress(HOST, port), CONNECT_TIMEOUT_MS);
		socket.setSoTimeout(READ_TIMEOUT_MS);
		return socket;
	}

	private static void send(Socket socket, String method, String path, JsonNode body) throws IOException {
		byte[] content = JSON.writeValueAsBytes(body);
		String head = method + " " + path + " HTTP/1.1\r\nHost: " + HOST + "\r\nContent-Type: application/json\r\n"
				+ "Content-Length: " + content.length + "\r\nConnection: close\r\n\r\n";
		OutputStream out = socket.getOutputStream();
		out.write(head.getBytes(StandardCharsets.US_ASCII));
		out.write(content);
		out.flush();
	}

	private static JsonNode call(int port, String method, String path, JsonNode body) throws IOException {
		try (Socket socket = open(port)) {
			send(socket, method, path, body);
			ByteArrayOutputStream response = new ByteArrayOutputStream();
			InputStream in = socket.getInputStream();
			byte[] buffer = new byte[8192];
			int n;
			while ((n = in.read(buffer)) >= 0) {
				response.write(buffer, 0, n);
			}
			String text = new String(response.toByteArray(), StandardCharsets.UTF_8);
			int bodyStart = text.indexOf("\r\n\r\n");
			if (!text.startsWith("HTTP/") || bodyStart < 0) {
				throw new IOException("Not an HTTP answer on port " + port);
			}
			return JSON.readTree(text.substring(bodyStart + 4));
		}
	}
}
