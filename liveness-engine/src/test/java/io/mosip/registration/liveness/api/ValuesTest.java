/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.mosip.registration.liveness.active.ChallengeType;

class ValuesTest {

    @Test
    void updateExposesOptionalFields() {
        LivenessUpdate u = new LivenessUpdate(SessionState.FAILED, FeedbackCode.FAILED, GuidanceTip.FOLLOW_ACTION,
            ChallengeType.BLINK, 0.5, 2, 3, FailureReason.CHALLENGE_TIMEOUT, true, false, null);
        assertEquals(GuidanceTip.FOLLOW_ACTION, u.tip().get());
        assertEquals(ChallengeType.BLINK, u.challenge().get());
        assertEquals(FailureReason.CHALLENGE_TIMEOUT, u.failureReason().get());
        assertFalse(u.diagnosticScore().isPresent());
        assertTrue(u.retryAvailable());
        assertEquals(2, u.attempt());
        assertEquals(3, u.maxAttempts());
        assertTrue(u.toString().contains("FAILED"));
        LivenessUpdate d = new LivenessUpdate(SessionState.PASSIVE_CHECK, FeedbackCode.CHECKING, null, null, 0.2, 1, 3,
            null, false, false, 0.9);
        assertEquals(0.9, d.diagnosticScore().getAsDouble());
        assertFalse(d.tip().isPresent());
    }

    @Test
    void captureCheckAndEventCarryTheModelVersion() {
        assertTrue(CaptureCheck.accepted("1.1.0").accepted());
        CaptureCheck rejected = CaptureCheck.rejected(FailureReason.LOW_QUALITY, "1.1.0");
        assertFalse(rejected.accepted());
        assertEquals(FailureReason.LOW_QUALITY, rejected.failureReason().get());
        assertEquals("1.1.0", rejected.modelSetVersion());
        LivenessEvent e = new LivenessEvent(LivenessEvent.Type.DEVICE_ERROR, Workflow.SUPERVISOR, 1, 250, "1.1.0",
            FailureReason.DEVICE_ERROR, null, DeviceEvent.STREAM_INTERRUPTED);
        assertEquals(DeviceEvent.STREAM_INTERRUPTED, e.deviceEvent().get());
        assertFalse(e.challenge().isPresent());
        assertEquals(250, e.durationMs());
        assertTrue(e.toString().contains("DEVICE_ERROR"));
    }

    @Test
    void metricsKeepTheirValues() {
        LivenessMetrics m = new LivenessMetrics(10, 120.5, 2300, 2, 0.25, 0.5);
        assertEquals(10, m.framesAnalysed());
        assertEquals(120.5, m.meanAnalysisMs());
        assertEquals(2300, m.meanChallengeMs());
        assertEquals(2, m.sessions());
        assertEquals(0.25, m.retryRate());
        assertEquals(0.5, m.failureRate());
    }
}
