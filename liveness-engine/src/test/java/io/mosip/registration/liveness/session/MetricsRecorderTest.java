/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.session;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.mosip.registration.liveness.api.LivenessMetrics;

class MetricsRecorderTest {

    @Test
    void emptyRecorderGivesZeros() {
        LivenessMetrics m = new MetricsRecorder().snapshot();
        assertEquals(0, m.framesAnalysed());
        assertEquals(0, m.meanAnalysisMs());
        assertEquals(0, m.meanChallengeMs());
        assertEquals(0, m.retryRate());
        assertEquals(0, m.failureRate());
    }

    @Test
    void computesMeansAndRates() {
        MetricsRecorder r = new MetricsRecorder();
        r.sessionStarted();
        r.attemptStarted();
        r.attemptStarted();
        r.attemptFailed();
        r.retried();
        r.frameAnalysed(100);
        r.frameAnalysed(300);
        r.challengeCompleted(2000);
        LivenessMetrics m = r.snapshot();
        assertEquals(1, m.sessions());
        assertEquals(2, m.framesAnalysed());
        assertEquals(200, m.meanAnalysisMs());
        assertEquals(2000, m.meanChallengeMs());
        assertEquals(0.5, m.retryRate());
        assertEquals(0.5, m.failureRate());
    }
}
