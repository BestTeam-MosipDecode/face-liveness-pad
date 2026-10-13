/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.active;

import java.util.Optional;
import java.util.OptionalDouble;

/**
 * What the frame pipeline found in one analysed frame: the number of faces, the box and measures of the face when
 * there is exactly one, and its passive score when the frame passed the quality checks.
 */
public final class FrameObservation {

    private final long timestampMs;
    private final int faceCount;
    private final FaceBox box;
    private final FaceMeasures measures;
    private final Double passiveScore;

    private FrameObservation(long timestampMs, int faceCount, FaceBox box, FaceMeasures measures,
            Double passiveScore) {
        this.timestampMs = timestampMs;
        this.faceCount = faceCount;
        this.box = box;
        this.measures = measures;
        this.passiveScore = passiveScore;
    }

    public static FrameObservation noFace(long timestampMs) {
        return new FrameObservation(timestampMs, 0, null, null, null);
    }

    public static FrameObservation multipleFaces(long timestampMs, int faceCount) {
        if (faceCount < 2) {
            throw new IllegalArgumentException("faceCount must be at least 2");
        }
        return new FrameObservation(timestampMs, faceCount, null, null, null);
    }

    /**
     * One face. {@code measures} is null when the landmarks could not be computed; {@code passiveScore} is null when
     * the frame failed the quality checks or its yaw is beyond the passive limit.
     */
    public static FrameObservation oneFace(long timestampMs, FaceBox box, FaceMeasures measures, Double passiveScore) {
        if (box == null) {
            throw new IllegalArgumentException("box is required");
        }
        return new FrameObservation(timestampMs, 1, box, measures, passiveScore);
    }

    public long timestampMs() {
        return timestampMs;
    }

    public int faceCount() {
        return faceCount;
    }

    public Optional<FaceBox> box() {
        return Optional.ofNullable(box);
    }

    public Optional<FaceMeasures> measures() {
        return Optional.ofNullable(measures);
    }

    public OptionalDouble passiveScore() {
        return passiveScore == null ? OptionalDouble.empty() : OptionalDouble.of(passiveScore);
    }
}
