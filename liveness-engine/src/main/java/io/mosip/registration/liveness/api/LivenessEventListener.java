/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.api;

/**
 * Receives the updates and the audit events of a session. Both methods are called on the worker thread of the
 * session: a JavaFX client moves to its own thread with {@code Platform.runLater}. Exceptions thrown here are
 * ignored by the engine. Calling {@code retry()} or {@code checkCapture()} from here is allowed.
 *
 * <p>Both methods must return quickly and never block: while they run, the session analyses no frame and does not
 * follow the face. Hand slow work, such as the capture request to the device, to another thread.
 */
public interface LivenessEventListener {

    /** New state, feedback and progress of the session. */
    default void onUpdate(LivenessUpdate update) {
    }

    /** Audit event, to record with the audit service of the client. */
    default void onEvent(LivenessEvent event) {
    }
}
