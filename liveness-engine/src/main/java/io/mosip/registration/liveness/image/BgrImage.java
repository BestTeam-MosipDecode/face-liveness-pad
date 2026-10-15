/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.image;

/**
 * 8-bit image in the memory layout of OpenCV: rows, then columns, then channels B, G, R. All models and the
 * preprocessing of {@code training/liveness/} work on this layout. Decoding is done by a {@code FrameDecoder}, so
 * that this class has no desktop dependency.
 */
public final class BgrImage {

    private final int width;
    private final int height;
    private final byte[] pixels;

    public BgrImage(int width, int height, byte[] pixels) {
        if (width <= 0 || height <= 0 || pixels.length != width * height * 3) {
            throw new IllegalArgumentException("pixels must hold width x height x 3 bytes");
        }
        this.width = width;
        this.height = height;
        this.pixels = pixels;
    }

    /** EXIF orientation of a JPEG (1 to 8), or 1 when there is none. */
    public static int exifOrientation(byte[] jpeg) {
        if (jpeg.length < 4 || (jpeg[0] & 0xFF) != 0xFF || (jpeg[1] & 0xFF) != 0xD8) {
            return 1;
        }
        int pos = 2;
        while (pos + 4 <= jpeg.length && (jpeg[pos] & 0xFF) == 0xFF) {
            int marker = jpeg[pos + 1] & 0xFF;
            int length = ((jpeg[pos + 2] & 0xFF) << 8) | (jpeg[pos + 3] & 0xFF);
            if (marker == 0xDA || length < 2) {
                return 1;
            }
            int start = pos + 4;
            if (marker == 0xE1 && start + 14 <= jpeg.length && jpeg[start] == 'E' && jpeg[start + 1] == 'x'
                    && jpeg[start + 2] == 'i' && jpeg[start + 3] == 'f') {
                return tiffOrientation(jpeg, start + 6, Math.min(jpeg.length, pos + 2 + length));
            }
            pos += 2 + length;
        }
        return 1;
    }

    private static int tiffOrientation(byte[] b, int tiff, int end) {
        boolean little = b[tiff] == 'I';
        int ifd = tiff + (int) read(b, tiff + 4, 4, little);
        if (ifd + 2 > end) {
            return 1;
        }
        int entries = (int) read(b, ifd, 2, little);
        for (int i = 0; i < entries; i++) {
            int entry = ifd + 2 + i * 12;
            if (entry + 12 > end) {
                return 1;
            }
            if (read(b, entry, 2, little) == 0x0112) {
                int value = (int) read(b, entry + 8, 2, little);
                return value >= 1 && value <= 8 ? value : 1;
            }
        }
        return 1;
    }

    private static long read(byte[] b, int offset, int size, boolean little) {
        long value = 0;
        for (int i = 0; i < size; i++) {
            int byteValue = b[offset + (little ? size - 1 - i : i)] & 0xFF;
            value = (value << 8) | byteValue;
        }
        return value;
    }

    /** The image displayed upright for an EXIF orientation value. */
    public BgrImage orient(int orientation) {
        if (orientation <= 1 || orientation > 8) {
            return this;
        }
        boolean swap = orientation >= 5;
        int w = swap ? height : width;
        int h = swap ? width : height;
        byte[] out = new byte[w * h * 3];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int sx;
                int sy;
                switch (orientation) {
                    case 2: sx = width - 1 - x; sy = y; break;
                    case 3: sx = width - 1 - x; sy = height - 1 - y; break;
                    case 4: sx = x; sy = height - 1 - y; break;
                    case 5: sx = y; sy = x; break;
                    case 6: sx = y; sy = height - 1 - x; break;
                    case 7: sx = width - 1 - y; sy = height - 1 - x; break;
                    default: sx = width - 1 - y; sy = x; break;
                }
                System.arraycopy(pixels, (sy * width + sx) * 3, out, (y * w + x) * 3, 3);
            }
        }
        return new BgrImage(w, h, out);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /** The pixel bytes, without copy. Callers must not modify them. */
    public byte[] pixels() {
        return pixels;
    }

    /** Value 0 to 255 of channel {@code c} (0 B, 1 G, 2 R) at column {@code x} and row {@code y}. */
    public int get(int x, int y, int c) {
        return pixels[(y * width + x) * 3 + c] & 0xFF;
    }

    /** Rows {@code y1} to {@code y2} and columns {@code x1} to {@code x2}, both ends included. */
    public BgrImage crop(int x1, int y1, int x2, int y2) {
        if (x1 < 0 || y1 < 0 || x2 >= width || y2 >= height || x2 < x1 || y2 < y1) {
            throw new IllegalArgumentException("crop outside the image");
        }
        int w = x2 - x1 + 1;
        int h = y2 - y1 + 1;
        byte[] out = new byte[w * h * 3];
        for (int y = 0; y < h; y++) {
            System.arraycopy(pixels, ((y1 + y) * width + x1) * 3, out, y * w * 3, w * 3);
        }
        return new BgrImage(w, h, out);
    }
}
