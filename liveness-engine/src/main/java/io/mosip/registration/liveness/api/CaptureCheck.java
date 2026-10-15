/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.api;

import java.util.Optional;

/** Result of the check of the captured image (section 6 of docs/decision-flow.md). */
public final class CaptureCheck {

    private final boolean accepted;
    private final FailureReason failureReason;
    private final String modelSetVersion;

    private CaptureCheck(boolean accepted, FailureReason failureReason, String modelSetVersion) {
        this.accepted = accepted;
        this.failureReason = failureReason;
        this.modelSetVersion = modelSetVersion;
    }

    /** The image may be stored or matched. */
    public static CaptureCheck accepted(String modelSetVersion) {
        return new CaptureCheck(true, null, modelSetVersion);
    }

    /** The image must not be used. */
    public static CaptureCheck rejected(FailureReason reason, String modelSetVersion) {
        return new CaptureCheck(false, reason, modelSetVersion);
    }

    /** True when the captured image passed every check. */
    public boolean accepted() {
        return accepted;
    }

    /** Why the image was refused. */
    public Optional<FailureReason> failureReason() {
        return Optional.ofNullable(failureReason);
    }

    /** Version of the model set that took the decision, for the audit trail. */
    public String modelSetVersion() {
        return modelSetVersion;
    }

    @Override
    public String toString() {
        return accepted ? "CaptureCheck(accepted)" : "CaptureCheck(rejected, " + failureReason + ")";
    }
}
