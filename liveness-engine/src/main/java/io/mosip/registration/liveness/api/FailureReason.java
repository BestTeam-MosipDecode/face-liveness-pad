/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.api;

/**
 * Why an attempt failed. Only audit and diagnostics may show it as is: the screen shows the generic message for
 * {@link #PAD_ATTACK_DETECTED} and {@link #ENGINE_ERROR}.
 */
public enum FailureReason {
    PAD_ATTACK_DETECTED,
    LIVENESS_BELOW_THRESHOLD,
    CHALLENGE_TIMEOUT,
    CHALLENGE_FAILED,
    FACE_CONTINUITY_LOST,
    NO_FACE,
    MULTIPLE_FACES,
    LOW_QUALITY,
    INVALID_FRAME,
    DEVICE_ERROR,
    ENGINE_ERROR
}
