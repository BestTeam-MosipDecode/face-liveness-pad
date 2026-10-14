/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.platform.desktop;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

import javax.imageio.ImageIO;

import io.mosip.registration.liveness.api.FrameDecoder;
import io.mosip.registration.liveness.image.BgrImage;

/**
 * Desktop decoder: {@code ImageIO} for JPEG and PNG, then the EXIF orientation, as OpenCV {@code imread} does. The
 * only class of the engine that uses {@code java.awt}.
 */
public final class ImageIoFrameDecoder implements FrameDecoder {

    @Override
    public Optional<BgrImage> decode(byte[] encoded) {
        if (encoded == null || encoded.length == 0) {
            return Optional.empty();
        }
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(encoded));
            return image == null ? Optional.empty()
                : Optional.of(toBgr(image).orient(BgrImage.exifOrientation(encoded)));
        } catch (IOException | RuntimeException e) {
            return Optional.empty();
        }
    }

    /** Copies a decoded image into the OpenCV layout (rows, columns, B, G, R). */
    public static BgrImage toBgr(BufferedImage image) {
        BufferedImage bgr = image;
        if (image.getType() != BufferedImage.TYPE_3BYTE_BGR) {
            bgr = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            bgr.getGraphics().drawImage(image, 0, 0, null);
        }
        byte[] data = ((DataBufferByte) bgr.getRaster().getDataBuffer()).getData();
        return new BgrImage(bgr.getWidth(), bgr.getHeight(), data.clone());
    }
}
