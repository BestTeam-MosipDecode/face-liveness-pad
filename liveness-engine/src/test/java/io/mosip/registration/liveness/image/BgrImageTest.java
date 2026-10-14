/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class BgrImageTest {

    /** 3 x 2 image whose blue channel holds 10 * row + column. */
    static BgrImage numbered() {
        byte[] p = new byte[3 * 2 * 3];
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 3; x++) {
                p[(y * 3 + x) * 3] = (byte) (10 * y + x);
            }
        }
        return new BgrImage(3, 2, p);
    }

    /** A JPEG header with an APP1 Exif segment holding the orientation tag, little or big endian. */
    static byte[] exifHeader(int orientation, boolean little) {
        byte[] tiff = little
                ? new byte[] {'I', 'I', 42, 0, 8, 0, 0, 0, 1, 0, 0x12, 0x01, 3, 0, 1, 0, 0, 0, (byte) orientation, 0,
                    0, 0, 0, 0, 0, 0}
                : new byte[] {'M', 'M', 0, 42, 0, 0, 0, 8, 0, 1, 0x01, 0x12, 0, 3, 0, 0, 0, 1, 0, (byte) orientation,
                    0, 0, 0, 0, 0, 0};
        int length = 2 + 6 + tiff.length;
        byte[] out = new byte[4 + 4 + 6 + tiff.length + 2];
        int i = 0;
        out[i++] = (byte) 0xFF;
        out[i++] = (byte) 0xD8;
        out[i++] = (byte) 0xFF;
        out[i++] = (byte) 0xE1;
        out[i++] = (byte) (length >> 8);
        out[i++] = (byte) length;
        for (byte b : new byte[] {'E', 'x', 'i', 'f', 0, 0}) {
            out[i++] = b;
        }
        System.arraycopy(tiff, 0, out, i, tiff.length);
        i += tiff.length;
        out[i++] = (byte) 0xFF;
        out[i] = (byte) 0xDA;
        return out;
    }

    @Test
    void readsTheExifOrientation() {
        assertEquals(6, BgrImage.exifOrientation(exifHeader(6, true)));
        assertEquals(8, BgrImage.exifOrientation(exifHeader(8, false)));
        assertEquals(1, BgrImage.exifOrientation(exifHeader(9, true)));
        assertEquals(1, BgrImage.exifOrientation(new byte[] {1, 2, 3, 4}));
    }

    @Test
    void decodesAJpegWithoutExif() throws Exception {
        BufferedImage image = new BufferedImage(16, 8, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, 0xFF0000);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        BgrImage decoded = BgrImage.decode(out.toByteArray()).get();
        assertEquals(16, decoded.width());
        assertEquals(8, decoded.height());
        assertEquals(255, decoded.get(0, 0, 2));
        assertEquals(0, decoded.get(0, 0, 0));
        assertEquals(1, BgrImage.exifOrientation(out.toByteArray()));
    }

    @Test
    void unreadableBytesGiveNothing() {
        assertFalse(BgrImage.decode(new byte[] {1, 2, 3}).isPresent());
        assertFalse(BgrImage.decode(new byte[0]).isPresent());
        assertFalse(BgrImage.decode(null).isPresent());
    }

    @Test
    void orientations() {
        BgrImage image = numbered();
        // Orientation 6: rotate 90 degrees clockwise to display upright.
        BgrImage r6 = image.orient(6);
        assertEquals(2, r6.width());
        assertEquals(3, r6.height());
        assertEquals(10, r6.get(0, 0, 0));
        assertEquals(0, r6.get(1, 0, 0));
        assertEquals(2, r6.get(1, 2, 0));
        BgrImage r8 = image.orient(8);
        assertEquals(2, r8.get(0, 0, 0));
        assertEquals(10, r8.get(1, 2, 0));
        assertEquals(12, image.orient(3).get(0, 0, 0));
        assertEquals(2, image.orient(2).get(0, 0, 0));
        assertEquals(10, image.orient(4).get(0, 0, 0));
        assertEquals(0, image.orient(5).get(0, 0, 0));
        assertEquals(12, image.orient(7).get(0, 0, 0));
        assertTrue(image == image.orient(1));
    }

    @Test
    void cropAndValidation() {
        BgrImage crop = numbered().crop(1, 1, 2, 1);
        assertEquals(2, crop.width());
        assertEquals(11, crop.get(0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> numbered().crop(0, 0, 3, 1));
        assertThrows(IllegalArgumentException.class, () -> new BgrImage(2, 2, new byte[3]));
    }
}
