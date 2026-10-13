/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.mock.sbi.devicehelper.face.source;

/**
 * Source of the frames sent by the simulated face device in STREAM and used for RCAPTURE.
 */
public interface FaceFrameSource {

	/**
	 * Checks the source once at startup, on the main thread. On macOS, the camera access request
	 * of the system can only be shown from this thread.
	 */
	default void prepare() throws Exception {
	}

	/** Opens the source. Called when a stream or a capture starts. */
	void open() throws Exception;

	/** Returns the next frame as JPEG bytes, or null when no frame is available. */
	byte[] nextFrame();

	/** Releases the source. Called when the stream or the capture ends. */
	void close();
}
