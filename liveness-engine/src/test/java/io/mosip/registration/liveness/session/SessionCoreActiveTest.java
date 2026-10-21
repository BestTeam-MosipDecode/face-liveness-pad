/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.session;

import static io.mosip.registration.liveness.session.SessionFixture.AMBIGUOUS;
import static io.mosip.registration.liveness.session.SessionFixture.BASE;
import static io.mosip.registration.liveness.session.SessionFixture.BLINK_ONLY;
import static io.mosip.registration.liveness.session.SessionFixture.EYES_CLOSED;
import static io.mosip.registration.liveness.session.SessionFixture.FAR_BOX;
import static io.mosip.registration.liveness.session.SessionFixture.LIVE;
import static io.mosip.registration.liveness.session.SessionFixture.SPOOF;
import static io.mosip.registration.liveness.session.SessionFixture.active;
import static io.mosip.registration.liveness.session.SessionFixture.activeAt;
import static io.mosip.registration.liveness.session.SessionFixture.noFace;
import static io.mosip.registration.liveness.session.SessionFixture.scored;
import static io.mosip.registration.liveness.session.SessionFixture.scoredAt;
import static io.mosip.registration.liveness.session.SessionFixture.twoFaces;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import io.mosip.registration.liveness.active.ChallengeType;
import io.mosip.registration.liveness.api.FailureReason;
import io.mosip.registration.liveness.api.FeedbackCode;
import io.mosip.registration.liveness.api.GuidanceTip;
import io.mosip.registration.liveness.api.LivenessEvent;
import io.mosip.registration.liveness.api.LivenessEvent.Type;
import io.mosip.registration.liveness.api.LivenessPolicy;
import io.mosip.registration.liveness.api.LivenessUpdate;
import io.mosip.registration.liveness.api.SessionState;

class SessionCoreActiveTest {

    private final SessionFixture f = new SessionFixture();

    /** Five ambiguous frames: the active check starts at t = 500. */
    private SessionCore activeCore(LivenessPolicy policy) {
        SessionCore core = f.core(policy);
        assertEquals(SessionState.ACTIVE_CHALLENGE, f.frames(core, 5, scored(AMBIGUOUS)).state());
        return core;
    }

    /** Feeds baseline frames until the instruction is shown. */
    private LivenessUpdate holdStill(SessionCore core) {
        LivenessUpdate u;
        int frames = 0;
        do {
            u = f.frame(core, active(BASE, LIVE));
            assertTrue(++frames < 20, "the instruction never came");
        } while (u.feedback() == FeedbackCode.HOLD_STILL);
        return u;
    }

    @Test
    void aBlinkPassesTheActiveCheck() {
        SessionCore core = activeCore(BLINK_ONLY);
        LivenessUpdate instruction = holdStill(core);
        assertEquals(FeedbackCode.CHALLENGE, instruction.feedback());
        assertEquals(ChallengeType.BLINK, instruction.challenge().get());
        f.frame(core, active(EYES_CLOSED, LIVE));
        LivenessUpdate u = f.frame(core, active(BASE, LIVE));
        assertEquals(SessionState.PASSED, u.state());
        assertEquals(FeedbackCode.HOLD_STILL, u.feedback());
        LivenessEvent passed = f.events.stream().filter(e -> e.type() == Type.CHALLENGE_PASSED).findFirst().get();
        assertEquals(ChallengeType.BLINK, passed.challenge().get());
        assertEquals(Arrays.asList(false, false, false, false, false, true), f.analyzer.activeFlags.subList(0, 6));
        assertTrue(f.metrics.snapshot().meanChallengeMs() > 0);
    }

    @Test
    void aChallengeNotPerformedInTimeFails() {
        SessionCore core = activeCore(SessionFixture.policy()
            .challengeTypes(BLINK_ONLY.challengeTypes()).minChallenges(1).challengeTimeoutMs(1000).build());
        LivenessUpdate u;
        int frames = 0;
        do {
            u = f.frame(core, active(BASE, LIVE));
            assertTrue(++frames < 20);
        } while (u.state() == SessionState.ACTIVE_CHALLENGE);
        assertEquals(FailureReason.CHALLENGE_TIMEOUT, u.failureReason().get());
        assertEquals(GuidanceTip.FOLLOW_ACTION, u.tip().get());
        int failedAt = f.eventTypes().indexOf(Type.CHALLENGE_FAILED);
        assertTrue(failedAt > 0);
        assertEquals(Type.FAILED, f.eventTypes().get(failedAt + 1));
        assertEquals(ChallengeType.BLINK, f.events.get(failedAt).challenge().get());
    }

    @Test
    void faceSwapAtTheStartOfTheActiveCheckFails() {
        SessionCore core = activeCore(BLINK_ONLY);
        LivenessUpdate u = f.frame(core, activeAt(FAR_BOX, BASE, LIVE));
        assertEquals(SessionState.FAILED, u.state());
        assertEquals(FailureReason.FACE_CONTINUITY_LOST, u.failureReason().get());
        assertFalse(f.eventTypes().contains(Type.CHALLENGE_FAILED));
    }

    @Test
    void aSecondFaceDuringTheChallengeFails() {
        SessionCore core = activeCore(BLINK_ONLY);
        f.frame(core, active(BASE, LIVE));
        assertEquals(FailureReason.FACE_CONTINUITY_LOST, f.frame(core, twoFaces()).failureReason().get());
    }

    @Test
    void lowScoresDuringTheChallengeAreAnAttack() {
        SessionCore core = activeCore(BLINK_ONLY);
        LivenessUpdate u = f.frames(core, 5, active(BASE, SPOOF));
        assertEquals(FailureReason.PAD_ATTACK_DETECTED, u.failureReason().get());
        assertFalse(u.tip().isPresent());
    }

    @Test
    void afterPassedAnotherFaceEndsTheAttempt() {
        SessionCore core = f.core(SessionFixture.PASSIVE);
        f.frames(core, 5, scored(LIVE));
        LivenessUpdate u = f.frame(core, scoredAt(FAR_BOX, LIVE));
        assertEquals(SessionState.FAILED, u.state());
        assertEquals(FailureReason.FACE_CONTINUITY_LOST, u.failureReason().get());
    }

    @Test
    void afterPassedTwoFacesEndTheAttempt() {
        SessionCore core = f.core(SessionFixture.PASSIVE);
        f.frames(core, 5, scored(LIVE));
        assertEquals(FailureReason.FACE_CONTINUITY_LOST, f.frame(core, twoFaces()).failureReason().get());
    }

    @Test
    void afterPassedAShortLossOfTheFaceIsTolerated() {
        SessionCore core = f.core(SessionFixture.PASSIVE);
        f.frames(core, 5, scored(LIVE));
        LivenessUpdate lost = f.frames(core, 5, noFace());
        assertEquals(SessionState.PASSED, lost.state());
        assertEquals(FeedbackCode.NO_FACE, lost.feedback());
        LivenessUpdate back = f.frame(core, scored(LIVE));
        assertEquals(SessionState.PASSED, back.state());
        assertEquals(FeedbackCode.HOLD_STILL, back.feedback());
        assertFalse(f.analyzer.activeFlags.get(f.analyzer.calls() - 1));
    }

    @Test
    void afterPassedALongLossOfTheFaceEndsTheAttempt() {
        SessionCore core = f.core(SessionFixture.PASSIVE);
        f.frames(core, 5, scored(LIVE));
        assertEquals(SessionState.PASSED, f.frames(core, 10, noFace()).state(), "1000 ms without a face");
        LivenessUpdate u = f.frame(core, noFace());
        assertEquals(FailureReason.FACE_CONTINUITY_LOST, u.failureReason().get());
        assertEquals(GuidanceTip.STAY_IN_FRAME, u.tip().get());
    }

    @Test
    void theFaceFollowedInTheActiveCheckIsKeptAfterPassed() {
        SessionCore core = activeCore(BLINK_ONLY);
        holdStill(core);
        f.frame(core, active(EYES_CLOSED, LIVE));
        assertEquals(SessionState.PASSED, f.frame(core, active(BASE, LIVE)).state());
        assertEquals(FailureReason.FACE_CONTINUITY_LOST,
            f.frame(core, scoredAt(FAR_BOX, LIVE)).failureReason().get());
    }

    @Test
    void updatesGiveThePositionOfTheChallenge() {
        SessionCore core = f.core(SessionFixture.policy().challengeTypes(BLINK_ONLY.challengeTypes()).minChallenges(2)
            .build());
        LivenessUpdate passive = f.frame(core, scored(AMBIGUOUS));
        assertEquals(0, passive.challengeIndex());
        assertEquals(0, passive.challengeCount());
        f.frames(core, 4, scored(AMBIGUOUS));
        LivenessUpdate first = holdStill(core);
        assertEquals(1, first.challengeIndex());
        assertEquals(2, first.challengeCount());
        f.frame(core, active(EYES_CLOSED, LIVE));
        LivenessUpdate detected = f.frame(core, active(BASE, LIVE));
        assertEquals(FeedbackCode.ACTION_DETECTED, detected.feedback());
        assertEquals(2, detected.challengeIndex());
        assertEquals(2, detected.challengeCount());
    }
}
