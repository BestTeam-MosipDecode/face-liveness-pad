/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.session;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

/** The engine must not close the models while a worker is still inside an inference. */
class WorkerShutdownTest {

    @Test
    void reportsAWorkerThatIsStillRunning() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        executor.execute(() -> {
            started.countDown();
            // Native inference ignores interrupts: wait without giving up on them.
            while (true) {
                try {
                    release.await();
                    return;
                } catch (InterruptedException e) {
                    // keep running, like OrtSession.run
                }
            }
        });
        assertTrue(started.await(2, TimeUnit.SECONDS));
        assertFalse(DefaultLivenessEngine.stopAndWait(executor, 100), "still inside the task");
        release.countDown();
        assertTrue(DefaultLivenessEngine.stopAndWait(executor, 2000));
    }

    @Test
    void anIdleWorkerStopsAtOnce() {
        assertTrue(DefaultLivenessEngine.stopAndWait(Executors.newSingleThreadExecutor(), 100));
        assertTrue(DefaultLivenessEngine.stopAndWait(null, 100));
    }
}
