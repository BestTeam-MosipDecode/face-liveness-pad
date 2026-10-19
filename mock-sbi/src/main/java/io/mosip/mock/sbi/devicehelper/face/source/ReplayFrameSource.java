/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.mock.sbi.devicehelper.face.source;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Frames replayed from a folder of JPEG files, as written by {@code training/scripts/capture_session.py}
 * ({@code frame_00000.jpg}, {@code frame_00001.jpg}... and {@code metadata.json} with the timestamps).
 *
 * The sequence is paced on the wall clock from {@link #open()}: {@link #nextFrame()} returns the frame
 * due at the current time, so a stream at 30 requests per second sees each frame about three times and
 * a slow stream skips frames, as a camera would. Frames are read once, at {@link #prepare()}.
 *
 * Failure simulation: {@code end = STOP} returns no frame once the sequence is over, as an unplugged
 * device; {@code corruptEvery = N} truncates every Nth returned frame to a few bytes.
 */
public class ReplayFrameSource implements FaceFrameSource {
	private static final Logger LOGGER = LoggerFactory.getLogger(ReplayFrameSource.class);
	private static final double DEFAULT_FPS = 10;
	private static final int CORRUPT_LENGTH = 16;
	private static final Pattern TIMESTAMPS = Pattern.compile("\"timestamps_ms\"\\s*:\\s*\\[([^\\]]*)\\]");

	/** What happens after the last frame. */
	public enum End {
		LOOP, HOLD, STOP
	}

	private final Path folder;
	private final double fps;
	private final End end;
	private final int corruptEvery;

	private List<byte[]> frames = Collections.emptyList();
	private long[] dueMs = new long[0];
	private long startMs;
	private long served;
	private volatile boolean open;

	public ReplayFrameSource(Path folder, double fps, End end, int corruptEvery) {
		this.folder = folder;
		this.fps = fps;
		this.end = end;
		this.corruptEvery = corruptEvery;
	}

	@Override
	public void prepare() throws Exception {
		load();
		LOGGER.info("Replay of {} frames from {}, {} ms long, end {}", frames.size(), folder,
				dueMs.length == 0 ? 0 : dueMs[dueMs.length - 1], end);
	}

	void load() throws IOException {
		if (folder == null || folder.toString().isEmpty() || !Files.isDirectory(folder))
			throw new IllegalStateException("Replay folder not found: " + folder);
		List<Path> files = new ArrayList<>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder, "frame_*.jpg")) {
			stream.forEach(files::add);
		}
		Collections.sort(files);
		if (files.isEmpty())
			throw new IllegalStateException("No frame_*.jpg file in " + folder);
		List<byte[]> loaded = new ArrayList<>(files.size());
		for (Path file : files)
			loaded.add(Files.readAllBytes(file));
		frames = loaded;
		dueMs = schedule(files.size(), fps > 0 ? null : readTimestamps(folder.resolve("metadata.json")), fps);
	}

	/** Timestamps of metadata.json, or null when the file or the field is missing or the count differs. */
	static double[] readTimestamps(Path metadata) {
		if (!Files.isRegularFile(metadata))
			return null;
		try {
			Matcher matcher = TIMESTAMPS.matcher(new String(Files.readAllBytes(metadata), "UTF-8"));
			if (!matcher.find())
				return null;
			String[] parts = matcher.group(1).trim().split("\\s*,\\s*");
			double[] values = new double[parts.length];
			for (int i = 0; i < parts.length; i++)
				values[i] = Double.parseDouble(parts[i]);
			return values;
		} catch (IOException | NumberFormatException ex) {
			LOGGER.warn("Cannot read the timestamps of {}: {}", metadata, ex.toString());
			return null;
		}
	}

	/** Time at which each frame is due, from the first frame, in milliseconds. */
	static long[] schedule(int count, double[] timestamps, double fps) {
		long[] due = new long[count];
		if (timestamps != null && timestamps.length == count && count > 0) {
			for (int i = 0; i < count; i++)
				due[i] = Math.round(timestamps[i] - timestamps[0]);
		} else {
			double interval = 1000 / (fps > 0 ? fps : DEFAULT_FPS);
			for (int i = 0; i < count; i++)
				due[i] = Math.round(i * interval);
		}
		return due;
	}

	@Override
	public synchronized void open() throws Exception {
		if (frames.isEmpty())
			load();
		startMs = System.currentTimeMillis();
		served = 0;
		open = true;
	}

	@Override
	public byte[] nextFrame() {
		if (!open)
			return null;
		int index = indexAt(System.currentTimeMillis() - startMs);
		if (index < 0)
			return null;
		served++;
		byte[] frame = frames.get(index);
		if (corruptEvery > 0 && served % corruptEvery == 0)
			return Arrays.copyOf(frame, Math.min(CORRUPT_LENGTH, frame.length));
		return frame;
	}

	/** Index of the frame due at {@code elapsedMs}, or -1 when the sequence is over and {@code end} is STOP. */
	int indexAt(long elapsedMs) {
		long length = dueMs[dueMs.length - 1] + intervalMs();
		if (elapsedMs >= length) {
			switch (end) {
				case LOOP:
					elapsedMs = elapsedMs % length;
					break;
				case HOLD:
					return dueMs.length - 1;
				default:
					return -1;
			}
		}
		int index = Arrays.binarySearch(dueMs, elapsedMs);
		if (index < 0)
			index = -index - 2;
		return Math.max(0, index);
	}

	/** Duration of the last frame: the mean interval, so that a loop keeps its rhythm. */
	private long intervalMs() {
		return dueMs.length > 1 ? Math.max(1, dueMs[dueMs.length - 1] / (dueMs.length - 1)) : Math.round(1000 / DEFAULT_FPS);
	}

	@Override
	public synchronized void close() {
		open = false;
	}
}
