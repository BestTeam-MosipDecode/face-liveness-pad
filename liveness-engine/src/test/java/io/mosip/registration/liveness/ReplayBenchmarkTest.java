/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import io.mosip.registration.liveness.api.LivenessEngine;
import io.mosip.registration.liveness.api.LivenessEvent;
import io.mosip.registration.liveness.api.LivenessEventListener;
import io.mosip.registration.liveness.api.LivenessMetrics;
import io.mosip.registration.liveness.api.LivenessPolicy;
import io.mosip.registration.liveness.api.LivenessSession;
import io.mosip.registration.liveness.api.LivenessUpdate;
import io.mosip.registration.liveness.api.Workflow;
import io.mosip.registration.liveness.platform.desktop.ImageIoFrameDecoder;

/**
 * Replays recorded sequences through the public API in real time and prints the figures of issue #29: time to
 * the passive decision, mean analysis time, frames analysed and skipped, CPU time and heap of the process.
 *
 * <p>The sequences are folders written by {@code training/scripts/capture_session.py}; they hold faces and stay
 * outside the repository. The test runs only with {@code -Dliveness.replay=<folder>[;<folder>...]} and is skipped
 * otherwise. Frames are submitted at their recorded timestamps, so the session skips frames as it would on a
 * device stream at the same rate. Nothing is written.
 */
class ReplayBenchmarkTest {

    private static final Pattern TIMESTAMPS = Pattern.compile("\"timestamps_ms\"\\s*:\\s*\\[([^\\]]*)\\]");

    @Test
    void replaySequences() throws Exception {
        String property = System.getProperty("liveness.replay");
        assumeTrue(property != null && !property.isEmpty(), "liveness.replay not set");

        com.sun.management.OperatingSystemMXBean os =
                (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        try (LivenessEngine engine = LivenessEngine.create(new ImageIoFrameDecoder())) {
            System.out.println("| Sequence | Frames | Analysed | Decision | Time to decision | Mean analysis | CPU | Heap |");
            System.out.println("| --- | --- | --- | --- | --- | --- | --- | --- |");
            for (String folder : property.split(";")) {
                replay(engine, Paths.get(folder.trim()), os);
            }
            LivenessMetrics m = engine.metrics();
            System.out.printf("%nEngine metrics: %d frames analysed, mean analysis %.1f ms, %d sessions, retry rate %.2f, "
                    + "failure rate %.2f%n", m.framesAnalysed(), m.meanAnalysisMs(), m.sessions(), m.retryRate(),
                    m.failureRate());
        }
    }

    private void replay(LivenessEngine engine, Path folder, com.sun.management.OperatingSystemMXBean os)
            throws Exception {
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder, "frame_*.jpg")) {
            stream.forEach(files::add);
        }
        Collections.sort(files);
        assumeTrue(!files.isEmpty(), "no frame in " + folder);
        double[] timestamps = readTimestamps(folder.resolve("metadata.json"), files.size());
        List<byte[]> frames = new ArrayList<>(files.size());
        for (Path file : files) {
            frames.add(Files.readAllBytes(file));
        }

        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<LivenessUpdate> last = new AtomicReference<>();
        AtomicReference<Long> decisionMs = new AtomicReference<>();
        long[] analysed = new long[1];
        long start = System.nanoTime();
        LivenessPolicy policy = LivenessPolicy.builder().activeEnabled(false).maxAttempts(1).build();
        LivenessEventListener listener = new LivenessEventListener() {
            @Override
            public void onUpdate(LivenessUpdate update) {
                last.set(update);
                analysed[0]++;
                if (update.finished() && decisionMs.get() == null) {
                    decisionMs.set((System.nanoTime() - start) / 1_000_000);
                    finished.countDown();
                }
            }

            @Override
            public void onEvent(LivenessEvent event) {
            }
        };

        System.gc();
        long cpuBefore = os.getProcessCpuTime();
        long wallBefore = System.nanoTime();
        try (LivenessSession session = engine.newSession(Workflow.RESIDENT, policy, listener)) {
            for (int i = 0; i < frames.size() && finished.getCount() > 0; i++) {
                long due = Math.round(timestamps[i] - timestamps[0]);
                long elapsed = (System.nanoTime() - start) / 1_000_000;
                if (due > elapsed) {
                    Thread.sleep(due - elapsed);
                }
                session.submit(frames.get(i).clone(), start / 1_000_000 + due);
            }
            finished.await(2, TimeUnit.SECONDS);
        }
        double wallMs = (System.nanoTime() - wallBefore) / 1e6;
        double cpuPercent = 100.0 * (os.getProcessCpuTime() - cpuBefore) / 1e6 / wallMs;
        Runtime runtime = Runtime.getRuntime();
        long heapMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        LivenessUpdate update = last.get();
        LivenessMetrics m = engine.metrics();
        System.out.printf("| `%s/%s` | %d | %d updates | %s | %s | %.1f ms | %.0f %% of one core | %d MB |%n",
                folder.getParent().getFileName(), folder.getFileName(), frames.size(), analysed[0],
                update == null ? "none" : update.state() + (update.failureReason().isPresent()
                        ? " " + update.failureReason().get() : ""),
                decisionMs.get() == null ? "none" : decisionMs.get() + " ms", m.meanAnalysisMs(), cpuPercent,
                heapMb);
    }

    private static double[] readTimestamps(Path metadata, int count) throws IOException {
        if (Files.isRegularFile(metadata)) {
            Matcher matcher = TIMESTAMPS.matcher(new String(Files.readAllBytes(metadata), "UTF-8"));
            if (matcher.find()) {
                String[] parts = matcher.group(1).trim().split("\\s*,\\s*");
                if (parts.length == count) {
                    double[] values = new double[count];
                    for (int i = 0; i < count; i++) {
                        values[i] = Double.parseDouble(parts[i]);
                    }
                    return values;
                }
            }
        }
        double[] values = new double[count];
        for (int i = 0; i < count; i++) {
            values[i] = i * 100.0;
        }
        return values;
    }
}
