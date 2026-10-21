/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.session;

import static io.mosip.registration.liveness.session.SessionFixture.BOX;
import static io.mosip.registration.liveness.session.SessionFixture.IMAGE;
import static io.mosip.registration.liveness.session.SessionFixture.LIVE;
import static io.mosip.registration.liveness.session.SessionFixture.SPOOF;
import static io.mosip.registration.liveness.session.SessionFixture.scored;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import io.mosip.registration.liveness.active.FaceBox;
import io.mosip.registration.liveness.api.CaptureCheck;
import io.mosip.registration.liveness.api.DeviceEvent;
import io.mosip.registration.liveness.api.FailureReason;
import io.mosip.registration.liveness.api.FeedbackCode;
import io.mosip.registration.liveness.api.GuidanceTip;
import io.mosip.registration.liveness.api.LivenessEvent;
import io.mosip.registration.liveness.api.LivenessEvent.Type;
import io.mosip.registration.liveness.api.LivenessPolicy;
import io.mosip.registration.liveness.api.LivenessUpdate;
import io.mosip.registration.liveness.api.SessionState;
import io.mosip.registration.liveness.image.BgrImage;
import io.mosip.registration.liveness.passive.QualityChecker.Tip;
import io.mosip.registration.liveness.passive.TestAnalyses;

class SessionCoreCaptureTest {

    private final SessionFixture f = new SessionFixture();

    private SessionCore passedCore(LivenessPolicy policy) {
        SessionCore core = f.core(policy);
        assertEquals(SessionState.PASSED, f.frames(core, 5, scored(LIVE)).state());
        return core;
    }

    private void rejects(ScriptedAnalyzer.Step capture, FailureReason reason) {
        SessionCore core = passedCore(SessionFixture.PASSIVE);
        f.analyzer.then(capture);
        CaptureCheck check = core.checkCapture(IMAGE);
        assertFalse(check.accepted());
        assertEquals(reason, check.failureReason().get());
        assertEquals(SessionState.FAILED, core.current().state());
        assertEquals(reason, core.current().failureReason().get());
    }

    @Test
    void acceptsAConsistentCapture() {
        SessionCore core = passedCore(SessionFixture.PASSIVE);
        f.analyzer.then(scored(LIVE));
        CaptureCheck check = core.checkCapture(IMAGE);
        assertTrue(check.accepted());
        assertEquals("1.1.0", check.modelSetVersion());
        LivenessUpdate u = core.current();
        assertEquals(SessionState.PASSED, u.state());
        assertEquals(FeedbackCode.SUCCESS, u.feedback());
        assertTrue(u.finished());
        assertEquals(Type.PASSED, f.eventTypes().get(f.events.size() - 1));
        int calls = f.analyzer.calls();
        f.frame(core, scored(LIVE));
        CaptureCheck again = core.checkCapture(IMAGE);
        assertFalse(again.accepted());
        assertEquals(calls, f.analyzer.calls(), "no analysis after the success");
        assertEquals(FeedbackCode.SUCCESS, core.current().feedback());
    }

    @Test
    void acceptsACaptureOfAnotherResolution() {
        SessionCore core = passedCore(SessionFixture.PASSIVE);
        f.analyzer.then(t -> TestAnalyses.scored(t, new FaceBox(440, 280, 400, 400), LIVE));
        assertTrue(core.checkCapture(new BgrImage(1280, 960, new byte[1280 * 960 * 3])).accepted());
    }

    @Test
    void refusesAnUnreadableCapture() {
        SessionCore core = passedCore(SessionFixture.PASSIVE);
        int calls = f.analyzer.calls();
        assertEquals(FailureReason.INVALID_FRAME, core.checkCapture(null).failureReason().get());
        assertEquals(calls, f.analyzer.calls());
    }

    @Test
    void refusesACaptureWithoutExactlyOneFace() {
        rejects(TestAnalyses::noFace, FailureReason.FACE_CONTINUITY_LOST);
    }

    @Test
    void refusesALowQualityCapture() {
        rejects(t -> TestAnalyses.lowQuality(t, BOX, Tip.LOOK_CAMERA), FailureReason.LOW_QUALITY);
        assertEquals(GuidanceTip.LOOK_CAMERA, f.updates.get(f.updates.size() - 1).tip().get());
    }

    @Test
    void refusesACaptureInTheAttackZone() {
        rejects(scored(SPOOF), FailureReason.PAD_ATTACK_DETECTED);
    }

    @Test
    void refusesACaptureAtTheAttackThreshold() {
        rejects(scored(0.30), FailureReason.PAD_ATTACK_DETECTED);
    }

    @Test
    void refusesACaptureOfAnotherFace() {
        rejects(t -> TestAnalyses.scored(t, new FaceBox(420, 140, 200, 200), LIVE),
            FailureReason.FACE_CONTINUITY_LOST);
    }

    @Test
    void refusesACaptureWhenTheEngineFails() {
        rejects(t -> {
            throw new IllegalStateException("onnx");
        }, FailureReason.ENGINE_ERROR);
    }

    @Test
    void refusesACaptureBeforePassed() {
        SessionCore core = f.core(SessionFixture.PASSIVE);
        CaptureCheck check = core.checkCapture(IMAGE);
        assertEquals(FailureReason.FACE_CONTINUITY_LOST, check.failureReason().get());
        assertEquals(SessionState.INIT, core.current().state());
        assertEquals(0, f.analyzer.calls());
    }

    @Test
    void consistencyUsesRelativePositions() {
        FaceBox followed = new FaceBox(220, 140, 200, 200);
        assertTrue(SessionCore.consistent(followed, 640, 480, new FaceBox(440, 280, 400, 400), 1280, 960));
        assertFalse(SessionCore.consistent(followed, 640, 480, new FaceBox(420, 140, 200, 200), 640, 480));
        assertFalse(SessionCore.consistent(followed, 640, 480, new FaceBox(270, 190, 100, 100), 640, 480));
        assertFalse(SessionCore.consistent(followed, 640, 480, new FaceBox(170, 90, 310, 310), 640, 480));
    }

    @Test
    void retryStartsANewAttempt() {
        SessionCore core = f.core(SessionFixture.PASSIVE);
        f.frames(core, 5, scored(SPOOF));
        assertTrue(core.retry());
        LivenessUpdate u = core.current();
        assertEquals(SessionState.INIT, u.state());
        assertEquals(FeedbackCode.CHECKING, u.feedback());
        assertEquals(1, u.attempt());
        LivenessUpdate next = f.frame(core, scored(LIVE));
        assertEquals(2, next.attempt());
        assertEquals(0.2, next.progress(), 1e-9, "the window starts empty");
        assertEquals(2, f.eventTypes().stream().filter(type -> type == Type.SESSION_STARTED).count());
        assertEquals(0.5, f.metrics.snapshot().retryRate());
    }

    @Test
    void retryIsRefusedOutsideAFailure() {
        SessionCore core = f.core(SessionFixture.PASSIVE);
        assertFalse(core.retry());
        f.frame(core, scored(LIVE));
        assertFalse(core.retry());
        SessionFixture g = new SessionFixture();
        SessionCore exhausted = g.core(SessionFixture.policy().maxAttempts(1).build());
        g.frames(exhausted, 5, scored(SPOOF));
        assertFalse(exhausted.retry());
        SessionCore succeeded = passedCore(SessionFixture.PASSIVE);
        f.analyzer.then(scored(LIVE));
        assertTrue(succeeded.checkCapture(IMAGE).accepted());
        assertFalse(succeeded.retry());
    }

    @Test
    void aDeviceErrorBeforeTheFirstFrameUsesNoAttempt() {
        SessionCore core = f.core(SessionFixture.PASSIVE);
        core.onDeviceEvent(DeviceEvent.UNAVAILABLE, 50);
        LivenessUpdate u = core.current();
        assertEquals(SessionState.INIT, u.state());
        assertEquals(FeedbackCode.DEVICE_ERROR, u.feedback());
        assertEquals(0, u.attempt());
        LivenessEvent event = f.events.get(0);
        assertEquals(Type.DEVICE_ERROR, event.type());
        assertEquals(DeviceEvent.UNAVAILABLE, event.deviceEvent().get());
        assertEquals(0, event.attempt());
        assertEquals(0, event.durationMs());
        assertEquals(1, f.frames(core, 5, scored(LIVE)).attempt());
    }

    @Test
    void aDeviceErrorDuringAnAttemptFailsIt() {
        SessionCore core = f.core(SessionFixture.PASSIVE);
        f.frame(core, scored(LIVE));
        core.onDeviceEvent(DeviceEvent.STREAM_INTERRUPTED, 150);
        LivenessUpdate u = core.current();
        assertEquals(SessionState.FAILED, u.state());
        assertEquals(FailureReason.DEVICE_ERROR, u.failureReason().get());
        assertTrue(u.retryAvailable());
        assertEquals(Arrays.asList(Type.SESSION_STARTED, Type.DEVICE_ERROR, Type.FAILED), f.eventTypes());
        core.onDeviceEvent(DeviceEvent.STREAM_INTERRUPTED, 200);
        assertEquals(3, f.events.size(), "ignored once failed");
    }

    @Test
    void aDeviceErrorOnTheLastAttemptEndsTheSession() {
        SessionCore core = f.core(SessionFixture.policy().maxAttempts(1).build());
        f.frame(core, scored(LIVE));
        core.onDeviceEvent(DeviceEvent.CONNECTION_FAILED, 150);
        assertEquals(FeedbackCode.MAX_ATTEMPTS_EXCEEDED, core.current().feedback());
    }
}
