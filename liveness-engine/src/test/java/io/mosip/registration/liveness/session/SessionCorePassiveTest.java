/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.session;

import static io.mosip.registration.liveness.session.SessionFixture.AMBIGUOUS;
import static io.mosip.registration.liveness.session.SessionFixture.BLINK_ONLY;
import static io.mosip.registration.liveness.session.SessionFixture.FAR_BOX;
import static io.mosip.registration.liveness.session.SessionFixture.LIVE;
import static io.mosip.registration.liveness.session.SessionFixture.SPOOF;
import static io.mosip.registration.liveness.session.SessionFixture.lowQuality;
import static io.mosip.registration.liveness.session.SessionFixture.noFace;
import static io.mosip.registration.liveness.session.SessionFixture.scored;
import static io.mosip.registration.liveness.session.SessionFixture.scoredAt;
import static io.mosip.registration.liveness.session.SessionFixture.twoFaces;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import org.junit.jupiter.api.Test;

import io.mosip.registration.liveness.active.ChallengeType;
import io.mosip.registration.liveness.api.FailureReason;
import io.mosip.registration.liveness.api.FeedbackCode;
import io.mosip.registration.liveness.api.GuidanceTip;
import io.mosip.registration.liveness.api.LivenessEvent;
import io.mosip.registration.liveness.api.LivenessEvent.Type;
import io.mosip.registration.liveness.api.LivenessEventListener;
import io.mosip.registration.liveness.api.LivenessPolicy;
import io.mosip.registration.liveness.api.LivenessUpdate;
import io.mosip.registration.liveness.api.SessionState;
import io.mosip.registration.liveness.api.Workflow;
import io.mosip.registration.liveness.passive.QualityChecker.Tip;

class SessionCorePassiveTest {

    private final SessionFixture f = new SessionFixture();

    @Test
    void startsInInit() {
        SessionCore core = f.core(SessionFixture.PASSIVE);
        LivenessUpdate u = core.current();
        assertEquals(SessionState.INIT, u.state());
        assertEquals(FeedbackCode.CHECKING, u.feedback());
        assertEquals(0, u.attempt());
        assertEquals(3, u.maxAttempts());
    }

    @Test
    void theDefaultPolicySendsALiveFaceToTheChallengesAndStillRejectsAnAttack() {
        // Decision of 21 October (#7): pass threshold 1.00 by default for the three workflows.
        assertEquals(1.0, LivenessPolicy.defaults().passThreshold());
        SessionCore live = f.core(LivenessPolicy.defaults());
        assertEquals(SessionState.ACTIVE_CHALLENGE, f.frames(live, 5, scored(LIVE)).state());

        SessionFixture g = new SessionFixture();
        SessionCore attack = g.core(LivenessPolicy.defaults());
        LivenessUpdate rejected = g.frames(attack, 5, scored(SPOOF));
        assertEquals(SessionState.FAILED, rejected.state());
        assertEquals(FailureReason.PAD_ATTACK_DETECTED, rejected.failureReason().get());
    }

    @Test
    void aLiveFacePassesWithoutAction() {
        SessionCore core = f.core(SessionFixture.PASSIVE);
        LivenessUpdate first = f.frame(core, scored(LIVE));
        assertEquals(SessionState.PASSIVE_CHECK, first.state());
        assertEquals(FeedbackCode.CHECKING, first.feedback());
        assertEquals(0.2, first.progress(), 1e-9);
        assertEquals(1, first.attempt());
        LivenessUpdate last = f.frames(core, 4, scored(LIVE));
        assertEquals(SessionState.PASSED, last.state());
        assertEquals(FeedbackCode.HOLD_STILL, last.feedback());
        assertEquals(1.0, last.progress());
        assertFalse(last.diagnosticScore().isPresent());
        assertEquals(Collections.singletonList(Type.SESSION_STARTED), f.eventTypes());
        LivenessEvent started = f.events.get(0);
        assertEquals(Workflow.RESIDENT, started.workflow());
        assertEquals(1, started.attempt());
        assertEquals("1.1.0", started.modelSetVersion());
        assertEquals(0, started.durationMs());
        assertEquals(Arrays.asList(false, false, false, false, false), f.analyzer.activeFlags);
        assertEquals(5, f.updates.size());
    }

    @Test
    void aPhotoIsAnAttack() {
        SessionCore core = f.core(SessionFixture.PASSIVE);
        LivenessUpdate u = f.frames(core, 5, scored(SPOOF));
        assertEquals(SessionState.FAILED, u.state());
        assertEquals(FeedbackCode.FAILED, u.feedback());
        assertEquals(FailureReason.PAD_ATTACK_DETECTED, u.failureReason().get());
        assertFalse(u.tip().isPresent());
        assertTrue(u.retryAvailable());
        assertFalse(u.finished());
        assertEquals(Arrays.asList(Type.SESSION_STARTED, Type.FAILED), f.eventTypes());
        LivenessEvent failed = f.events.get(1);
        assertEquals(FailureReason.PAD_ATTACK_DETECTED, failed.failureReason().get());
        assertEquals(400, failed.durationMs());
    }

    @Test
    void anAmbiguousScoreFailsWhenTheActiveCheckIsDisabled() {
        SessionCore core = f.core(SessionFixture.policy().activeEnabled(false).build());
        LivenessUpdate u = f.frames(core, 5, scored(AMBIGUOUS));
        assertEquals(SessionState.FAILED, u.state());
        assertEquals(FailureReason.LIVENESS_BELOW_THRESHOLD, u.failureReason().get());
    }

    @Test
    void anAmbiguousScoreStartsTheActiveCheck() {
        SessionCore core = f.core(BLINK_ONLY);
        LivenessUpdate u = f.frames(core, 5, scored(AMBIGUOUS));
        assertEquals(SessionState.ACTIVE_CHALLENGE, u.state());
        assertEquals(FeedbackCode.HOLD_STILL, u.feedback());
        assertEquals(ChallengeType.BLINK, u.challenge().get());
        assertEquals(0, u.progress());
        assertEquals(Arrays.asList(Type.SESSION_STARTED, Type.ACTIVE_TRIGGERED), f.eventTypes());
    }

    @Test
    void guidanceFramesDoNotCount() {
        SessionCore core = f.core(SessionFixture.PASSIVE);
        assertEquals(FeedbackCode.NO_FACE, f.frame(core, noFace()).feedback());
        assertEquals(FeedbackCode.MULTIPLE_FACES, f.frame(core, twoFaces()).feedback());
        LivenessUpdate dark = f.frame(core, lowQuality(Tip.LIGHTING));
        assertEquals(FeedbackCode.LOW_QUALITY, dark.feedback());
        assertEquals(GuidanceTip.LIGHTING, dark.tip().get());
        assertEquals(0, dark.progress());
        assertEquals(SessionState.PASSIVE_CHECK, f.frames(core, 4, scored(LIVE)).state());
        assertEquals(SessionState.PASSED, f.frame(core, scored(LIVE)).state());
    }

    @Test
    void anotherFaceStartsANewWindow() {
        SessionCore core = f.core(SessionFixture.PASSIVE);
        f.frames(core, 4, scored(LIVE));
        LivenessUpdate moved = f.frame(core, scoredAt(FAR_BOX, LIVE));
        assertEquals(SessionState.PASSIVE_CHECK, moved.state());
        assertEquals(0.2, moved.progress(), 1e-9);
        assertEquals(SessionState.PASSED, f.frames(core, 4, scoredAt(FAR_BOX, LIVE)).state());
    }

    @Test
    void noDecisionInTimeFailsWithTheMostFrequentGuidance() {
        SessionCore core = f.core(SessionFixture.policy().passiveTimeoutMs(1000).build());
        f.frames(core, 3, noFace());
        LivenessUpdate u = f.frames(core, 8, lowQuality(Tip.LIGHTING));
        assertEquals(SessionState.PASSIVE_CHECK, u.state(), "t = 1100, 1000 ms after the first frame");
        u = f.frame(core, lowQuality(Tip.LIGHTING));
        assertEquals(SessionState.FAILED, u.state());
        assertEquals(FailureReason.LOW_QUALITY, u.failureReason().get());
        assertEquals(GuidanceTip.LIGHTING, u.tip().get());
    }

    @Test
    void anUnreadableFrameChangesNothing() {
        SessionCore core = f.core(SessionFixture.PASSIVE);
        core.onFrame(null, 100);
        assertEquals(1, core.invalidFrames());
        assertEquals(SessionState.INIT, core.current().state());
        assertEquals(0, f.analyzer.calls());
        assertTrue(f.updates.isEmpty());
    }

    @Test
    void anEngineErrorFailsTheAttempt() {
        SessionCore core = f.core(SessionFixture.PASSIVE);
        LivenessUpdate u = f.frame(core, t -> {
            throw new IllegalStateException("onnx");
        });
        assertEquals(SessionState.FAILED, u.state());
        assertEquals(FailureReason.ENGINE_ERROR, u.failureReason().get());
        assertFalse(u.tip().isPresent());
        assertEquals(1, u.attempt());
    }

    @Test
    void theLastAttemptEndsTheSession() {
        SessionCore core = f.core(SessionFixture.policy().maxAttempts(1).build());
        LivenessUpdate u = f.frames(core, 5, scored(SPOOF));
        assertEquals(FeedbackCode.MAX_ATTEMPTS_EXCEEDED, u.feedback());
        assertTrue(u.finished());
        assertFalse(u.retryAvailable());
        assertEquals(Arrays.asList(Type.SESSION_STARTED, Type.FAILED, Type.MAX_ATTEMPTS), f.eventTypes());
        f.frame(core, scored(LIVE));
        assertEquals(5, f.analyzer.calls());
    }

    @Test
    void aFailingListenerDoesNotStopTheSession() {
        LivenessEventListener throwing = new LivenessEventListener() {
            @Override
            public void onUpdate(LivenessUpdate update) {
                throw new IllegalStateException("client bug");
            }

            @Override
            public void onEvent(LivenessEvent event) {
                throw new IllegalStateException("client bug");
            }
        };
        SessionCore core = new SessionCore(Workflow.OPERATOR, SessionFixture.PASSIVE, f.analyzer, f.clock,
            new Random(1), "1.1.0", throwing, f.metrics);
        assertEquals(SessionState.PASSED, f.frames(core, 5, scored(LIVE)).state());
    }

    @Test
    void theScoreIsShownOnlyInDiagnosticMode() {
        SessionCore core = f.core(SessionFixture.policy().diagnostic(true).build());
        assertEquals(LIVE, f.frame(core, scored(LIVE)).diagnosticScore().getAsDouble());
    }

    @Test
    void countsAttemptsAndFailures() {
        SessionCore core = f.core(SessionFixture.PASSIVE);
        f.frames(core, 5, scored(SPOOF));
        assertEquals(5, f.metrics.snapshot().framesAnalysed());
        assertEquals(1.0, f.metrics.snapshot().failureRate());
    }
}
