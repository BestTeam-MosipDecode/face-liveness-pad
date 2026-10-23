/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.active;

/**
 * Parameters of the active check. Defaults follow section 10 of {@code docs/decision-flow.md}; the configuration
 * provider of the client overrides the configuration keys.
 */
public final class ActiveSettings {

    /** {@code active.baseline_ms}. */
    private long baselineMs = 600;
    /** {@code active.challenge_timeout_ms}. */
    private long challengeTimeoutMs = 15000;
    /** {@code passive.threshold.attack}. */
    private double attackThreshold = 0.30;
    /** {@code passive.min_frames}. */
    private int minScoredFrames = 5;
    /** Engine constants. */
    private int minBaselineFrames = 3;
    private long maxNoFaceMs = 1000;
    private double minTrackOverlap = 0.3;
    /** Time after the instruction during which the feedback stays {@code CHALLENGE} before {@code KEEP_GOING}. */
    private long keepGoingAfterMs = 2000;

    public long baselineMs() {
        return baselineMs;
    }

    public ActiveSettings baselineMs(long value) {
        baselineMs = requirePositive(value, "baselineMs");
        return this;
    }

    public long challengeTimeoutMs() {
        return challengeTimeoutMs;
    }

    public ActiveSettings challengeTimeoutMs(long value) {
        challengeTimeoutMs = requirePositive(value, "challengeTimeoutMs");
        return this;
    }

    public double attackThreshold() {
        return attackThreshold;
    }

    public ActiveSettings attackThreshold(double value) {
        if (value < 0 || value > 1) {
            throw new IllegalArgumentException("attackThreshold must be between 0 and 1");
        }
        attackThreshold = value;
        return this;
    }

    public int minScoredFrames() {
        return minScoredFrames;
    }

    public ActiveSettings minScoredFrames(int value) {
        minScoredFrames = (int) requirePositive(value, "minScoredFrames");
        return this;
    }

    public int minBaselineFrames() {
        return minBaselineFrames;
    }

    public ActiveSettings minBaselineFrames(int value) {
        minBaselineFrames = (int) requirePositive(value, "minBaselineFrames");
        return this;
    }

    public long maxNoFaceMs() {
        return maxNoFaceMs;
    }

    public ActiveSettings maxNoFaceMs(long value) {
        maxNoFaceMs = requirePositive(value, "maxNoFaceMs");
        return this;
    }

    public double minTrackOverlap() {
        return minTrackOverlap;
    }

    public ActiveSettings minTrackOverlap(double value) {
        if (value < 0 || value > 1) {
            throw new IllegalArgumentException("minTrackOverlap must be between 0 and 1");
        }
        minTrackOverlap = value;
        return this;
    }

    public long keepGoingAfterMs() {
        return keepGoingAfterMs;
    }

    public ActiveSettings keepGoingAfterMs(long value) {
        keepGoingAfterMs = requirePositive(value, "keepGoingAfterMs");
        return this;
    }

    private static long requirePositive(long value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
