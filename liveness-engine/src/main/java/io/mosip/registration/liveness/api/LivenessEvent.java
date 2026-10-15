/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.api;

import java.util.Optional;

import io.mosip.registration.liveness.active.ChallengeType;

/**
 * Audit event of a session (section 9 of docs/decision-flow.md). Carries codes, a duration and the model set
 * version; never a score, a measure or an image.
 */
public final class LivenessEvent {

    /** Event types, one per transition of section 9. */
    public enum Type {
        SESSION_STARTED,
        ACTIVE_TRIGGERED,
        CHALLENGE_PASSED,
        CHALLENGE_FAILED,
        PASSED,
        FAILED,
        MAX_ATTEMPTS,
        DEVICE_ERROR
    }

    private final Type type;
    private final Workflow workflow;
    private final int attempt;
    private final long durationMs;
    private final String modelSetVersion;
    private final FailureReason failureReason;
    private final ChallengeType challenge;
    private final DeviceEvent deviceEvent;

    /** Created by the engine; {@code failureReason}, {@code challenge} and {@code deviceEvent} are nullable. */
    public LivenessEvent(Type type, Workflow workflow, int attempt, long durationMs, String modelSetVersion,
            FailureReason failureReason, ChallengeType challenge, DeviceEvent deviceEvent) {
        this.type = type;
        this.workflow = workflow;
        this.attempt = attempt;
        this.durationMs = durationMs;
        this.modelSetVersion = modelSetVersion;
        this.failureReason = failureReason;
        this.challenge = challenge;
        this.deviceEvent = deviceEvent;
    }

    /** What happened. */
    public Type type() {
        return type;
    }

    /** Workflow of the session. */
    public Workflow workflow() {
        return workflow;
    }

    /** Attempt number, 0 before the first analysed frame. */
    public int attempt() {
        return attempt;
    }

    /** Time since the start of the attempt, 0 outside an attempt. */
    public long durationMs() {
        return durationMs;
    }

    /** Version of the model set. */
    public String modelSetVersion() {
        return modelSetVersion;
    }

    /** Failure reason, for FAILED, CHALLENGE_FAILED, MAX_ATTEMPTS and DEVICE_ERROR. */
    public Optional<FailureReason> failureReason() {
        return Optional.ofNullable(failureReason);
    }

    /** Challenge type, for CHALLENGE_PASSED and CHALLENGE_FAILED. */
    public Optional<ChallengeType> challenge() {
        return Optional.ofNullable(challenge);
    }

    /** Device event, for DEVICE_ERROR. */
    public Optional<DeviceEvent> deviceEvent() {
        return Optional.ofNullable(deviceEvent);
    }

    @Override
    public String toString() {
        return String.format("LivenessEvent(%s, %s, attempt=%d, %d ms, models %s, reason=%s, challenge=%s, device=%s)",
            type, workflow, attempt, durationMs, modelSetVersion, failureReason, challenge, deviceEvent);
    }
}
