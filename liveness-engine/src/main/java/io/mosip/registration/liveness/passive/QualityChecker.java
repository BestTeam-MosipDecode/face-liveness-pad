/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.passive;

import io.mosip.registration.liveness.active.FaceBox;
import io.mosip.registration.liveness.face.FaceDetection;
import io.mosip.registration.liveness.image.BgrImage;
import io.mosip.registration.liveness.image.Resize;

/**
 * Quality checks of one frame (section 3.5 of {@code docs/decision-flow.md}), as in
 * {@code training/liveness/quality.py}. A frame that fails a check gives guidance and does not count for the score.
 */
public final class QualityChecker {

    /** Guidance of a failed check, in the order the checks are applied. */
    public enum Tip {
        STAY_IN_FRAME,
        LIGHTING,
        HOLD_STILL,
        LOOK_CAMERA
    }

    // Provisional bounds, to calibrate on the team captures (#7).
    public static final double MIN_BRIGHTNESS = 60.0;
    public static final double MAX_BRIGHTNESS = 200.0;
    public static final double MIN_SHARPNESS = 40.0;
    static final int SHARPNESS_SIZE = 128;
    /** Depth of the nose tip in front of the eyes divided by the distance between the eyes. To calibrate. */
    public static final double NOSE_DEPTH_RATIO = 0.3;

    private final int minFacePx;
    private final double maxYawDeg;

    /** {@code quality.min_face_px} and {@code quality.max_yaw_deg}. */
    public QualityChecker(int minFacePx, double maxYawDeg) {
        this.minFacePx = minFacePx;
        this.maxYawDeg = maxYawDeg;
    }

    public QualityChecker() {
        this(120, 20);
    }

    public double maxYawDeg() {
        return maxYawDeg;
    }

    /** Runs the checks on one detection. {@code checkYaw} is false during the active check. */
    public QualityResult check(BgrImage image, FaceDetection face, boolean checkYaw) {
        FaceBox b = face.box();
        double facePx = Math.min(b.width(), b.height());
        boolean inside = b.x() >= 0 && b.y() >= 0 && b.x() + b.width() <= image.width()
                && b.y() + b.height() <= image.height();
        BgrImage region = faceRegion(image, b);
        double light = region == null ? 0 : brightness(region);
        double sharp = region == null ? 0 : sharpness(region);
        double yaw = yawDegrees(face.keypoints());

        Tip tip = null;
        if (facePx < minFacePx || !inside) {
            tip = Tip.STAY_IN_FRAME;
        } else if (light < MIN_BRIGHTNESS || light > MAX_BRIGHTNESS) {
            tip = Tip.LIGHTING;
        } else if (sharp < MIN_SHARPNESS) {
            tip = Tip.HOLD_STILL;
        } else if (checkYaw && Math.abs(yaw) > maxYawDeg) {
            tip = Tip.LOOK_CAMERA;
        }
        return new QualityResult(facePx, inside, light, sharp, yaw, tip);
    }

    /** Pixels of the face box, clipped to the image, or null when nothing is left. */
    static BgrImage faceRegion(BgrImage image, FaceBox b) {
        int x1 = Math.max(0, (int) Math.rint(b.x()));
        int y1 = Math.max(0, (int) Math.rint(b.y()));
        int x2 = Math.min(image.width(), (int) Math.rint(b.x() + b.width()));
        int y2 = Math.min(image.height(), (int) Math.rint(b.y() + b.height()));
        return x2 > x1 && y2 > y1 ? image.crop(x1, y1, x2 - 1, y2 - 1) : null;
    }

    /** Grey level of OpenCV 4 {@code COLOR_BGR2GRAY} on 8-bit images: fixed point with 15 bits. */
    static int[] grey(BgrImage image) {
        byte[] p = image.pixels();
        int[] grey = new int[image.width() * image.height()];
        for (int i = 0; i < grey.length; i++) {
            int b = p[i * 3] & 0xFF;
            int g = p[i * 3 + 1] & 0xFF;
            int r = p[i * 3 + 2] & 0xFF;
            grey[i] = (b * 3735 + g * 19235 + r * 9798 + (1 << 14)) >> 15;
        }
        return grey;
    }

    /** Mean grey level of the face region, 0 to 255. */
    static double brightness(BgrImage region) {
        long sum = 0;
        for (int v : grey(region)) {
            sum += v;
        }
        return (double) sum / (region.width() * region.height());
    }

    /**
     * Variance of the Laplacian (3 x 3 aperture, border reflected without repeating the edge) of the face region
     * resized to 128 x 128, so that it does not depend on the distance to the camera.
     */
    static double sharpness(BgrImage region) {
        int n = SHARPNESS_SIZE;
        int[] g = grey(Resize.linear(region, n, n));
        double sum = 0;
        double sumSquares = 0;
        for (int y = 0; y < n; y++) {
            int up = y == 0 ? 1 : y - 1;
            int down = y == n - 1 ? n - 2 : y + 1;
            for (int x = 0; x < n; x++) {
                int left = x == 0 ? 1 : x - 1;
                int right = x == n - 1 ? n - 2 : x + 1;
                double v = g[up * n + x] + g[down * n + x] + g[y * n + left] + g[y * n + right] - 4.0 * g[y * n + x];
                sum += v;
                sumSquares += v * v;
            }
        }
        double mean = sum / (n * n);
        return sumSquares / (n * n) - mean * mean;
    }

    /** Horizontal position of the nose tip between the eyes: 0.5 when facing the camera. */
    static double yawRatio(double[][] keypoints) {
        double right = keypoints[FaceDetection.RIGHT_EYE][0];
        double left = keypoints[FaceDetection.LEFT_EYE][0];
        return left != right ? (keypoints[FaceDetection.NOSE_TIP][0] - right) / (left - right) : 0.5;
    }

    /** Estimated yaw in degrees, from {@code r - 0.5 = (d / D) tan(yaw)}. */
    static double yawDegrees(double[][] keypoints) {
        return Math.toDegrees(Math.atan((yawRatio(keypoints) - 0.5) / NOSE_DEPTH_RATIO));
    }
}
