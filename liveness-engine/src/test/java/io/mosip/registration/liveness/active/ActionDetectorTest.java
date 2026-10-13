/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.active;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import io.mosip.registration.liveness.active.ActionDetector.Result;

/** Same cases as {@code training/tests/test_actions.py}, so that Java and Python agree. */
class ActionDetectorTest {

    static final FaceMeasures BASE = new FaceMeasures(0.30, 0.40, 0.50);

    static Result run(ChallengeType type, List<FaceMeasures> sequence) {
        ActionDetector detector = ActionDetector.create(type, BASE);
        Result result = Result.PENDING;
        for (FaceMeasures m : sequence) {
            result = detector.update(m);
            if (result != Result.PENDING) {
                break;
            }
        }
        return result;
    }

    static List<FaceMeasures> eyes(Double... values) {
        return Arrays.stream(values).map(v -> new FaceMeasures(v, BASE.mouth(), BASE.yaw())).collect(Collectors.toList());
    }

    static List<FaceMeasures> mouths(Double... values) {
        return Arrays.stream(values).map(v -> new FaceMeasures(BASE.eye(), v, BASE.yaw())).collect(Collectors.toList());
    }

    static List<FaceMeasures> yaws(Double... values) {
        return Arrays.stream(values).map(v -> new FaceMeasures(BASE.eye(), BASE.mouth(), v)).collect(Collectors.toList());
    }

    @Test
    void blinkDetected() {
        assertEquals(Result.DETECTED, run(ChallengeType.BLINK, eyes(0.30, 0.15, 0.29)));
    }

    @Test
    void eyesClosedWithoutReopening() {
        assertEquals(Result.PENDING, run(ChallengeType.BLINK, eyes(0.30, 0.15, 0.15, 0.20)));
    }

    @Test
    void partialClosureIgnored() {
        assertEquals(Result.PENDING, run(ChallengeType.BLINK, eyes(0.30, 0.20, 0.30, 0.20, 0.30)));
    }

    @Test
    void blinkAtExactFactors() {
        assertEquals(Result.DETECTED, run(ChallengeType.BLINK, eyes(0.30 * 0.6, 0.30 * 0.85)));
    }

    @Test
    void smileOnTwoFrames() {
        assertEquals(Result.DETECTED, run(ChallengeType.SMILE, mouths(0.40, 0.45, 0.46)));
    }

    @Test
    void singleSmileFrameIsNotEnough() {
        assertEquals(Result.PENDING, run(ChallengeType.SMILE, mouths(0.45, 0.40, 0.45, 0.40)));
    }

    @Test
    void turnLeftAndBack() {
        assertEquals(Result.DETECTED, run(ChallengeType.TURN_LEFT, yaws(0.55, 0.66, 0.60, 0.55)));
    }

    @Test
    void turnLeftWithoutReturn() {
        assertEquals(Result.PENDING, run(ChallengeType.TURN_LEFT, yaws(0.66, 0.70, 0.68)));
    }

    @Test
    void turnRightWhenLeftRequestedFails() {
        assertEquals(Result.FAILED, run(ChallengeType.TURN_LEFT, yaws(0.40, 0.34)));
    }

    @Test
    void turnRightAndBack() {
        assertEquals(Result.DETECTED, run(ChallengeType.TURN_RIGHT, yaws(0.40, 0.34, 0.45)));
    }

    @Test
    void turnLeftWhenRightRequestedFails() {
        assertEquals(Result.FAILED, run(ChallengeType.TURN_RIGHT, yaws(0.66)));
    }

    @Test
    void invalidDirectionRejected() {
        assertThrows(IllegalArgumentException.class, () -> new HeadTurnDetector(BASE, 0));
    }

    @Test
    void medianOfMeasures() {
        FaceMeasures median = FaceMeasures.median(Arrays.asList(new FaceMeasures(0.1, 0.4, 0.5),
                new FaceMeasures(0.3, 0.2, 0.7), new FaceMeasures(0.2, 0.3, 0.6), new FaceMeasures(0.4, 0.1, 0.4)));
        assertEquals(0.25, median.eye(), 1e-12);
        assertEquals(0.25, median.mouth(), 1e-12);
        assertEquals(0.55, median.yaw(), 1e-12);
    }

    @Test
    void medianOfNothingRejected() {
        assertThrows(IllegalArgumentException.class, () -> FaceMeasures.median(Arrays.asList()));
    }
}
