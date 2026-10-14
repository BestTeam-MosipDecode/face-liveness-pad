/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.active;

/** The mouth width ratio reaches at least 1.06 x baseline on 2 consecutive analysed frames. */
public final class SmileDetector implements ActionDetector {

    public static final double FACTOR = 1.06;
    public static final int FRAMES = 2;

    private final double level;
    private final int frames;
    private int streak;

    public SmileDetector(FaceMeasures baseline) {
        this(baseline, FACTOR, FRAMES);
    }

    public SmileDetector(FaceMeasures baseline, double factor, int frames) {
        this.level = baseline.mouth() * factor;
        this.frames = frames;
    }

    @Override
    public Result update(FaceMeasures measures) {
        streak = measures.mouth() >= level ? streak + 1 : 0;
        return streak >= frames ? Result.DETECTED : Result.PENDING;
    }
}
