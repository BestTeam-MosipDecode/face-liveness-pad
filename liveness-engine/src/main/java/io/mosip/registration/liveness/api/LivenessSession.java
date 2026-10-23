/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.api;

import io.mosip.registration.liveness.image.BgrImage;

/**
 * One liveness session: one resident face capture, or one operator or supervisor face authentication. Frames go in
 * through {@link #submit}; results come out through the listener given to {@link LivenessEngine#newSession}. Every
 * method is safe to call from any thread.
 */
public interface LivenessSession extends AutoCloseable {

    /**
     * Offers one frame of the device stream. Returns at once. The frame is skipped while another one is analysed or
     * when it comes less than {@code samplingMs} after the last analysed frame.
     *
     * @param encodedFrame JPEG or PNG bytes, as received from the device. The frame is decoded later on the worker
     *            thread: the caller must not reuse or modify the array.
     * @param timestampMs time of the frame in milliseconds, from a monotonic clock such as
     *            {@code System.nanoTime() / 1_000_000}. A time that goes back during an attempt ends it with
     *            {@code DEVICE_ERROR}, so the wall clock, which moves when the system time is adjusted, must not be
     *            used.
     */
    void submit(byte[] encodedFrame, long timestampMs);

    /**
     * As {@link #submit(byte[], long)}, with the passive score that the face device computed for the frame and sent
     * with it (section 4 of docs/decision-flow.md). On a frame that the session can use, this score replaces the one
     * the session would compute; a score outside 0 to 1 is ignored. Without a device score, use
     * {@link #submit(byte[], long)}.
     */
    default void submit(byte[] encodedFrame, long timestampMs, double deviceScore) {
        submit(encodedFrame, timestampMs);
    }

    /** Reports a device problem. Never skipped. Counts as a failed attempt once a frame has been analysed. */
    void deviceEvent(DeviceEvent event, long timestampMs);

    /**
     * Checks the image returned by the capture request (section 6 of docs/decision-flow.md). Call it once, after
     * the state became {@link SessionState#PASSED}; in any other case the image is refused and the state does not
     * change. Waits for the result at most 5 seconds; a check that has not started by then never runs.
     *
     * @param encodedImage JPEG or PNG bytes, decoded by the platform decoder of the engine. RCAPTURE returns an
     *            ISO/IEC 19794-5 record holding a JPEG 2000 image: convert it first, or use
     *            {@link #checkCapture(BgrImage)}.
     */
    CaptureCheck checkCapture(byte[] encodedImage);

    /**
     * Same check on an image already decoded by the client, for example the {@code BufferedImage} that the MOSIP
     * {@code FaceDecoder.convertFaceISOToBufferedImage} extracts from the ISO/IEC 19794-5 record of RCAPTURE, turned
     * into pixels with {@code ImageIoFrameDecoder.toBgr}. No JPEG round trip.
     */
    CaptureCheck checkCapture(BgrImage capturedImage);

    /**
     * Starts a new attempt after a failure, with an empty score window and new challenges.
     *
     * @return false when the session is not in a failed attempt or no attempt is left
     */
    boolean retry();

    /** Last update published, also available without a listener. */
    LivenessUpdate current();

    /** Workflow of this session. */
    Workflow workflow();

    /** Stops the session: later calls are ignored. Opening a new session closes this one too. */
    @Override
    void close();
}
