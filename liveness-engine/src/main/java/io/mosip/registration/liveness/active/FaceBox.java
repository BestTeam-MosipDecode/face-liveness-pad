/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.active;

/** Face box in pixels of the source image: top-left corner, width and height. */
public final class FaceBox {

    private final double x;
    private final double y;
    private final double width;
    private final double height;

    public FaceBox(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double width() {
        return width;
    }

    public double height() {
        return height;
    }

    /** Intersection over union with {@code other}, between 0 and 1. */
    public double iou(FaceBox other) {
        double x1 = Math.max(x, other.x);
        double y1 = Math.max(y, other.y);
        double x2 = Math.min(x + width, other.x + other.width);
        double y2 = Math.min(y + height, other.y + other.height);
        double intersection = Math.max(0, x2 - x1) * Math.max(0, y2 - y1);
        double union = width * height + other.width * other.height - intersection;
        return union > 0 ? intersection / union : 0;
    }

    @Override
    public String toString() {
        return String.format("FaceBox(%.1f, %.1f, %.1f, %.1f)", x, y, width, height);
    }
}
