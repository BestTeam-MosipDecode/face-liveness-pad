/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.api;

/** States of a liveness session (section 1 of docs/decision-flow.md). */
public enum SessionState {
    /** No frame analysed in the current attempt. */
    INIT,
    /** Good frames are scored, the user does nothing. */
    PASSIVE_CHECK,
    /** The user performs facial actions drawn at random. */
    ACTIVE_CHALLENGE,
    /** Liveness is established; the client may send the capture request and submit the image to checkCapture. */
    PASSED,
    /** The attempt is over, with a failure reason. */
    FAILED
}
