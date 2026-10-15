/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.api;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import io.mosip.registration.liveness.active.ChallengeType;

/**
 * Parameters of one workflow (section 10 of docs/decision-flow.md). Immutable. The builder starts from the defaults
 * and refuses any invalid value; falling back to a default for a missing or invalid configuration key is done by
 * the configuration provider of the client. {@code enabled} and {@code on_max_attempts} are not here: when liveness
 * is disabled the client creates no session, and it applies {@code on_max_attempts} itself.
 */
public final class LivenessPolicy {

    private final double passThreshold;
    private final double attackThreshold;
    private final int minFrames;
    private final long windowMs;
    private final long samplingMs;
    private final int minFacePx;
    private final double maxYawDeg;
    private final boolean activeEnabled;
    private final int minChallenges;
    private final Set<ChallengeType> challengeTypes;
    private final long challengeTimeoutMs;
    private final long baselineMs;
    private final int maxAttempts;
    private final long passiveTimeoutMs;
    private final boolean diagnostic;

    private LivenessPolicy(Builder b) {
        this.passThreshold = b.passThreshold;
        this.attackThreshold = b.attackThreshold;
        this.minFrames = b.minFrames;
        this.windowMs = b.windowMs;
        this.samplingMs = b.samplingMs;
        this.minFacePx = b.minFacePx;
        this.maxYawDeg = b.maxYawDeg;
        this.activeEnabled = b.activeEnabled;
        this.minChallenges = b.minChallenges;
        this.challengeTypes = Collections.unmodifiableSet(EnumSet.copyOf(b.challengeTypes));
        this.challengeTimeoutMs = b.challengeTimeoutMs;
        this.baselineMs = b.baselineMs;
        this.maxAttempts = b.maxAttempts;
        this.passiveTimeoutMs = b.passiveTimeoutMs;
        this.diagnostic = b.diagnostic;
    }

    /** A builder holding the defaults. */
    public static Builder builder() {
        return new Builder();
    }

    /** The defaults of section 10. */
    public static LivenessPolicy defaults() {
        return builder().build();
    }

    /** {@code passive.threshold.pass}: aggregate score from which liveness is established. */
    public double passThreshold() {
        return passThreshold;
    }

    /** {@code passive.threshold.attack}: aggregate score at or below which an attack is detected. */
    public double attackThreshold() {
        return attackThreshold;
    }

    /** {@code passive.min_frames}: scored frames needed for a passive decision. */
    public int minFrames() {
        return minFrames;
    }

    /** {@code passive.window_ms}: age of the oldest scored frame kept. */
    public long windowMs() {
        return windowMs;
    }

    /** {@code frame.sampling_ms}: shortest time between two analysed frames. */
    public long samplingMs() {
        return samplingMs;
    }

    /** {@code quality.min_face_px}: shortest side of the face box, in pixels. */
    public int minFacePx() {
        return minFacePx;
    }

    /** {@code quality.max_yaw_deg}: largest yaw for a scored frame. */
    public double maxYawDeg() {
        return maxYawDeg;
    }

    /** {@code active.enabled}: whether an ambiguous score starts the active check. */
    public boolean activeEnabled() {
        return activeEnabled;
    }

    /** {@code active.min_challenges}: number of challenges per attempt. */
    public int minChallenges() {
        return minChallenges;
    }

    /** {@code active.challenge_types}: the types drawn from, read-only. */
    public Set<ChallengeType> challengeTypes() {
        return challengeTypes;
    }

    /** {@code active.challenge_timeout_ms}: time allowed for one challenge, hold-still phase included. */
    public long challengeTimeoutMs() {
        return challengeTimeoutMs;
    }

    /** {@code active.baseline_ms}: duration of the hold-still phase. */
    public long baselineMs() {
        return baselineMs;
    }

    /** {@code max_attempts}: attempts allowed in one session. */
    public int maxAttempts() {
        return maxAttempts;
    }

    /** {@code passive.timeout_ms}: time allowed to reach a passive decision. */
    public long passiveTimeoutMs() {
        return passiveTimeoutMs;
    }

    /** {@code diagnostic.enabled}: whether updates carry the frame score. */
    public boolean diagnostic() {
        return diagnostic;
    }

    /** Builder of {@link LivenessPolicy}. Each setter refuses an invalid value with an exception. */
    public static final class Builder {

        private double passThreshold = 0.85;
        private double attackThreshold = 0.30;
        private int minFrames = 5;
        private long windowMs = 3000;
        private long samplingMs = 100;
        private int minFacePx = 120;
        private double maxYawDeg = 20;
        private boolean activeEnabled = true;
        private int minChallenges = 2;
        private Set<ChallengeType> challengeTypes = EnumSet.allOf(ChallengeType.class);
        private long challengeTimeoutMs = 8000;
        private long baselineMs = 600;
        private int maxAttempts = 3;
        private long passiveTimeoutMs = 20000;
        private boolean diagnostic;

        private Builder() {
        }

        /** Between 0 and 1. */
        public Builder passThreshold(double value) {
            passThreshold = unit(value, "passThreshold");
            return this;
        }

        /** Between 0 and 1, below the pass threshold at build time. */
        public Builder attackThreshold(double value) {
            attackThreshold = unit(value, "attackThreshold");
            return this;
        }

        /** At least 1. */
        public Builder minFrames(int value) {
            minFrames = (int) positive(value, "minFrames");
            return this;
        }

        /** At least 1. */
        public Builder windowMs(long value) {
            windowMs = positive(value, "windowMs");
            return this;
        }

        /** 0 or more; 0 analyses every frame that arrives while the worker is idle. */
        public Builder samplingMs(long value) {
            if (value < 0) {
                throw new IllegalArgumentException("samplingMs must not be negative");
            }
            samplingMs = value;
            return this;
        }

        /** At least 1. */
        public Builder minFacePx(int value) {
            minFacePx = (int) positive(value, "minFacePx");
            return this;
        }

        /** Above 0 and at most 90. */
        public Builder maxYawDeg(double value) {
            if (!(value > 0 && value <= 90)) {
                throw new IllegalArgumentException("maxYawDeg must be above 0 and at most 90");
            }
            maxYawDeg = value;
            return this;
        }

        /** Whether an ambiguous passive score starts the active check. */
        public Builder activeEnabled(boolean value) {
            activeEnabled = value;
            return this;
        }

        /** At least 1. */
        public Builder minChallenges(int value) {
            minChallenges = (int) positive(value, "minChallenges");
            return this;
        }

        /** Not empty, no null element. */
        public Builder challengeTypes(Collection<ChallengeType> value) {
            if (value == null || value.isEmpty()) {
                throw new IllegalArgumentException("at least one challenge type is required");
            }
            for (ChallengeType type : value) {
                if (type == null) {
                    throw new IllegalArgumentException("challenge types must not contain null");
                }
            }
            challengeTypes = EnumSet.copyOf(value);
            return this;
        }

        /** At least 1, above the baseline duration at build time. */
        public Builder challengeTimeoutMs(long value) {
            challengeTimeoutMs = positive(value, "challengeTimeoutMs");
            return this;
        }

        /** At least 1. */
        public Builder baselineMs(long value) {
            baselineMs = positive(value, "baselineMs");
            return this;
        }

        /** At least 1. */
        public Builder maxAttempts(int value) {
            maxAttempts = (int) positive(value, "maxAttempts");
            return this;
        }

        /** At least 1. */
        public Builder passiveTimeoutMs(long value) {
            passiveTimeoutMs = positive(value, "passiveTimeoutMs");
            return this;
        }

        /** Whether updates carry the frame score. Keep false in production. */
        public Builder diagnostic(boolean value) {
            diagnostic = value;
            return this;
        }

        /** Checks the combinations, then creates the policy. */
        public LivenessPolicy build() {
            if (attackThreshold >= passThreshold) {
                throw new IllegalArgumentException("attackThreshold must be below passThreshold");
            }
            if (windowMs < (long) minFrames * samplingMs) {
                throw new IllegalArgumentException("windowMs must hold minFrames frames at samplingMs");
            }
            if (baselineMs >= challengeTimeoutMs) {
                throw new IllegalArgumentException("baselineMs must be below challengeTimeoutMs");
            }
            return new LivenessPolicy(this);
        }

        private static double unit(double value, String name) {
            if (!(value >= 0 && value <= 1)) {
                throw new IllegalArgumentException(name + " must be between 0 and 1");
            }
            return value;
        }

        private static long positive(long value, String name) {
            if (value <= 0) {
                throw new IllegalArgumentException(name + " must be positive");
            }
            return value;
        }
    }
}
