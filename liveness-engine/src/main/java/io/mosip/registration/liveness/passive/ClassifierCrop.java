/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.passive;

import io.mosip.registration.liveness.active.FaceBox;
import io.mosip.registration.liveness.image.BgrImage;
import io.mosip.registration.liveness.image.Resize;

/**
 * Crop fed to the MiniFASNet classifiers, as specified in {@code training/README.md} and implemented in
 * {@code training/liveness/crop.py} (from Silent-Face-Anti-Spoofing, Apache-2.0, Copyright 2020 Minivision).
 */
public final class ClassifierCrop {

    public static final int PATCH_SIZE = 80;

    private ClassifierCrop() {
    }

    /**
     * Crop box {@code [x1, y1, x2, y2]}, both ends included. The face box is truncated to integers first, as the
     * Python reference does. The scale is capped so that the crop fits in the image, then the box is shifted back
     * inside the image when it overflows.
     */
    public static int[] box(int srcWidth, int srcHeight, FaceBox face, double scale) {
        int x = (int) face.x();
        int y = (int) face.y();
        int w = (int) face.width();
        int h = (int) face.height();
        double s = Math.min((srcHeight - 1) / (double) h, Math.min((srcWidth - 1) / (double) w, scale));

        double newW = w * s;
        double newH = h * s;
        double centerX = w / 2.0 + x;
        double centerY = h / 2.0 + y;

        double x1 = centerX - newW / 2;
        double y1 = centerY - newH / 2;
        double x2 = centerX + newW / 2;
        double y2 = centerY + newH / 2;

        if (x1 < 0) {
            x2 -= x1;
            x1 = 0;
        }
        if (y1 < 0) {
            y2 -= y1;
            y1 = 0;
        }
        if (x2 > srcWidth - 1) {
            x1 -= x2 - srcWidth + 1;
            x2 = srcWidth - 1;
        }
        if (y2 > srcHeight - 1) {
            y1 -= y2 - srcHeight + 1;
            y2 = srcHeight - 1;
        }
        return new int[] {(int) x1, (int) y1, (int) x2, (int) y2};
    }

    /** The 80 x 80 patch, B, G, R, for the model that uses {@code scale}. */
    public static BgrImage patch(BgrImage image, FaceBox face, double scale) {
        int[] b = box(image.width(), image.height(), face, scale);
        return Resize.linear(image.crop(b[0], b[1], b[2], b[3]), PATCH_SIZE, PATCH_SIZE);
    }
}
