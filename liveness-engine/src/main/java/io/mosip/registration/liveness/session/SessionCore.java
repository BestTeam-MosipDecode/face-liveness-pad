/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.session;

import java.time.Clock;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import io.mosip.registration.liveness.active.ActiveSettings;
import io.mosip.registration.liveness.active.ChallengeGenerator;
import io.mosip.registration.liveness.active.ChallengeRunner;
import io.mosip.registration.liveness.active.ChallengeStatus;
import io.mosip.registration.liveness.active.ChallengeType;
import io.mosip.registration.liveness.active.FrameObservation;
import io.mosip.registration.liveness.api.DeviceEvent;
import io.mosip.registration.liveness.api.FailureReason;
import io.mosip.registration.liveness.api.FeedbackCode;
import io.mosip.registration.liveness.api.GuidanceTip;
import io.mosip.registration.liveness.api.LivenessEvent;
import io.mosip.registration.liveness.api.LivenessEventListener;
import io.mosip.registration.liveness.api.LivenessPolicy;
import io.mosip.registration.liveness.api.LivenessUpdate;
import io.mosip.registration.liveness.api.SessionState;
import io.mosip.registration.liveness.api.Workflow;
import io.mosip.registration.liveness.face.FaceDetection;
import io.mosip.registration.liveness.image.BgrImage;
import io.mosip.registration.liveness.passive.FrameAnalyzer.FrameAnalysis;
import io.mosip.registration.liveness.passive.PassiveWindow;
import io.mosip.registration.liveness.passive.QualityResult;

/**
 * State machine of one liveness session (sections 3 to 9 of docs/decision-flow.md). Synchronous and not
 * thread-safe: the worker of the session calls it from its single thread. Frame timestamps drive windows, deadlines
 * and timeouts; the injected clock drives durations and metrics.
 */
final class SessionCore {

    /** Longest time without a face once a face is followed (section 10.3). */
    static final long MAX_NO_FACE_MS = 1000;
    /** Minimum overlap with the tracked face (section 3.4). */
    static final double MIN_TRACK_OVERLAP = 0.3;

    private final Workflow workflow;
    private final LivenessPolicy policy;
    private final Analyzer analyzer;
    private final Clock clock;
    private final Random random;
    private final String modelSetVersion;
    private final LivenessEventListener listener;
    private final MetricsRecorder metrics;
    private final PassiveWindow window;
    private final FaceTracker tracker = new FaceTracker(MIN_TRACK_OVERLAP, MAX_NO_FACE_MS);
    private final Map<FailureReason, Integer> guidance = new EnumMap<>(FailureReason.class);

    private SessionState state = SessionState.INIT;
    private int attempt;
    private long attemptStartMs;
    private long firstFrameT;
    private long lastFrameT;
    private int imageWidth;
    private int imageHeight;
    private GuidanceTip lastTip;
    private Double lastScore;
    private ChallengeRunner runner;
    private int completedChallenges;
    private long challengeClockStart;
    private boolean finished;
    private int invalidFrames;
    private volatile LivenessUpdate current;

    SessionCore(Workflow workflow, LivenessPolicy policy, Analyzer analyzer, Clock clock, Random random,
            String modelSetVersion, LivenessEventListener listener, MetricsRecorder metrics) {
        this.workflow = workflow;
        this.policy = policy;
        this.analyzer = analyzer;
        this.clock = clock;
        this.random = random;
        this.modelSetVersion = modelSetVersion;
        this.listener = listener;
        this.metrics = metrics;
        this.window = new PassiveWindow(policy.passThreshold(), policy.attackThreshold(), policy.minFrames(),
            policy.windowMs());
        this.current = new LivenessUpdate(SessionState.INIT, FeedbackCode.CHECKING, null, null, 0, 0,
            policy.maxAttempts(), null, false, false, null);
    }

    /** Last update published. Safe to read from any thread. */
    LivenessUpdate current() {
        return current;
    }

    /** Frames that could not be decoded, for diagnostics. */
    int invalidFrames() {
        return invalidFrames;
    }

    /** One frame of the stream, or null when it could not be decoded. */
    void onFrame(BgrImage image, long t) {
        if (finished || state == SessionState.FAILED) {
            return;
        }
        if (image == null) {
            invalidFrames++;
            return;
        }
        if (state == SessionState.INIT) {
            startAttempt(t);
        }
        lastFrameT = t;
        long started = clock.millis();
        FrameAnalysis analysis;
        try {
            analysis = analyzer.analyze(image, t, state == SessionState.ACTIVE_CHALLENGE);
        } catch (Exception e) {
            fail(FailureReason.ENGINE_ERROR, null);
            return;
        }
        metrics.frameAnalysed(clock.millis() - started);
        imageWidth = image.width();
        imageHeight = image.height();
        switch (state) {
            case PASSIVE_CHECK:
                passive(analysis, t);
                break;
            case ACTIVE_CHALLENGE:
                active(analysis, t);
                break;
            case PASSED:
                passed(analysis, t);
                break;
            default:
                break;
        }
    }

    private void startAttempt(long t) {
        attempt++;
        attemptStartMs = clock.millis();
        firstFrameT = t;
        state = SessionState.PASSIVE_CHECK;
        window.clear();
        tracker.reset();
        guidance.clear();
        lastTip = null;
        lastScore = null;
        runner = null;
        completedChallenges = 0;
        metrics.attemptStarted();
        emit(LivenessEvent.Type.SESSION_STARTED, null, null, null);
    }

    private void passive(FrameAnalysis analysis, long t) {
        if (t - firstFrameT > policy.passiveTimeoutMs()) {
            failPassiveTimeout();
            return;
        }
        List<FaceDetection> faces = analysis.faces();
        if (faces.isEmpty()) {
            guide(FailureReason.NO_FACE, GuidanceTip.STAY_IN_FRAME);
            publish(FeedbackCode.NO_FACE, null, window.progress(), null);
            return;
        }
        if (faces.size() > 1) {
            guide(FailureReason.MULTIPLE_FACES, null);
            publish(FeedbackCode.MULTIPLE_FACES, null, window.progress(), null);
            return;
        }
        if (!tracker.follow(faces.get(0).box(), t)) {
            window.clear();
        }
        QualityResult quality = analysis.quality();
        if (quality != null && !quality.passed()) {
            GuidanceTip tip = GuidanceTip.valueOf(quality.tip().get().name());
            guide(FailureReason.LOW_QUALITY, tip);
            publish(FeedbackCode.LOW_QUALITY, tip, window.progress(), null);
            return;
        }
        if (analysis.score() == null) {
            publish(FeedbackCode.CHECKING, null, window.progress(), null);
            return;
        }
        lastScore = analysis.score();
        window.add(t, analysis.score());
        switch (window.decision()) {
            case PASSED:
                enterPassed();
                break;
            case ATTACK:
                fail(FailureReason.PAD_ATTACK_DETECTED, null);
                break;
            case CHALLENGE:
                if (policy.activeEnabled()) {
                    enterActive(t);
                } else {
                    fail(FailureReason.LIVENESS_BELOW_THRESHOLD, null);
                }
                break;
            default:
                publish(FeedbackCode.CHECKING, null, window.progress(), null);
        }
    }

    private void guide(FailureReason reason, GuidanceTip tip) {
        guidance.merge(reason, 1, Integer::sum);
        lastTip = tip;
    }

    /** Section 4: the attempt fails with the guidance given most often. */
    private void failPassiveTimeout() {
        FailureReason reason = FailureReason.LIVENESS_BELOW_THRESHOLD;
        int most = 0;
        for (Map.Entry<FailureReason, Integer> entry : guidance.entrySet()) {
            if (entry.getValue() > most) {
                most = entry.getValue();
                reason = entry.getKey();
            }
        }
        fail(reason, most == 0 ? null : lastTip);
    }

    private void enterPassed() {
        state = SessionState.PASSED;
        publish(FeedbackCode.HOLD_STILL, null, 1.0, null);
    }

    private void enterActive(long t) {
        List<ChallengeType> challenges = new ChallengeGenerator(policy.challengeTypes(), policy.minChallenges(),
            random).next();
        runner = new ChallengeRunner(challenges, activeSettings(), t);
        state = SessionState.ACTIVE_CHALLENGE;
        completedChallenges = 0;
        challengeClockStart = clock.millis();
        emit(LivenessEvent.Type.ACTIVE_TRIGGERED, null, null, null);
        publish(FeedbackCode.HOLD_STILL, null, 0, challenges.get(0));
    }

    private void active(FrameAnalysis analysis, long t) {
        FrameObservation observation = analysis.toObservation();
        // The runner starts with an empty track: the session checks continuity with the face of the passive check.
        if (observation.box().isPresent() && !tracker.follow(observation.box().get(), t)) {
            fail(FailureReason.FACE_CONTINUITY_LOST, null);
            return;
        }
        if (analysis.score() != null) {
            lastScore = analysis.score();
        }
        ChallengeStatus status = runner.update(observation);
        while (completedChallenges < status.completed()) {
            ChallengeType done = runner.challenges().get(completedChallenges);
            completedChallenges++;
            long now = clock.millis();
            metrics.challengeCompleted(now - challengeClockStart);
            challengeClockStart = now;
            emit(LivenessEvent.Type.CHALLENGE_PASSED, null, done, null);
        }
        switch (status.outcome()) {
            case RUNNING:
                publish(FeedbackCode.valueOf(status.feedback().name()), null, status.progress(), status.challenge());
                break;
            case PASSED:
                enterPassed();
                break;
            default:
                FailureReason reason = FailureReason.valueOf(status.outcome().name());
                boolean missed = reason == FailureReason.CHALLENGE_TIMEOUT || reason == FailureReason.CHALLENGE_FAILED;
                if (missed) {
                    emit(LivenessEvent.Type.CHALLENGE_FAILED, reason, status.challenge(), null);
                }
                fail(reason, missed ? GuidanceTip.FOLLOW_ACTION : null);
        }
    }

    /** Section 6: after PASSED, the face keeps being followed with the rules of the active check. */
    private void passed(FrameAnalysis analysis, long t) {
        List<FaceDetection> faces = analysis.faces();
        if (faces.size() > 1) {
            fail(FailureReason.FACE_CONTINUITY_LOST, null);
            return;
        }
        if (faces.isEmpty()) {
            if (tracker.withinNoFaceLimit(t)) {
                publish(FeedbackCode.NO_FACE, null, 1.0, null);
            } else {
                fail(FailureReason.FACE_CONTINUITY_LOST, GuidanceTip.STAY_IN_FRAME);
            }
            return;
        }
        if (!tracker.follow(faces.get(0).box(), t)) {
            fail(FailureReason.FACE_CONTINUITY_LOST, null);
            return;
        }
        publish(FeedbackCode.HOLD_STILL, null, 1.0, null);
    }

    private ActiveSettings activeSettings() {
        return new ActiveSettings().baselineMs(policy.baselineMs()).challengeTimeoutMs(policy.challengeTimeoutMs())
            .attackThreshold(policy.attackThreshold()).minScoredFrames(policy.minFrames());
    }

    /** Ends the attempt. Detected attacks and engine errors carry no tip, as section 8 asks. */
    private void fail(FailureReason reason, GuidanceTip tip) {
        state = SessionState.FAILED;
        metrics.attemptFailed();
        GuidanceTip shown = reason == FailureReason.PAD_ATTACK_DETECTED || reason == FailureReason.ENGINE_ERROR
            ? null : tip;
        emit(LivenessEvent.Type.FAILED, reason, null, null);
        boolean exhausted = attempt >= policy.maxAttempts();
        if (exhausted) {
            finished = true;
            emit(LivenessEvent.Type.MAX_ATTEMPTS, reason, null, null);
        }
        update(new LivenessUpdate(SessionState.FAILED,
            exhausted ? FeedbackCode.MAX_ATTEMPTS_EXCEEDED : FeedbackCode.FAILED, shown, null, current.progress(),
            attempt, policy.maxAttempts(), reason, !exhausted, exhausted, diagnosticScore()));
    }

    private void publish(FeedbackCode feedback, GuidanceTip tip, double progress, ChallengeType challenge) {
        update(new LivenessUpdate(state, feedback, tip, challenge, progress, attempt, policy.maxAttempts(), null,
            false, finished, diagnosticScore()));
    }

    private Double diagnosticScore() {
        return policy.diagnostic() ? lastScore : null;
    }

    private void update(LivenessUpdate update) {
        current = update;
        try {
            listener.onUpdate(update);
        } catch (RuntimeException ignored) {
            // A client bug must not stop the session.
        }
    }

    private void emit(LivenessEvent.Type type, FailureReason reason, ChallengeType challenge, DeviceEvent device) {
        long duration = state == SessionState.INIT ? 0 : clock.millis() - attemptStartMs;
        LivenessEvent event = new LivenessEvent(type, workflow, attempt, duration, modelSetVersion, reason, challenge,
            device);
        try {
            listener.onEvent(event);
        } catch (RuntimeException ignored) {
            // A client bug must not stop the session.
        }
    }
}
