/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.active;

import java.util.Arrays;
import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * Measures of one frame used by the action detectors, computed from the Face Mesh V2 landmarks as in
 * {@code training/liveness/face_mesh.py}.
 */
public final class FaceMeasures {

    private final double eye;
    private final double mouth;
    private final double yaw;

    /**
     * @param eye   mean eye aspect ratio of both eyes
     * @param mouth distance between the mouth corners divided by the face width
     * @param yaw   horizontal position of the nose tip between the cheek edges, 0.5 when facing the camera
     */
    public FaceMeasures(double eye, double mouth, double yaw) {
        this.eye = eye;
        this.mouth = mouth;
        this.yaw = yaw;
    }

    public double eye() {
        return eye;
    }

    public double mouth() {
        return mouth;
    }

    public double yaw() {
        return yaw;
    }

    /** Median of each measure over the samples, used as the baseline of a challenge. */
    public static FaceMeasures median(List<FaceMeasures> samples) {
        if (samples.isEmpty()) {
            throw new IllegalArgumentException("no sample");
        }
        return new FaceMeasures(median(samples, FaceMeasures::eye), median(samples, FaceMeasures::mouth),
                median(samples, FaceMeasures::yaw));
    }

    private static double median(List<FaceMeasures> samples, ToDoubleFunction<FaceMeasures> measure) {
        return median(samples.stream().mapToDouble(measure).toArray());
    }

    static double median(double[] values) {
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        int middle = sorted.length / 2;
        return sorted.length % 2 == 1 ? sorted[middle] : (sorted[middle - 1] + sorted[middle]) / 2;
    }

    @Override
    public String toString() {
        return String.format("FaceMeasures(eye=%.3f, mouth=%.3f, yaw=%.3f)", eye, mouth, yaw);
    }
}
