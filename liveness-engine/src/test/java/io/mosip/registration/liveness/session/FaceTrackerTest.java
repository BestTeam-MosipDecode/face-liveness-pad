/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.mosip.registration.liveness.active.FaceBox;

class FaceTrackerTest {

    static final FaceBox BOX = new FaceBox(100, 100, 200, 200);

    @Test
    void followsOverlappingBoxes() {
        FaceTracker tracker = new FaceTracker(0.3, 1000);
        assertFalse(tracker.box().isPresent());
        assertTrue(tracker.follow(BOX, 0));
        assertTrue(tracker.follow(new FaceBox(120, 100, 200, 200), 100));
        assertFalse(tracker.follow(new FaceBox(400, 100, 200, 200), 200));
        assertEquals(400, tracker.box().get().x());
    }

    @Test
    void limitsTheTimeWithoutAFace() {
        FaceTracker tracker = new FaceTracker(0.3, 1000);
        assertTrue(tracker.withinNoFaceLimit(5000), "no track yet");
        tracker.follow(BOX, 1000);
        assertTrue(tracker.withinNoFaceLimit(2000));
        assertFalse(tracker.withinNoFaceLimit(2001));
    }

    @Test
    void resetForgetsTheTrack() {
        FaceTracker tracker = new FaceTracker(0.3, 1000);
        tracker.follow(BOX, 0);
        tracker.reset();
        assertFalse(tracker.box().isPresent());
        assertTrue(tracker.follow(new FaceBox(400, 100, 200, 200), 100));
    }
}
