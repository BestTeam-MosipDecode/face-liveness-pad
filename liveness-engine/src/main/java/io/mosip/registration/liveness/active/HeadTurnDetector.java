/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.active;

/**
 * The yaw ratio moves by the amplitude in the requested direction, then comes back near the baseline. A turn of
 * the same amplitude in the opposite direction fails the challenge.
 *
 * <p>When a person turns the head to their left, in a non-mirrored image, the nose tip moves towards the left cheek
 * edge and the ratio rises.
 */
public final class HeadTurnDetector implements ActionDetector {

    public static final int LEFT = 1;
    public static final int RIGHT = -1;
    public static final double AMPLITUDE = 0.15;
    public static final double RETURN_MARGIN = 0.08;

    private final double baseline;
    private final int direction;
    private final double amplitude;
    private final double returnMargin;
    private boolean turned;

    public HeadTurnDetector(FaceMeasures baseline, int direction) {
        this(baseline, direction, AMPLITUDE, RETURN_MARGIN);
    }

    public HeadTurnDetector(FaceMeasures baseline, int direction, double amplitude, double returnMargin) {
        if (direction != LEFT && direction != RIGHT) {
            throw new IllegalArgumentException("direction must be LEFT or RIGHT");
        }
        this.baseline = baseline.yaw();
        this.direction = direction;
        this.amplitude = amplitude;
        this.returnMargin = returnMargin;
    }

    @Override
    public Result update(FaceMeasures measures) {
        double offset = (measures.yaw() - baseline) * direction;
        if (offset <= -amplitude) {
            return Result.FAILED;
        }
        if (offset >= amplitude) {
            turned = true;
        } else if (turned && Math.abs(offset) <= returnMargin) {
            return Result.DETECTED;
        }
        return Result.PENDING;
    }
}
