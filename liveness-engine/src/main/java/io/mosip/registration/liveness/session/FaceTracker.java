/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.session;

import java.util.Optional;

import io.mosip.registration.liveness.active.FaceBox;

/** Follows one face from frame to frame (sections 3.3 and 3.4 of docs/decision-flow.md). */
final class FaceTracker {

    private final double minOverlap;
    private final long maxNoFaceMs;
    private FaceBox box;
    private long lastFaceT;

    FaceTracker(double minOverlap, long maxNoFaceMs) {
        this.minOverlap = minOverlap;
        this.maxNoFaceMs = maxNoFaceMs;
    }

    /** Forgets the face: the next box starts a new track. */
    void reset() {
        box = null;
    }

    /** Records the face of a frame. Returns false when it does not continue the track (overlap below the minimum). */
    boolean follow(FaceBox next, long t) {
        boolean continues = box == null || box.iou(next) >= minOverlap;
        box = next;
        lastFaceT = t;
        return continues;
    }

    /** True while no face has been followed yet, or the last one was seen at most {@code maxNoFaceMs} before t. */
    boolean withinNoFaceLimit(long t) {
        return box == null || t - lastFaceT <= maxNoFaceMs;
    }

    /** Box of the last face followed. */
    Optional<FaceBox> box() {
        return Optional.ofNullable(box);
    }
}
