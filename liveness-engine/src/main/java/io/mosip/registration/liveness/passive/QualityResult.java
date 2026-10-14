/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.passive;

import java.util.Optional;

import io.mosip.registration.liveness.passive.QualityChecker.Tip;

/** Measures of one frame and the first failed check, if any. */
public final class QualityResult {

    private final double facePx;
    private final boolean inside;
    private final double brightness;
    private final double sharpness;
    private final double yawDeg;
    private final Tip tip;

    QualityResult(double facePx, boolean inside, double brightness, double sharpness, double yawDeg, Tip tip) {
        this.facePx = facePx;
        this.inside = inside;
        this.brightness = brightness;
        this.sharpness = sharpness;
        this.yawDeg = yawDeg;
        this.tip = tip;
    }

    public boolean passed() {
        return tip == null;
    }

    public Optional<Tip> tip() {
        return Optional.ofNullable(tip);
    }

    public double facePx() {
        return facePx;
    }

    public boolean inside() {
        return inside;
    }

    public double brightness() {
        return brightness;
    }

    public double sharpness() {
        return sharpness;
    }

    public double yawDeg() {
        return yawDeg;
    }

    @Override
    public String toString() {
        return String.format("QualityResult(facePx=%.0f, inside=%s, brightness=%.1f, sharpness=%.1f, yaw=%.1f, tip=%s)",
                facePx, inside, brightness, sharpness, yawDeg, tip);
    }
}
