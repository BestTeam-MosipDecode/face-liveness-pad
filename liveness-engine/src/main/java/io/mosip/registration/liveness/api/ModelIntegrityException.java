/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.api;

/** A model file is missing or differs from models.json: the engine refuses to start. */
public final class ModelIntegrityException extends Exception {

    private static final long serialVersionUID = 1L;

    /** With a message naming the file and the problem. */
    public ModelIntegrityException(String message) {
        super(message);
    }

    /** With a message and the underlying cause. */
    public ModelIntegrityException(String message, Throwable cause) {
        super(message, cause);
    }
}
