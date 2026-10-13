/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.active;

/** Actions the resident can be asked to perform during the active check. "Left" and "right" refer to the person. */
public enum ChallengeType {
    BLINK,
    SMILE,
    TURN_LEFT,
    TURN_RIGHT
}
