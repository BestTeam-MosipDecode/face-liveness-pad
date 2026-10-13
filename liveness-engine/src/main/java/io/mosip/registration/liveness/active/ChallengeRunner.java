/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.active;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.mosip.registration.liveness.active.ChallengeStatus.Feedback;
import io.mosip.registration.liveness.active.ChallengeStatus.Outcome;

/**
 * Runs the challenges of one attempt (section 5 of {@code docs/decision-flow.md}).
 *
 * <p>Each challenge has a hold-still phase, where the baseline is the median of at least three frames over
 * {@code active.baseline_ms}, then an instruction phase, where its detector judges each frame against the baseline.
 * The deadline counts from the start of the hold-still phase. During the whole check, the runner also enforces:
 * <ul>
 * <li>face continuity: one face, with a box overlapping the tracked one by at least 0.3, and no more than
 * 1 000 ms without a face;</li>
 * <li>the passive score: the median of the last {@code passive.min_frames} scores must stay above the attack
 * threshold, and the mean of all scores of the check must be above it at the end.</li>
 * </ul>
 *
 * <p>Not thread-safe: the session calls it from its single analysis thread.
 */
public final class ChallengeRunner {

    private final List<ChallengeType> challenges;
    private final ActiveSettings settings;
    private final List<Double> scores = new ArrayList<>();

    private int index;
    private long challengeStartMs;
    private long instructionMs;
    private List<FaceMeasures> baselineSamples = new ArrayList<>();
    private FaceMeasures baseline;
    private ActionDetector detector;
    private FaceBox track;
    private long lastFaceMs;
    private ChallengeStatus status;

    /** Starts the check at {@code startMs}, the timestamp of the frame clock at which the first challenge begins. */
    public ChallengeRunner(List<ChallengeType> challenges, ActiveSettings settings, long startMs) {
        if (challenges == null || challenges.isEmpty()) {
            throw new IllegalArgumentException("at least one challenge is required");
        }
        this.challenges = Collections.unmodifiableList(new ArrayList<>(challenges));
        this.settings = settings;
        this.lastFaceMs = startMs;
        startChallenge(startMs);
        this.status = new ChallengeStatus(Outcome.RUNNING, Feedback.HOLD_STILL, current(), 0, total());
    }

    public List<ChallengeType> challenges() {
        return challenges;
    }

    public ChallengeStatus status() {
        return status;
    }

    /** Baseline of the challenge in progress, or null during its hold-still phase. */
    public FaceMeasures baseline() {
        return baseline;
    }

    /** Feeds one analysed frame. Once the check has ended, the final status is returned unchanged. */
    public ChallengeStatus update(FrameObservation frame) {
        if (status.isFinished()) {
            return status;
        }
        long t = frame.timestampMs();
        if (t > challengeStartMs + settings.challengeTimeoutMs()) {
            return finish(Outcome.CHALLENGE_TIMEOUT);
        }
        if (frame.faceCount() > 1) {
            return finish(Outcome.FACE_CONTINUITY_LOST);
        }
        if (frame.faceCount() == 0) {
            if (t - lastFaceMs > settings.maxNoFaceMs()) {
                return finish(Outcome.FACE_CONTINUITY_LOST);
            }
            return running(Feedback.NO_FACE);
        }

        FaceBox box = frame.box().orElseThrow(IllegalStateException::new);
        if (track != null && track.iou(box) < settings.minTrackOverlap()) {
            return finish(Outcome.FACE_CONTINUITY_LOST);
        }
        track = box;
        lastFaceMs = t;

        if (frame.passiveScore().isPresent()) {
            scores.add(frame.passiveScore().getAsDouble());
            if (scores.size() >= settings.minScoredFrames() && recentMedian() <= settings.attackThreshold()) {
                return finish(Outcome.PAD_ATTACK_DETECTED);
            }
        }

        if (!frame.measures().isPresent()) {
            return running(detector == null ? Feedback.HOLD_STILL : instructionFeedback(t));
        }
        FaceMeasures measures = frame.measures().get();

        if (detector == null) {
            baselineSamples.add(measures);
            if (t - challengeStartMs >= settings.baselineMs()
                    && baselineSamples.size() >= settings.minBaselineFrames()) {
                baseline = FaceMeasures.median(baselineSamples);
                detector = ActionDetector.create(current(), baseline);
                instructionMs = t;
                return running(Feedback.CHALLENGE);
            }
            return running(Feedback.HOLD_STILL);
        }

        switch (detector.update(measures)) {
            case FAILED:
                return finish(Outcome.CHALLENGE_FAILED);
            case DETECTED:
                index++;
                if (index == total()) {
                    return finishActive();
                }
                startChallenge(t);
                return running(Feedback.ACTION_DETECTED);
            default:
                return running(instructionFeedback(t));
        }
    }

    private void startChallenge(long t) {
        challengeStartMs = t;
        baselineSamples = new ArrayList<>();
        baseline = null;
        detector = null;
    }

    private ChallengeType current() {
        return challenges.get(Math.min(index, total() - 1));
    }

    private int total() {
        return challenges.size();
    }

    private Feedback instructionFeedback(long t) {
        return t - instructionMs < settings.keepGoingAfterMs() ? Feedback.CHALLENGE : Feedback.KEEP_GOING;
    }

    private double recentMedian() {
        int from = scores.size() - settings.minScoredFrames();
        double[] recent = new double[settings.minScoredFrames()];
        for (int i = 0; i < recent.length; i++) {
            recent[i] = scores.get(from + i);
        }
        return FaceMeasures.median(recent);
    }

    /** All challenges detected: section 5.5 also requires the mean score of the check above the attack threshold. */
    private ChallengeStatus finishActive() {
        if (scores.isEmpty()) {
            return finish(Outcome.LIVENESS_BELOW_THRESHOLD);
        }
        double mean = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        if (mean <= settings.attackThreshold()) {
            return finish(Outcome.PAD_ATTACK_DETECTED);
        }
        status = new ChallengeStatus(Outcome.PASSED, Feedback.PASSED, current(), total(), total());
        return status;
    }

    private ChallengeStatus running(Feedback feedback) {
        status = new ChallengeStatus(Outcome.RUNNING, feedback, current(), index, total());
        return status;
    }

    private ChallengeStatus finish(Outcome outcome) {
        status = new ChallengeStatus(outcome, Feedback.FAILED, current(), index, total());
        return status;
    }
}
