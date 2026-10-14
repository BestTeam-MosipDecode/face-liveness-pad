/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

class FrameWorkerTest {

    /** Executor whose tasks run only when the test says so. */
    static final class ManualExecutor implements Executor {
        final Deque<Runnable> tasks = new ArrayDeque<>();

        @Override
        public void execute(Runnable task) {
            tasks.add(task);
        }

        void runAll() {
            while (!tasks.isEmpty()) {
                tasks.poll().run();
            }
        }
    }

    @Test
    void skipsFramesWhileAnAnalysisRuns() {
        ManualExecutor executor = new ManualExecutor();
        FrameWorker worker = new FrameWorker(executor, 100);
        List<Long> analysed = new ArrayList<>();
        assertTrue(worker.offerFrame(100, () -> analysed.add(100L)));
        assertFalse(worker.offerFrame(300, () -> analysed.add(300L)));
        executor.runAll();
        assertTrue(worker.offerFrame(400, () -> analysed.add(400L)));
        executor.runAll();
        assertEquals(Arrays.asList(100L, 400L), analysed);
    }

    @Test
    void respectsTheSamplingInterval() {
        FrameWorker worker = new FrameWorker(Runnable::run, 100);
        assertTrue(worker.offerFrame(100, () -> { }));
        assertFalse(worker.offerFrame(150, () -> { }));
        assertTrue(worker.offerFrame(200, () -> { }));
    }

    @Test
    void acceptsATimestampThatGoesBack() {
        FrameWorker worker = new FrameWorker(Runnable::run, 100);
        assertTrue(worker.offerFrame(1000, () -> { }));
        assertTrue(worker.offerFrame(50, () -> { }));
    }

    @Test
    void controlTasksAreNeverSkipped() {
        ManualExecutor executor = new ManualExecutor();
        FrameWorker worker = new FrameWorker(executor, 100);
        worker.offerFrame(100, () -> { });
        worker.run(() -> { });
        assertEquals(2, executor.tasks.size());
    }

    @Test
    void callReturnsTheResult() {
        FrameWorker worker = new FrameWorker(Runnable::run, 100);
        assertEquals(42, (int) worker.call(() -> 42, 1000, -1));
        assertEquals(-1, (int) worker.call(() -> {
            throw new IllegalStateException();
        }, 1000, -1));
    }

    @Test
    void callGivesTheFallbackAfterTheTimeout() {
        FrameWorker worker = new FrameWorker(new ManualExecutor(), 100);
        assertEquals(-1, (int) worker.call(() -> 42, 50, -1));
    }

    @Test
    void callFromTheWorkerThreadRunsInline() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            FrameWorker worker = new FrameWorker(executor, 100);
            long start = System.nanoTime();
            assertEquals(7, (int) worker.call(() -> worker.call(() -> 7, 2000, -1), 3000, -2));
            assertTrue(System.nanoTime() - start < 1_000_000_000L, "no wait for the inner call");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void aFailingTaskReleasesTheWorker() {
        FrameWorker worker = new FrameWorker(Runnable::run, 100);
        assertTrue(worker.offerFrame(100, () -> {
            throw new IllegalStateException("bug");
        }));
        assertTrue(worker.offerFrame(200, () -> { }));
    }

    @Test
    void aClosedWorkerIgnoresEverything() {
        ManualExecutor executor = new ManualExecutor();
        FrameWorker worker = new FrameWorker(executor, 100);
        worker.close();
        assertFalse(worker.offerFrame(100, () -> { }));
        worker.run(() -> { });
        assertTrue(executor.tasks.isEmpty());
        assertEquals(-1, (int) worker.call(() -> 42, 1000, -1));
    }
}
