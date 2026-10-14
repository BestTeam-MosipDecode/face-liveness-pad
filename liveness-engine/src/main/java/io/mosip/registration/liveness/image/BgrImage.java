/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.image;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

import javax.imageio.ImageIO;

/**
 * 8-bit image in the memory layout of OpenCV: rows, then columns, then channels B, G, R. All models and the
 * preprocessing of {@code training/liveness/} work on this layout.
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

    /** Decodes a JPEG or PNG frame. Returns an empty result when the bytes are not a readable image. */
    public static Optional<BgrImage> decode(byte[] encoded) {
        if (encoded == null || encoded.length == 0) {
            return Optional.empty();
        }
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(encoded));
            return image == null ? Optional.empty() : Optional.of(of(image));
        } catch (IOException | RuntimeException e) {
            return Optional.empty();
        }
    }

    public static BgrImage of(BufferedImage image) {
        BufferedImage bgr = image;
        if (image.getType() != BufferedImage.TYPE_3BYTE_BGR) {
            bgr = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            bgr.getGraphics().drawImage(image, 0, 0, null);
        }
        byte[] data = ((DataBufferByte) bgr.getRaster().getDataBuffer()).getData();
        return new BgrImage(bgr.getWidth(), bgr.getHeight(), data.clone());
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
