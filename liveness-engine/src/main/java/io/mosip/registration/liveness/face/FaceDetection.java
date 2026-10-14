/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.face;

import io.mosip.registration.liveness.active.FaceBox;

/**
 * One face found by YuNet: box, five keypoints in pixels of the source image, score between 0 and 1. Keypoints are
 * in the order right eye, left eye, nose tip, right mouth corner, left mouth corner, where "right" is the person's
 * right, on the left side of a non-mirrored image.
 */
public final class FaceDetection {

    public static final int RIGHT_EYE = 0;
    public static final int LEFT_EYE = 1;
    public static final int NOSE_TIP = 2;

    private final FaceBox box;
    private final double[][] keypoints;
    private final double score;

    public FaceDetection(FaceBox box, double[][] keypoints, double score) {
        this.box = box;
        this.keypoints = keypoints;
        this.score = score;
    }

    public FaceBox box() {
        return box;
    }

    /** {@code keypoints()[i]} is {@code {x, y}} of keypoint {@code i}. */
    public double[][] keypoints() {
        return keypoints;
    }

    public double score() {
        return score;
    }

    /**
     * Square box of the same area and centre, fed to the MiniFASNet crop: YuNet boxes are taller than wide, while
     * the detector used to train MiniFASNet gives nearly square boxes.
     */
    public FaceBox classifierBox() {
        double side = Math.sqrt(box.width() * box.height());
        return new FaceBox(box.x() + box.width() / 2 - side / 2, box.y() + box.height() / 2 - side / 2, side, side);
    }

    @Override
    public String toString() {
        return String.format("FaceDetection(%s, score=%.4f)", box, score);
    }
}
