/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.api;

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
     * @param encodedFrame JPEG or PNG bytes, as received from the device
     * @param timestampMs time of the frame in milliseconds, increasing along the stream
     */
    void submit(byte[] encodedFrame, long timestampMs);

    /** Reports a device problem. Never skipped. Counts as a failed attempt once a frame has been analysed. */
    void deviceEvent(DeviceEvent event, long timestampMs);

    /**
     * Checks the image returned by the capture request (section 6 of docs/decision-flow.md). Call it once, after
     * the state became {@link SessionState#PASSED}; in any other case the image is refused and the state does not
     * change. Waits for the result at most 5 seconds.
     */
    CaptureCheck checkCapture(byte[] encodedImage);

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
