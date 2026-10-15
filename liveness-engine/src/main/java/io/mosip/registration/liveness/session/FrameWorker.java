/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.session;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Single analysis thread of a session (section 3.1 of docs/decision-flow.md). Frames are skipped while one is
 * analysed or when they come less than {@code samplingMs} after the last one; other tasks are never skipped.
 */
final class FrameWorker {

    private final Executor executor;
    private final long samplingMs;
    private static final int QUEUED = 0;
    private static final int STARTED = 1;
    private static final int ABANDONED = 2;

    private final AtomicBoolean busy = new AtomicBoolean();
    private volatile boolean hasFrame;
    private volatile long lastFrameT;
    private volatile boolean closed;
    private volatile Thread workerThread;

    FrameWorker(Executor executor, long samplingMs) {
        this.executor = executor;
        this.samplingMs = samplingMs;
    }

    /** Queues a frame task unless it must be skipped. A timestamp that goes back is never skipped. */
    boolean offerFrame(long t, Runnable task) {
        if (closed || !busy.compareAndSet(false, true)) {
            return false;
        }
        if (hasFrame && t >= lastFrameT && t - lastFrameT < samplingMs) {
            busy.set(false);
            return false;
        }
        lastFrameT = t;
        hasFrame = true;
        if (!execute(() -> {
            try {
                task.run();
            } finally {
                busy.set(false);
            }
        })) {
            busy.set(false);
            return false;
        }
        return true;
    }

    /** Queues a task that is never skipped. */
    void run(Runnable task) {
        if (!closed) {
            execute(task);
        }
    }

    /**
     * Runs a task on the worker and waits for its result. From the worker thread itself (a listener calling back),
     * the task runs at once. Gives {@code fallback} when closed, on timeout or when the task throws.
     */
    <T> T call(Callable<T> task, long timeoutMs, T fallback) {
        if (closed) {
            return fallback;
        }
        if (Thread.currentThread() == workerThread) {
            try {
                return task.call();
            } catch (Exception e) {
                return fallback;
            }
        }
        // A task the caller gave up on before it started never runs. A task already started goes to the end and its
        // result is returned: the session has acted on it, so the caller must see what it did.
        AtomicInteger stage = new AtomicInteger(QUEUED);
        FutureTask<T> future = new FutureTask<>(() -> stage.compareAndSet(QUEUED, STARTED) ? task.call() : fallback);
        if (!execute(future)) {
            return fallback;
        }
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (stage.compareAndSet(QUEUED, ABANDONED)) {
                future.cancel(false);
            }
            return fallback;
        } catch (TimeoutException e) {
            if (stage.compareAndSet(QUEUED, ABANDONED)) {
                future.cancel(false);
                return fallback;
            }
            return awaitStarted(future, fallback);
        } catch (ExecutionException e) {
            return fallback;
        }
    }

    private static <T> T awaitStarted(FutureTask<T> future, T fallback) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return fallback;
        } catch (ExecutionException e) {
            return fallback;
        }
    }

    /** Refuses every later task. */
    void close() {
        closed = true;
    }

    private boolean execute(Runnable task) {
        try {
            executor.execute(() -> {
                Thread previous = workerThread;
                workerThread = Thread.currentThread();
                try {
                    task.run();
                } catch (RuntimeException e) {
                    // Keep the worker alive; SessionCore already turns analysis errors into ENGINE_ERROR.
                } finally {
                    workerThread = previous;
                }
            });
            return true;
        } catch (RejectedExecutionException e) {
            return false;
        }
    }
}
