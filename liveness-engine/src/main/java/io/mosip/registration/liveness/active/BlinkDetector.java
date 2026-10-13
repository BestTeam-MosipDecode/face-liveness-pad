/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.active;

/** The mean eye aspect ratio drops to at most 0.6 x baseline, then rises back to at least 0.85 x baseline. */
public final class BlinkDetector implements ActionDetector {

    public static final double CLOSED_FACTOR = 0.6;
    public static final double OPEN_FACTOR = 0.85;

    private final double closedLevel;
    private final double openLevel;
    private boolean closed;

    public BlinkDetector(FaceMeasures baseline) {
        this(baseline, CLOSED_FACTOR, OPEN_FACTOR);
    }

    public BlinkDetector(FaceMeasures baseline, double closedFactor, double openFactor) {
        this.closedLevel = baseline.eye() * closedFactor;
        this.openLevel = baseline.eye() * openFactor;
    }

    @Override
    public Result update(FaceMeasures measures) {
        if (measures.eye() <= closedLevel) {
            closed = true;
        } else if (closed && measures.eye() >= openLevel) {
            return Result.DETECTED;
        }
        return Result.PENDING;
    }
}
