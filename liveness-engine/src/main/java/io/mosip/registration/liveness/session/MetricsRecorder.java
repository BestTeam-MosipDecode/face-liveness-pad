/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.session;

import io.mosip.registration.liveness.api.LivenessMetrics;

/** Anonymous counters of the engine, shared by its sessions. Thread-safe. */
final class MetricsRecorder {

    private long sessions;
    private long attempts;
    private long retries;
    private long failures;
    private long frames;
    private long analysisMs;
    private long challenges;
    private long challengeMs;

    synchronized void sessionStarted() {
        sessions++;
    }

    synchronized void attemptStarted() {
        attempts++;
    }

    synchronized void retried() {
        retries++;
    }

    synchronized void attemptFailed() {
        failures++;
    }

    synchronized void frameAnalysed(long ms) {
        frames++;
        analysisMs += ms;
    }

    synchronized void challengeCompleted(long ms) {
        challenges++;
        challengeMs += ms;
    }

    synchronized LivenessMetrics snapshot() {
        return new LivenessMetrics(frames, frames == 0 ? 0 : (double) analysisMs / frames,
            challenges == 0 ? 0 : (double) challengeMs / challenges, sessions,
            attempts == 0 ? 0 : (double) retries / attempts, attempts == 0 ? 0 : (double) failures / attempts);
    }
}
