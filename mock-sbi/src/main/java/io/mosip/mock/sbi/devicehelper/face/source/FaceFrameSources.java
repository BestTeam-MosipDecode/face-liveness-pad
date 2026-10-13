/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.mock.sbi.devicehelper.face.source;

import io.mosip.mock.sbi.util.ApplicationPropertyHelper;

/**
 * Creates the face frame source chosen in application.properties.
 */
public final class FaceFrameSources {
	public static final String KEY_SOURCE = "mosip.mock.sbi.face.source";
	public static final String KEY_WEBCAM_INDEX = "mosip.mock.sbi.face.webcam.index";
	public static final String KEY_WEBCAM_WIDTH = "mosip.mock.sbi.face.webcam.width";
	public static final String KEY_WEBCAM_HEIGHT = "mosip.mock.sbi.face.webcam.height";
	public static final String KEY_WEBCAM_JPEG_QUALITY = "mosip.mock.sbi.face.webcam.jpeg.quality";

	public static final String SOURCE_STATIC = "static";
	public static final String SOURCE_WEBCAM = "webcam";

	private FaceFrameSources() {
	}

	/**
	 * Returns the configured source, or null for the static mode, which keeps the original
	 * behaviour of MockMDS (fixed stream image and prepared ISO records).
	 */
	public static FaceFrameSource create() {
		String source = value(KEY_SOURCE, SOURCE_STATIC).trim().toLowerCase();
		switch (source) {
			case SOURCE_STATIC:
				return null;
			case SOURCE_WEBCAM:
				return new WebcamFrameSource(
						intValue(KEY_WEBCAM_INDEX, 0),
						intValue(KEY_WEBCAM_WIDTH, 640),
						intValue(KEY_WEBCAM_HEIGHT, 480),
						intValue(KEY_WEBCAM_JPEG_QUALITY, 90));
			default:
				throw new IllegalArgumentException("Unknown value for " + KEY_SOURCE + ": " + source);
		}
	}

	private static String value(String key, String defaultValue) {
		String value = ApplicationPropertyHelper.getPropertyKeyValue(key);
		return value == null || value.trim().isEmpty() ? defaultValue : value;
	}

	private static int intValue(String key, int defaultValue) {
		return Integer.parseInt(value(key, String.valueOf(defaultValue)).trim());
	}
}
