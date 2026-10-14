/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.passive;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.OptionalDouble;

/**
 * Passive decision over a window of frame scores (section 4 of {@code docs/decision-flow.md}), as in
 * {@code training/liveness/passive.py}. The window keeps the scored frames of the current track from the last
 * {@code passive.window_ms}. Once it holds {@code passive.min_frames} frames, their median is compared with the pass
 * and attack thresholds.
 */
public final class PassiveWindow {

    /** Decision of the passive check after the last frame. */
    public enum Decision {
        PENDING,
        PASSED,
        ATTACK,
        CHALLENGE
    }

    private final double passThreshold;
    private final double attackThreshold;
    private final int minFrames;
    private final long windowMs;
    private final Deque<double[]> frames = new ArrayDeque<>();

    public PassiveWindow(double passThreshold, double attackThreshold, int minFrames, long windowMs) {
        if (attackThreshold >= passThreshold) {
            throw new IllegalArgumentException("the attack threshold must be below the pass threshold");
        }
        this.passThreshold = passThreshold;
        this.attackThreshold = attackThreshold;
        this.minFrames = minFrames;
        this.windowMs = windowMs;
    }

    /** Defaults of section 10: pass 0.85, attack 0.30, 5 frames, 3 000 ms. */
    public PassiveWindow() {
        this(0.85, 0.30, 5, 3000);
    }

    /** Starts a new track: the face box changed or the face was lost. */
    public void clear() {
        frames.clear();
    }

    public void add(long timestampMs, double score) {
        frames.addLast(new double[] {timestampMs, score});
        while (!frames.isEmpty() && frames.peekFirst()[0] < timestampMs - windowMs) {
            frames.removeFirst();
        }
    }

    public int size() {
        return frames.size();
    }

    /** Number of scored frames divided by {@code passive.min_frames}, at most 1: the progress of section 8. */
    public double progress() {
        return Math.min(1.0, (double) frames.size() / minFrames);
    }

    /** Median score of the window, empty while it holds fewer than {@code passive.min_frames} frames. */
    public OptionalDouble aggregate() {
        if (frames.size() < minFrames) {
            return OptionalDouble.empty();
        }
        double[] scores = frames.stream().mapToDouble(f -> f[1]).toArray();
        Arrays.sort(scores);
        int middle = scores.length / 2;
        return OptionalDouble.of(scores.length % 2 == 1 ? scores[middle] : (scores[middle - 1] + scores[middle]) / 2);
    }

    public Decision decision() {
        OptionalDouble score = aggregate();
        if (!score.isPresent()) {
            return Decision.PENDING;
        }
        if (score.getAsDouble() >= passThreshold) {
            return Decision.PASSED;
        }
        if (score.getAsDouble() <= attackThreshold) {
            return Decision.ATTACK;
        }
        return Decision.CHALLENGE;
    }
}
