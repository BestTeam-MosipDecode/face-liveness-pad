/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.platform.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import io.mosip.registration.liveness.image.BgrImage;

class ImageIoFrameDecoderTest {

    private final ImageIoFrameDecoder decoder = new ImageIoFrameDecoder();

    static byte[] encode(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }

    @Test
    void decodesAPngInBgrOrder() throws IOException {
        BufferedImage image = new BufferedImage(16, 8, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, 0xFF0000);
        BgrImage decoded = decoder.decode(encode(image, "png")).get();
        assertEquals(16, decoded.width());
        assertEquals(8, decoded.height());
        assertEquals(255, decoded.get(0, 0, 2));
        assertEquals(0, decoded.get(0, 0, 0));
    }

    @Test
    void appliesTheExifOrientationOfAJpeg() throws IOException {
        byte[] jpeg = encode(new BufferedImage(16, 8, BufferedImage.TYPE_3BYTE_BGR), "jpg");
        // Insert an APP1 Exif segment with orientation 6 after the JFIF APP0 segment, which must stay first.
        int app0End = 4 + (((jpeg[4] & 0xFF) << 8) | (jpeg[5] & 0xFF));
        byte[] header = exifSegment(6);
        byte[] tagged = new byte[jpeg.length + header.length];
        System.arraycopy(jpeg, 0, tagged, 0, app0End);
        System.arraycopy(header, 0, tagged, app0End, header.length);
        System.arraycopy(jpeg, app0End, tagged, app0End + header.length, jpeg.length - app0End);
        BgrImage decoded = decoder.decode(tagged).get();
        assertEquals(8, decoded.width());
        assertEquals(16, decoded.height());
    }

    @Test
    void unreadableBytesGiveNothing() {
        assertFalse(decoder.decode(new byte[] {1, 2, 3}).isPresent());
        assertFalse(decoder.decode(new byte[0]).isPresent());
        assertFalse(decoder.decode(null).isPresent());
    }

    /** APP1 segment (marker, length, "Exif\0\0", little-endian TIFF with one orientation entry). */
    static byte[] exifSegment(int orientation) {
        byte[] tiff = {'I', 'I', 42, 0, 8, 0, 0, 0, 1, 0, 0x12, 0x01, 3, 0, 1, 0, 0, 0, (byte) orientation, 0,
            0, 0, 0, 0, 0, 0};
        int length = 2 + 6 + tiff.length;
        byte[] out = new byte[2 + length];
        out[0] = (byte) 0xFF;
        out[1] = (byte) 0xE1;
        out[2] = (byte) (length >> 8);
        out[3] = (byte) length;
        byte[] exif = {'E', 'x', 'i', 'f', 0, 0};
        System.arraycopy(exif, 0, out, 4, exif.length);
        System.arraycopy(tiff, 0, out, 10, tiff.length);
        return out;
    }
}
