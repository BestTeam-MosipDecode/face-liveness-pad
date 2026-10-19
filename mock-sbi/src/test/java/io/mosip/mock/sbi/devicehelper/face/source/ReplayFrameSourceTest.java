/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.mock.sbi.devicehelper.face.source;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.mosip.mock.sbi.devicehelper.face.source.ReplayFrameSource.End;

class ReplayFrameSourceTest {

	Path folder;

	@BeforeEach
	void createFolder() throws IOException {
		folder = Files.createTempDirectory("replay-test");
	}

	@AfterEach
	void deleteFolder() throws IOException {
		try (java.util.stream.Stream<Path> files = Files.list(folder)) {
			for (Path file : (Iterable<Path>) files::iterator)
				Files.deleteIfExists(file);
		}
		Files.deleteIfExists(folder);
	}

	/** Three "frames" whose first byte is their index, with timestamps 0, 100 and 300 ms. */
	private void writeSequence(boolean metadata) throws IOException {
		for (int i = 0; i < 3; i++) {
			byte[] frame = new byte[64];
			frame[0] = (byte) i;
			Files.write(folder.resolve(String.format("frame_%05d.jpg", i)), frame);
		}
		Files.write(folder.resolve("notes.txt"), "ignored".getBytes(StandardCharsets.UTF_8));
		if (metadata)
			Files.write(folder.resolve("metadata.json"),
					"{\"subject\": \"S01\", \"timestamps_ms\": [0.0, 100.0, 300.0], \"frame_count\": 3}"
							.getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void framesAreDueAtTheRecordedTimestamps() throws Exception {
		writeSequence(true);
		ReplayFrameSource source = new ReplayFrameSource(folder, 0, End.LOOP, 0);
		source.load();
		assertEquals(0, source.indexAt(0));
		assertEquals(0, source.indexAt(99));
		assertEquals(1, source.indexAt(100));
		assertEquals(1, source.indexAt(299));
		assertEquals(2, source.indexAt(300));
		// The last frame lasts the mean interval, 150 ms, then the sequence loops.
		assertEquals(2, source.indexAt(449));
		assertEquals(0, source.indexAt(450));
		assertEquals(1, source.indexAt(550));
	}

	@Test
	void fixedRateWithoutMetadata() throws Exception {
		writeSequence(false);
		ReplayFrameSource source = new ReplayFrameSource(folder, 0, End.HOLD, 0);
		source.load();
		assertEquals(0, source.indexAt(50));
		assertEquals(1, source.indexAt(100));
		assertEquals(2, source.indexAt(200));
		assertEquals(2, source.indexAt(10_000));
	}

	@Test
	void fixedRateOverridesMetadata() throws Exception {
		writeSequence(true);
		ReplayFrameSource source = new ReplayFrameSource(folder, 20, End.STOP, 0);
		source.load();
		assertEquals(1, source.indexAt(50));
		assertEquals(2, source.indexAt(100));
		assertEquals(-1, source.indexAt(150));
	}

	@Test
	void stopReturnsNoFrameAfterTheSequence() throws Exception {
		writeSequence(true);
		ReplayFrameSource source = new ReplayFrameSource(folder, 0, End.STOP, 0);
		source.prepare();
		source.open();
		byte[] first = source.nextFrame();
		assertNotNull(first);
		assertEquals(0, first[0]);
		Thread.sleep(600);
		assertNull(source.nextFrame());
		source.close();
		assertNull(source.nextFrame());
	}

	@Test
	void everyNthFrameIsTruncated() throws Exception {
		writeSequence(true);
		ReplayFrameSource source = new ReplayFrameSource(folder, 0, End.HOLD, 2);
		source.open();
		assertEquals(64, source.nextFrame().length);
		assertEquals(16, source.nextFrame().length);
		assertEquals(64, source.nextFrame().length);
	}

	@Test
	void timestampsAreReadFromMetadata() throws Exception {
		writeSequence(true);
		assertArrayEquals(new double[] {0.0, 100.0, 300.0},
				ReplayFrameSource.readTimestamps(folder.resolve("metadata.json")));
		assertNull(ReplayFrameSource.readTimestamps(folder.resolve("missing.json")));
	}

	@Test
	void scheduleFallsBackWhenTheCountDiffers() {
		assertArrayEquals(new long[] {0, 100, 200}, ReplayFrameSource.schedule(3, new double[] {0, 50}, 0));
		assertArrayEquals(new long[] {0, 5, 10}, ReplayFrameSource.schedule(3, new double[] {40, 45, 50}, 0));
	}

	@Test
	void missingFolderOrFramesAreReported() {
		assertThrows(IllegalStateException.class,
				() -> new ReplayFrameSource(folder.resolve("none"), 0, End.LOOP, 0).load());
		assertThrows(IllegalStateException.class, () -> new ReplayFrameSource(folder, 0, End.LOOP, 0).load());
	}
}
