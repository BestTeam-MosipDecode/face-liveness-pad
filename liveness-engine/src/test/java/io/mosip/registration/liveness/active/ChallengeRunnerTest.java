/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.active;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.mosip.registration.liveness.active.ChallengeStatus.Feedback;
import io.mosip.registration.liveness.active.ChallengeStatus.Outcome;

class ChallengeRunnerTest {

    static final FaceMeasures BASE = new FaceMeasures(0.30, 0.40, 0.50);
    static final FaceMeasures EYES_CLOSED = new FaceMeasures(0.10, 0.40, 0.50);
    static final FaceMeasures SMILING = new FaceMeasures(0.30, 0.46, 0.50);
    static final FaceMeasures TURNED_RIGHT = new FaceMeasures(0.30, 0.40, 0.30);
    static final FaceBox BOX = new FaceBox(100, 100, 200, 200);
    static final double LIVE = 0.9;

    private long t;

    static ChallengeRunner runner(ChallengeType... challenges) {
        return new ChallengeRunner(Arrays.asList(challenges), new ActiveSettings(), 0);
    }

    FrameObservation face(FaceMeasures measures, Double score) {
        return FrameObservation.oneFace(t, BOX, measures, score);
    }

    /** Feeds frames 100 ms apart until the baseline is taken. */
    ChallengeStatus holdStill(ChallengeRunner runner) {
        ChallengeStatus status;
        do {
            t += 100;
            status = runner.update(face(BASE, LIVE));
        } while (status.feedback() == Feedback.HOLD_STILL);
        return status;
    }

    ChallengeStatus next(ChallengeRunner runner, FaceMeasures measures) {
        t += 100;
        return runner.update(face(measures, LIVE));
    }

    @Test
    void blinkThenSmilePasses() {
        ChallengeRunner runner = runner(ChallengeType.BLINK, ChallengeType.SMILE);
        assertEquals(Feedback.HOLD_STILL, runner.status().feedback());
        assertEquals(Feedback.CHALLENGE, holdStill(runner).feedback());
        assertNotNull(runner.baseline());
        assertEquals(Feedback.CHALLENGE, next(runner, EYES_CLOSED).feedback());
        ChallengeStatus detected = next(runner, BASE);
        assertEquals(Feedback.ACTION_DETECTED, detected.feedback());
        assertEquals(1, detected.completed());
        assertEquals(0.5, detected.progress(), 1e-12);
        assertEquals(ChallengeType.SMILE, detected.challenge());
        assertNull(runner.baseline());

        assertEquals(Feedback.CHALLENGE, holdStill(runner).feedback());
        next(runner, SMILING);
        ChallengeStatus passed = next(runner, SMILING);
        assertEquals(Outcome.PASSED, passed.outcome());
        assertEquals(1.0, passed.progress(), 1e-12);
        assertTrue(passed.isFinished());
    }

    @Test
    void baselineNeedsThreeFramesAndTheHoldStillTime() {
        ChallengeRunner runner = runner(ChallengeType.BLINK);
        t = 700;
        assertEquals(Feedback.HOLD_STILL, runner.update(face(BASE, LIVE)).feedback());
        t = 800;
        assertEquals(Feedback.HOLD_STILL, runner.update(face(BASE, LIVE)).feedback());
        t = 900;
        assertEquals(Feedback.CHALLENGE, runner.update(face(BASE, LIVE)).feedback());
    }

    @Test
    void timeoutWhenTheActionIsNotPerformed() {
        ChallengeRunner runner = runner(ChallengeType.BLINK);
        holdStill(runner);
        ChallengeStatus status = runner.status();
        while (!status.isFinished()) {
            status = next(runner, BASE);
            if (!status.isFinished()) {
                assertTrue(t <= 8000);
            }
        }
        assertEquals(Outcome.CHALLENGE_TIMEOUT, status.outcome());
        assertEquals(Feedback.FAILED, status.feedback());
        assertEquals(8100, t);
    }

    @Test
    void keepGoingAfterTwoSeconds() {
        ChallengeRunner runner = runner(ChallengeType.BLINK);
        holdStill(runner);
        long instruction = t;
        ChallengeStatus status;
        do {
            status = next(runner, BASE);
        } while (t - instruction < 2000);
        assertEquals(Feedback.KEEP_GOING, status.feedback());
    }

    @Test
    void wrongDirectionFails() {
        ChallengeRunner runner = runner(ChallengeType.TURN_LEFT);
        holdStill(runner);
        assertEquals(Outcome.CHALLENGE_FAILED, next(runner, TURNED_RIGHT).outcome());
    }

    @Test
    void actionHeldFromTheStartIsNotCounted() {
        ChallengeRunner runner = runner(ChallengeType.SMILE);
        ChallengeStatus status;
        do {
            t += 100;
            status = runner.update(face(SMILING, LIVE));
        } while (status.feedback() == Feedback.HOLD_STILL);
        assertEquals(Outcome.RUNNING, next(runner, SMILING).outcome());
        assertEquals(Outcome.RUNNING, next(runner, SMILING).outcome());
    }

    @Test
    void severalFacesEndTheAttempt() {
        ChallengeRunner runner = runner(ChallengeType.BLINK);
        holdStill(runner);
        assertEquals(Outcome.FACE_CONTINUITY_LOST, runner.update(FrameObservation.multipleFaces(t + 100, 2)).outcome());
    }

    @Test
    void shortLossOfTheFaceIsTolerated() {
        ChallengeRunner runner = runner(ChallengeType.BLINK);
        holdStill(runner);
        long lastFace = t;
        ChallengeStatus status = runner.update(FrameObservation.noFace(lastFace + 500));
        assertEquals(Outcome.RUNNING, status.outcome());
        assertEquals(Feedback.NO_FACE, status.feedback());
        assertEquals(Outcome.RUNNING, runner.update(FrameObservation.noFace(lastFace + 1000)).outcome());
        assertEquals(Outcome.FACE_CONTINUITY_LOST, runner.update(FrameObservation.noFace(lastFace + 1100)).outcome());
    }

    @Test
    void faceSwappedDuringAChallengeEndsTheAttempt() {
        ChallengeRunner runner = runner(ChallengeType.BLINK);
        holdStill(runner);
        t += 100;
        FaceBox elsewhere = new FaceBox(600, 100, 200, 200);
        assertEquals(Outcome.FACE_CONTINUITY_LOST,
                runner.update(FrameObservation.oneFace(t, elsewhere, BASE, LIVE)).outcome());
    }

    @Test
    void smallMovementKeepsTheTrack() {
        ChallengeRunner runner = runner(ChallengeType.BLINK);
        holdStill(runner);
        t += 100;
        FaceBox moved = new FaceBox(130, 110, 200, 200);
        assertEquals(Outcome.RUNNING, runner.update(FrameObservation.oneFace(t, moved, BASE, LIVE)).outcome());
    }

    @Test
    void lowPassiveScoresStopTheCheck() {
        ChallengeRunner runner = runner(ChallengeType.BLINK);
        ChallengeStatus status = runner.status();
        for (int i = 0; i < 5; i++) {
            t += 100;
            status = runner.update(face(BASE, 0.1));
        }
        assertEquals(Outcome.PAD_ATTACK_DETECTED, status.outcome());
    }

    @Test
    void oneLowScoreDoesNotStopTheCheck() {
        ChallengeRunner runner = runner(ChallengeType.BLINK);
        double[] scores = {0.9, 0.9, 0.1, 0.9, 0.9, 0.1};
        ChallengeStatus status = runner.status();
        for (double score : scores) {
            t += 100;
            status = runner.update(face(BASE, score));
        }
        assertEquals(Outcome.RUNNING, status.outcome());
    }

    @Test
    void lowMeanScoreFailsAtTheEnd() {
        ChallengeRunner runner = new ChallengeRunner(Collections.singletonList(ChallengeType.BLINK),
                new ActiveSettings().minScoredFrames(50), 0);
        ChallengeStatus status;
        do {
            t += 100;
            status = runner.update(face(BASE, 0.2));
        } while (status.feedback() == Feedback.HOLD_STILL);
        t += 100;
        runner.update(face(EYES_CLOSED, 0.2));
        t += 100;
        assertEquals(Outcome.PAD_ATTACK_DETECTED, runner.update(face(BASE, 0.2)).outcome());
    }

    @Test
    void noScoredFrameMeansLivenessNotEstablished() {
        ChallengeRunner runner = runner(ChallengeType.BLINK);
        ChallengeStatus status;
        do {
            t += 100;
            status = runner.update(face(BASE, null));
        } while (status.feedback() == Feedback.HOLD_STILL);
        t += 100;
        runner.update(face(EYES_CLOSED, null));
        t += 100;
        assertEquals(Outcome.LIVENESS_BELOW_THRESHOLD, runner.update(face(BASE, null)).outcome());
    }

    @Test
    void framesWithoutLandmarksDoNotCount() {
        ChallengeRunner runner = runner(ChallengeType.BLINK);
        t += 1000;
        assertEquals(Feedback.HOLD_STILL, runner.update(face(null, LIVE)).feedback());
        holdStill(runner);
        t += 100;
        assertEquals(Feedback.CHALLENGE, runner.update(face(null, LIVE)).feedback());
    }

    @Test
    void finalStatusIsKept() {
        ChallengeRunner runner = runner(ChallengeType.TURN_LEFT);
        holdStill(runner);
        ChallengeStatus failed = next(runner, TURNED_RIGHT);
        assertSame(failed, next(runner, BASE));
        assertFalse(failed.toString().isEmpty());
    }

    @Test
    void invalidArgumentsRejected() {
        List<ChallengeType> none = Collections.emptyList();
        assertThrows(IllegalArgumentException.class, () -> new ChallengeRunner(none, new ActiveSettings(), 0));
        assertThrows(IllegalArgumentException.class, () -> FrameObservation.multipleFaces(0, 1));
        assertThrows(IllegalArgumentException.class, () -> FrameObservation.oneFace(0, null, BASE, null));
        assertThrows(IllegalArgumentException.class, () -> new ActiveSettings().baselineMs(0));
        assertThrows(IllegalArgumentException.class, () -> new ActiveSettings().attackThreshold(1.5));
        assertThrows(IllegalArgumentException.class, () -> new ActiveSettings().minTrackOverlap(-0.1));
    }

    @Test
    void faceBoxOverlap() {
        assertEquals(1.0, BOX.iou(BOX), 1e-12);
        assertEquals(0.0, BOX.iou(new FaceBox(400, 400, 10, 10)), 1e-12);
        assertEquals(1.0 / 3, new FaceBox(0, 0, 2, 2).iou(new FaceBox(1, 0, 2, 2)), 1e-12);
        assertEquals(0.0, new FaceBox(0, 0, 0, 0).iou(new FaceBox(0, 0, 0, 0)), 1e-12);
    }
}
