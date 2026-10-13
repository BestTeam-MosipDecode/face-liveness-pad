/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.active;

/** State of the active check after one analysed frame. */
public final class ChallengeStatus {

    /** Result of the active check. Every value other than {@code RUNNING} and {@code PASSED} is a failure reason. */
    public enum Outcome {
        RUNNING,
        PASSED,
        CHALLENGE_TIMEOUT,
        CHALLENGE_FAILED,
        FACE_CONTINUITY_LOST,
        PAD_ATTACK_DETECTED,
        LIVENESS_BELOW_THRESHOLD
    }

    /** Feedback codes of section 8 of {@code docs/decision-flow.md} used during the active check. */
    public enum Feedback {
        HOLD_STILL,
        CHALLENGE,
        KEEP_GOING,
        ACTION_DETECTED,
        NO_FACE,
        FAILED,
        PASSED
    }

    private final Outcome outcome;
    private final Feedback feedback;
    private final ChallengeType challenge;
    private final int completed;
    private final int total;

    ChallengeStatus(Outcome outcome, Feedback feedback, ChallengeType challenge, int completed, int total) {
        this.outcome = outcome;
        this.feedback = feedback;
        this.challenge = challenge;
        this.completed = completed;
        this.total = total;
    }

    public Outcome outcome() {
        return outcome;
    }

    public Feedback feedback() {
        return feedback;
    }

    /** Challenge in progress, or the last one when the check has ended. */
    public ChallengeType challenge() {
        return challenge;
    }

    public int completed() {
        return completed;
    }

    public int total() {
        return total;
    }

    /** Number of challenges detected divided by their total. */
    public double progress() {
        return (double) completed / total;
    }

    public boolean isFinished() {
        return outcome != Outcome.RUNNING;
    }

    @Override
    public String toString() {
        return String.format("ChallengeStatus(%s, %s, %s, %d/%d)", outcome, feedback, challenge, completed, total);
    }
}
