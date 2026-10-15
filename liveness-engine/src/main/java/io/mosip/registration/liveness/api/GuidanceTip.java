/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.api;

/** Additional guidance shown with a feedback code (section 8 of docs/decision-flow.md). */
public enum GuidanceTip {
    /** Keep your face within the capture area. */
    STAY_IN_FRAME,
    /** Improve the lighting. */
    LIGHTING,
    /** Hold still. */
    HOLD_STILL,
    /** Look directly at the camera. */
    LOOK_CAMERA,
    /** Follow the prompted action. */
    FOLLOW_ACTION
}
