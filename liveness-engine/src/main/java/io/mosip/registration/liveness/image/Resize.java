/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.image;

/**
 * Bilinear resize with the arithmetic of OpenCV {@code cv2.resize(..., INTER_LINEAR)} on 8-bit images
 * ({@code modules/imgproc/src/resize.cpp}): pixel-centre mapping, 11-bit fixed-point coefficients, and the vertical
 * pass of the vectorised path {@code VResizeLinearVec_32s8u}.
 *
 * <p>The result is identical byte for byte to OpenCV when shrinking to a width whose row size is a multiple of 16
 * bytes, which covers the 80 x 80 classifier patch and the 256 x 256 Face Mesh input. For other sizes, and when
 * enlarging, OpenCV takes other code paths and values may differ by one.
 */
public final class Resize {

    private static final int COEF_BITS = 11;
    private static final int COEF_SCALE = 1 << COEF_BITS;

    private Resize() {
    }

    public static BgrImage linear(BgrImage src, int dstWidth, int dstHeight) {
        int[] xOffsets = new int[dstWidth];
        int[] alpha = coefficients(src.width(), dstWidth, xOffsets);
        int[] yOffsets = new int[dstHeight];
        int[] beta = coefficients(src.height(), dstHeight, yOffsets);

        int rowSize = dstWidth * 3;
        int[][] rows = new int[src.height()][];
        byte[] out = new byte[dstHeight * rowSize];
        for (int dy = 0; dy < dstHeight; dy++) {
            int sy0 = yOffsets[dy];
            int sy1 = Math.min(sy0 + 1, src.height() - 1);
            int[] r0 = horizontal(src, sy0, rows, xOffsets, alpha, dstWidth);
            int[] r1 = horizontal(src, sy1, rows, xOffsets, alpha, dstWidth);
            int b0 = beta[dy * 2];
            int b1 = beta[dy * 2 + 1];
            for (int i = 0; i < rowSize; i++) {
                int v = ((((r0[i] >> 4) * b0) >> 16) + (((r1[i] >> 4) * b1) >> 16) + 2) >> 2;
                out[dy * rowSize + i] = (byte) Math.max(0, Math.min(255, v));
            }
        }
        return new BgrImage(dstWidth, dstHeight, out);
    }

    /** Source offsets and pairs of fixed-point weights, as computed by OpenCV for one axis. */
    static int[] coefficients(int srcSize, int dstSize, int[] offsets) {
        double scale = 1.0 / ((double) dstSize / srcSize);
        int[] weights = new int[dstSize * 2];
        for (int d = 0; d < dstSize; d++) {
            float f = (float) ((d + 0.5) * scale - 0.5);
            int s = (int) Math.floor(f);
            f -= s;
            if (s < 0) {
                s = 0;
                f = 0;
            }
            if (s >= srcSize - 1) {
                s = srcSize - 1;
                f = 0;
            }
            offsets[d] = s;
            weights[d * 2] = (int) Math.rint((1.f - f) * COEF_SCALE);
            weights[d * 2 + 1] = (int) Math.rint(f * COEF_SCALE);
        }
        return weights;
    }

    private static int[] horizontal(BgrImage src, int y, int[][] cache, int[] xOffsets, int[] alpha, int dstWidth) {
        if (cache[y] != null) {
            return cache[y];
        }
        byte[] p = src.pixels();
        int base = y * src.width() * 3;
        int[] row = new int[dstWidth * 3];
        for (int dx = 0; dx < dstWidth; dx++) {
            int sx = xOffsets[dx];
            for (int c = 0; c < 3; c++) {
                int s0 = p[base + sx * 3 + c] & 0xFF;
                row[dx * 3 + c] = sx + 1 < src.width()
                        ? s0 * alpha[dx * 2] + (p[base + (sx + 1) * 3 + c] & 0xFF) * alpha[dx * 2 + 1]
                        : s0 * COEF_SCALE;
            }
        }
        cache[y] = row;
        return row;
    }
}
