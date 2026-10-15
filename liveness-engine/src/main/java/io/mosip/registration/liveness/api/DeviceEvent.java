/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.api;

/** Device problems reported by the frame source adapter of the client. */
public enum DeviceEvent {
    /** No device answers. */
    UNAVAILABLE,
    /** The device answered but the stream could not be opened. */
    CONNECTION_FAILED,
    /** The stream stopped while frames were expected. */
    STREAM_INTERRUPTED
}
