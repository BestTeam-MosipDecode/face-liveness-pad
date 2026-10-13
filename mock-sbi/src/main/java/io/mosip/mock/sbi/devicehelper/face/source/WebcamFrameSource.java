/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.mock.sbi.devicehelper.face.source;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfInt;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Frames from a webcam of the workstation, read with OpenCV and encoded as JPEG.
 *
 * A background thread reads the camera at its own rate and keeps the last frame, so that the
 * stream loop and the capture never wait for the camera.
 */
public class WebcamFrameSource implements FaceFrameSource {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebcamFrameSource.class);

	// The first frames are dark while the exposure settles
	private static final long WARM_UP_MS = 1000;
	private static final long FIRST_FRAME_TIMEOUT_MS = 5000;

	private static boolean nativeLoaded = false;

	private final int index;
	private final int width;
	private final int height;
	private final int jpegQuality;

	private VideoCapture capture;
	private Thread reader;
	private volatile boolean running;
	private volatile byte[] lastFrame;

	public WebcamFrameSource(int index, int width, int height, int jpegQuality) {
		this.index = index;
		this.width = width;
		this.height = height;
		this.jpegQuality = jpegQuality;
	}

	private static synchronized void loadNative() {
		if (!nativeLoaded) {
			nu.pattern.OpenCV.loadLocally();
			nativeLoaded = true;
		}
	}

	@Override
	public void prepare() throws Exception {
		loadNative();
		VideoCapture probe = new VideoCapture(index);
		try {
			if (!probe.isOpened())
				throw new IllegalStateException("Webcam " + index + " cannot be opened");
			LOGGER.info("Webcam {} available", index);
		} finally {
			probe.release();
		}
	}

	@Override
	public synchronized void open() throws Exception {
		if (running)
			return;
		loadNative();
		capture = new VideoCapture(index);
		if (!capture.isOpened()) {
			capture = null;
			throw new IllegalStateException("Webcam " + index + " cannot be opened");
		}
		capture.set(Videoio.CAP_PROP_FRAME_WIDTH, width);
		capture.set(Videoio.CAP_PROP_FRAME_HEIGHT, height);
		LOGGER.info("Webcam {} opened, {}x{}", index,
				(int) capture.get(Videoio.CAP_PROP_FRAME_WIDTH), (int) capture.get(Videoio.CAP_PROP_FRAME_HEIGHT));
		lastFrame = null;
		running = true;
		reader = new Thread(this::readLoop, "webcam-" + index);
		reader.setDaemon(true);
		reader.start();
	}

	private void readLoop() {
		Mat frame = new Mat();
		MatOfByte jpeg = new MatOfByte();
		MatOfInt params = new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, jpegQuality);
		long publishFrom = System.currentTimeMillis() + WARM_UP_MS;
		try {
			while (running) {
				if (!capture.read(frame) || frame.empty())
					continue;
				if (System.currentTimeMillis() < publishFrom)
					continue;
				if (Imgcodecs.imencode(".jpg", frame, jpeg, params))
					lastFrame = jpeg.toArray();
			}
		} catch (Exception ex) {
			LOGGER.error("Webcam {} read failed", index, ex);
		} finally {
			frame.release();
			jpeg.release();
			params.release();
		}
	}

	/** Returns the last frame, waiting for the first one after the warm-up. */
	@Override
	public byte[] nextFrame() {
		long deadline = System.currentTimeMillis() + FIRST_FRAME_TIMEOUT_MS;
		while (running && lastFrame == null && System.currentTimeMillis() < deadline) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				return null;
			}
		}
		return lastFrame;
	}

	@Override
	public synchronized void close() {
		if (!running)
			return;
		running = false;
		try {
			reader.join(2000);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		capture.release();
		capture = null;
		reader = null;
		lastFrame = null;
		LOGGER.info("Webcam {} released", index);
	}
}
