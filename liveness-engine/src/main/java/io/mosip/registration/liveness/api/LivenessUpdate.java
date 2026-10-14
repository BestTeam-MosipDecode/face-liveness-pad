/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.api;

import java.util.Optional;
import java.util.OptionalDouble;

import io.mosip.registration.liveness.active.ChallengeType;

/**
 * State of a session after an analysed frame, a device event, a capture check or a retry. Created by the engine and
 * delivered to {@link LivenessEventListener#onUpdate}. Never holds an image; the score only in diagnostic mode.
 */
public final class LivenessUpdate {

    private final SessionState state;
    private final FeedbackCode feedback;
    private final GuidanceTip tip;
    private final ChallengeType challenge;
    private final double progress;
    private final int attempt;
    private final int maxAttempts;
    private final FailureReason failureReason;
    private final boolean retryAvailable;
    private final boolean finished;
    private final Double diagnosticScore;

    /**
     * Created by the engine. {@code tip}, {@code challenge}, {@code failureReason} and {@code diagnosticScore} may
     * be null.
     */
    public LivenessUpdate(SessionState state, FeedbackCode feedback, GuidanceTip tip, ChallengeType challenge,
            double progress, int attempt, int maxAttempts, FailureReason failureReason, boolean retryAvailable,
            boolean finished, Double diagnosticScore) {
        this.state = state;
        this.feedback = feedback;
        this.tip = tip;
        this.challenge = challenge;
        this.progress = progress;
        this.attempt = attempt;
        this.maxAttempts = maxAttempts;
        this.failureReason = failureReason;
        this.retryAvailable = retryAvailable;
        this.finished = finished;
        this.diagnosticScore = diagnosticScore;
    }

    /** Current state of the session. */
    public SessionState state() {
        return state;
    }

    /** Code of the message to show. */
    public FeedbackCode feedback() {
        return feedback;
    }

    /** Additional guidance, if any. */
    public Optional<GuidanceTip> tip() {
        return Optional.ofNullable(tip);
    }

    /** Challenge in progress, or the last one, during the active check. */
    public Optional<ChallengeType> challenge() {
        return Optional.ofNullable(challenge);
    }

    /** Progress between 0 and 1: scored frames in the passive check, detected challenges in the active check. */
    public double progress() {
        return progress;
    }

    /** Number of the current attempt, from 1; 0 before the first analysed frame. */
    public int attempt() {
        return attempt;
    }

    /** Maximum number of attempts of the policy. */
    public int maxAttempts() {
        return maxAttempts;
    }

    /** Reason of the failure when the state is {@link SessionState#FAILED}. */
    public Optional<FailureReason> failureReason() {
        return Optional.ofNullable(failureReason);
    }

    /** True when the attempt failed and {@code retry()} can start another one. */
    public boolean retryAvailable() {
        return retryAvailable;
    }

    /** True when the session is over: captured image accepted, or attempts exhausted. */
    public boolean finished() {
        return finished;
    }

    /** Last frame score, present only when the policy enables the diagnostic mode. */
    public OptionalDouble diagnosticScore() {
        return diagnosticScore == null ? OptionalDouble.empty() : OptionalDouble.of(diagnosticScore);
    }

    @Override
    public String toString() {
        return String.format("LivenessUpdate(%s, %s, tip=%s, challenge=%s, progress=%.2f, attempt=%d/%d, reason=%s, "
            + "retry=%s, finished=%s)", state, feedback, tip, challenge, progress, attempt, maxAttempts, failureReason,
            retryAvailable, finished);
    }
}
