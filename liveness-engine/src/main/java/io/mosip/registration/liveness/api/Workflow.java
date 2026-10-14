/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.api;

/** The face workflow a session serves. Each one has its own policy (section 10 of docs/decision-flow.md). */
public enum Workflow {
    /** Face capture of a resident during registration. */
    RESIDENT,
    /** Face authentication of an operator: login and packet creation. */
    OPERATOR,
    /** Face authentication of a supervisor: exception review and end of day approval. */
    SUPERVISOR
}
