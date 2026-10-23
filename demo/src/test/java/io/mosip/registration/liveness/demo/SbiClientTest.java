/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.demo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

class SbiClientTest {

	private static final byte[] FRAME_A = { (byte) 0xFF, (byte) 0xD8, 1, 2, 3, (byte) 0xFF, (byte) 0xD9 };
	private static final byte[] FRAME_B = { (byte) 0xFF, (byte) 0xD8, 9, (byte) 0xFF, (byte) 0xD9 };

	/** Two parts as the simulated device writes them: boundary, headers, blank line, JPEG, line break. */
	private static InputStream stream(byte[]... frames) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write("HTTP/1.1 200 OK\r\nContent-Type: multipart/x-mixed-replace; boundary=--BoundaryString\r\n\r\n"
				.getBytes(StandardCharsets.US_ASCII));
		for (byte[] frame : frames) {
			out.write(("--BoundaryString\r\nContent-type: image/jpeg\r\nContent-Length: " + frame.length + "\r\n\r\n")
					.getBytes(StandardCharsets.US_ASCII));
			out.write(frame);
			out.write("\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
		}
		return new ByteArrayInputStream(out.toByteArray());
	}

	@Test
	void readsEachFrameOfTheStream() throws IOException {
		InputStream in = stream(FRAME_A, FRAME_B);
		assertArrayEquals(FRAME_A, SbiClient.readFrame(in));
		assertArrayEquals(FRAME_B, SbiClient.readFrame(in));
		assertNull(SbiClient.readFrame(in));
	}

	@Test
	void readsThePassiveScoreThatTheDeviceSendsWithAFrame() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(("--BoundaryString\r\nContent-Type:image/jpeg\r\nX-Liveness-Score:0.9731\r\nContent-Length:"
				+ FRAME_A.length + "\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
		out.write(FRAME_A);
		out.write("\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
		out.write(stream(FRAME_B).readAllBytes());
		InputStream in = new ByteArrayInputStream(out.toByteArray());
		SbiClient.Frame scored = SbiClient.readScoredFrame(in);
		assertArrayEquals(FRAME_A, scored.image);
		assertEquals(0.9731, scored.livenessScore, 0.0);
		SbiClient.Frame unscored = SbiClient.readScoredFrame(in);
		assertArrayEquals(FRAME_B, unscored.image);
		assertNull(unscored.livenessScore);
		assertNull(SbiClient.livenessScore("X-Liveness-Score:NaN\r\n"));
	}

	@Test
	void aTruncatedFrameEndsTheStream() throws IOException {
		byte[] whole = stream(FRAME_A).readAllBytes();
		byte[] truncated = java.util.Arrays.copyOf(whole, whole.length - 6);
		assertNull(SbiClient.readFrame(new ByteArrayInputStream(truncated)));
	}

	@Test
	void readsThePayloadOfACompactJws() throws IOException {
		String payload = Base64.getUrlEncoder().withoutPadding()
				.encodeToString("{\"bioValue\":\"AQID\",\"qualityScore\":\"44\"}".getBytes(StandardCharsets.UTF_8));
		assertEquals("44", SbiClient.jwsPayload("eyJhbGciOiJSUzI1NiJ9." + payload + ".c2ln").path("qualityScore").asText());
	}

	@Test
	void refusesCaptureDataThatIsNotAJws() {
		assertThrows(IOException.class, () -> SbiClient.jwsPayload("not-a-jws"));
	}
}
