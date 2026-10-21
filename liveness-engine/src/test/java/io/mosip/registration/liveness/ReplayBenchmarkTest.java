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
import java.util.Locale;
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
import io.mosip.registration.liveness.api.SessionState;
import io.mosip.registration.liveness.api.Workflow;
import io.mosip.registration.liveness.active.ChallengeType;
import io.mosip.registration.liveness.platform.desktop.ImageIoFrameDecoder;

/**
 * Replays recorded sequences through the public API in real time and prints the figures of issue #29: time to
 * the passive decision, mean analysis time, frames analysed and skipped, CPU time and heap of the process.
 *
 * <p>The sequences are folders written by {@code training/scripts/capture_session.py}; they hold faces and stay
 * outside the repository. The test runs only with {@code -Dliveness.replay=<folder>[;<folder>...]} and is skipped
 * otherwise. Frames are submitted at their recorded timestamps, so the session skips frames as it would on a
 * device stream at the same rate. Nothing is written.
 *
 * <p>A folder whose type is {@code action_blink}, {@code action_turn_left} or {@code action_turn_right} runs the
 * active check: the pass threshold is raised to 1.0 so that the passive check sends the session to a challenge, and
 * the challenge pool holds the recorded action only. The row then also gives the time from the start of the
 * challenge to the decision.
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
            System.out.println("| Sequence | Frames | Updates | Check | Decision | Time to decision | Challenge time "
                    + "| Mean analysis of the sequence | CPU | Heap |");
            System.out.println("| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |");
            for (String folder : property.split(";")) {
                replay(engine, Paths.get(folder.trim()), os);
            }
            LivenessMetrics m = engine.metrics();
            System.out.printf(Locale.ROOT, "%nEngine metrics: %d frames analysed, mean analysis %.1f ms, %d sessions, retry rate %.2f, "
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
        AtomicReference<Long> challengeStartMs = new AtomicReference<>();
        long[] analysed = new long[1];
        long start = System.nanoTime();
        ChallengeType action = actionOf(folder);
        LivenessPolicy.Builder builder = LivenessPolicy.builder().maxAttempts(1);
        if (action == null) {
            builder.activeEnabled(false);
        } else {
            builder.passThreshold(1.0).minChallenges(1).challengeTypes(Collections.singleton(action));
        }
        LivenessPolicy policy = builder.build();
        LivenessEventListener listener = new LivenessEventListener() {
            @Override
            public void onUpdate(LivenessUpdate update) {
                last.set(update);
                analysed[0]++;
                long now = (System.nanoTime() - start) / 1_000_000;
                if (update.state() == SessionState.ACTIVE_CHALLENGE && challengeStartMs.get() == null) {
                    challengeStartMs.set(now);
                }
                boolean decided = update.state() == SessionState.PASSED || update.state() == SessionState.FAILED;
                if (decided && decisionMs.get() == null) {
                    decisionMs.set(now);
                    finished.countDown();
                }
            }

            @Override
            public void onEvent(LivenessEvent event) {
            }
        };

        LivenessMetrics before = engine.metrics();
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
        long sequenceFrames = m.framesAnalysed() - before.framesAnalysed();
        double sequenceMs = sequenceFrames == 0 ? 0 : (m.meanAnalysisMs() * m.framesAnalysed()
                - before.meanAnalysisMs() * before.framesAnalysed()) / sequenceFrames;
        String challenge = "none";
        if (challengeStartMs.get() != null) {
            challenge = decisionMs.get() == null ? "not decided" : (decisionMs.get() - challengeStartMs.get()) + " ms";
        }
        System.out.printf(Locale.ROOT, "| `%s/%s` | %d | %d | %s | %s | %s | %s | %.1f ms | %.0f %% of one core | %d MB |%n",
                folder.getParent().getFileName(), folder.getFileName(), frames.size(), analysed[0],
                action == null ? "passive" : "active " + action,
                update == null ? "none" : update.state() + (update.failureReason().isPresent()
                        ? " " + update.failureReason().get() : ""),
                decisionMs.get() == null ? "none" : decisionMs.get() + " ms", challenge, sequenceMs,
                cpuPercent, heapMb);
    }

    /** The recorded action of an {@code action_*} sequence folder, or null for the passive check. */
    static ChallengeType actionOf(Path folder) {
        String type = folder.getParent() == null ? "" : folder.getParent().getFileName().toString();
        switch (type) {
            case "action_blink":
                return ChallengeType.BLINK;
            case "action_smile":
                return ChallengeType.SMILE;
            case "action_turn_left":
                return ChallengeType.TURN_LEFT;
            case "action_turn_right":
                return ChallengeType.TURN_RIGHT;
            default:
                return null;
        }
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
