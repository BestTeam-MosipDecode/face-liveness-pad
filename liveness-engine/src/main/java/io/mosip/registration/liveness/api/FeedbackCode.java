/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.api;

/**
 * What the screen should tell the user after a frame (section 8 of docs/decision-flow.md). The client maps each code
 * to a message key; a detected attack is always shown with the generic failure message.
 */
public enum FeedbackCode {
    CHECKING,
    NO_FACE,
    MULTIPLE_FACES,
    LOW_QUALITY,
    HOLD_STILL,
    CHALLENGE,
    KEEP_GOING,
    ACTION_DETECTED,
    SUCCESS,
    FAILED,
    MAX_ATTEMPTS_EXCEEDED,
    DEVICE_ERROR
}
