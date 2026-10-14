/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.api;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class ApiContractTest {

    @Test
    void listenerMethodsDefaultToNothing() {
        LivenessEventListener listener = new LivenessEventListener() {
        };
        assertDoesNotThrow(() -> listener.onUpdate(null));
        assertDoesNotThrow(() -> listener.onEvent(null));
    }
}
