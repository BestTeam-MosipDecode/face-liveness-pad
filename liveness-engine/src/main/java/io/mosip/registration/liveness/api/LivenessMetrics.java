/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.api;

/** Anonymous counters of the engine since it started. No personal data. */
public final class LivenessMetrics {

    private final long framesAnalysed;
    private final double meanAnalysisMs;
    private final double meanChallengeMs;
    private final long sessions;
    private final double retryRate;
    private final double failureRate;

    /** Created by the engine. */
    public LivenessMetrics(long framesAnalysed, double meanAnalysisMs, double meanChallengeMs, long sessions,
            double retryRate, double failureRate) {
        this.framesAnalysed = framesAnalysed;
        this.meanAnalysisMs = meanAnalysisMs;
        this.meanChallengeMs = meanChallengeMs;
        this.sessions = sessions;
        this.retryRate = retryRate;
        this.failureRate = failureRate;
    }

    /** Number of frames analysed. */
    public long framesAnalysed() {
        return framesAnalysed;
    }

    /** Mean time to analyse one frame. */
    public double meanAnalysisMs() {
        return meanAnalysisMs;
    }

    /** Mean time to complete one challenge. */
    public double meanChallengeMs() {
        return meanChallengeMs;
    }

    /** Number of sessions opened. */
    public long sessions() {
        return sessions;
    }

    /** Retries divided by attempts. */
    public double retryRate() {
        return retryRate;
    }

    /** Failed attempts divided by attempts. */
    public double failureRate() {
        return failureRate;
    }
}
