/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.mock.sbi.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import com.github.jaiimageio.jpeg2000.J2KImageWriteParam;

/**
 * Builds an ISO/IEC 19794-5:2011 face record from a JPEG frame, with the image stored in
 * JPEG 2000 like the prepared records of MockMDS.
 */
public final class FaceIsoBuilder {

	/** Bits per pixel of the lossy JPEG 2000 image: about 40 KB for 640x480. */
	private static final double ENCODING_RATE = 1.0;

	private FaceIsoBuilder() {
	}

	public static byte[] fromJpeg(String purpose, byte[] jpeg) throws Exception {
		BufferedImage image = ImageIO.read(new ByteArrayInputStream(jpeg));
		if (image == null)
			throw new IOException("Frame is not a readable image");
		return BioUtilHelper.getFaceIsoFromJP2000(purpose, "Full face", toJpeg2000(toRgb(image)));
	}

	private static BufferedImage toRgb(BufferedImage image) {
		if (image.getType() == BufferedImage.TYPE_3BYTE_BGR || image.getType() == BufferedImage.TYPE_INT_RGB)
			return image;
		BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
		rgb.getGraphics().drawImage(image, 0, 0, null);
		return rgb;
	}

	private static byte[] toJpeg2000(BufferedImage image) throws IOException {
		ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg2000").next();
		J2KImageWriteParam param = (J2KImageWriteParam) writer.getDefaultWriteParam();
		param.setLossless(false);
		param.setEncodingRate(ENCODING_RATE);
		param.setWriteCodeStreamOnly(false);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
			writer.setOutput(ios);
			writer.write(null, new IIOImage(image, null, null), param);
		} finally {
			writer.dispose();
		}
		return out.toByteArray();
	}
}
