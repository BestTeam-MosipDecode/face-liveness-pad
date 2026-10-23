/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.session;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import io.mosip.registration.liveness.active.ChallengeType;
import io.mosip.registration.liveness.active.FaceBox;
import io.mosip.registration.liveness.active.FaceMeasures;
import io.mosip.registration.liveness.api.LivenessEvent;
import io.mosip.registration.liveness.api.LivenessEventListener;
import io.mosip.registration.liveness.api.LivenessPolicy;
import io.mosip.registration.liveness.api.LivenessUpdate;
import io.mosip.registration.liveness.api.Workflow;
import io.mosip.registration.liveness.image.BgrImage;
import io.mosip.registration.liveness.passive.QualityChecker;
import io.mosip.registration.liveness.passive.TestAnalyses;

/** A session core on scripted analyses, a manual clock and recorded listener calls; frames come 100 ms apart. */
final class SessionFixture {

    static final FaceBox BOX = new FaceBox(220, 140, 200, 200);
    /** No overlap with BOX. */
    static final FaceBox FAR_BOX = new FaceBox(10, 10, 150, 150);
    static final BgrImage IMAGE = new BgrImage(640, 480, new byte[640 * 480 * 3]);
    static final FaceMeasures BASE = new FaceMeasures(0.30, 0.40, 0.50);
    static final FaceMeasures EYES_CLOSED = new FaceMeasures(0.10, 0.40, 0.50);
    static final double LIVE = 0.9;
    static final double AMBIGUOUS = 0.5;
    static final double SPOOF = 0.05;
    /**
     * Pass threshold of these tests: a LIVE score passes on the passive check. The default became 1.00 on 21 October
     * (#7), which sends every session to the active check; the tests of the passive pass and of what follows it keep
     * 0.85, and {@code SessionCorePassiveTest} checks the default.
     */
    static final double PASS = 0.85;
    /** The defaults with the pass threshold of these tests. */
    static final LivenessPolicy PASSIVE = policy().build();
    /** One blink challenge, so that the drawing is known. */
    static final LivenessPolicy BLINK_ONLY = policy()
        .challengeTypes(EnumSet.of(ChallengeType.BLINK)).minChallenges(1).build();

    /** A builder with the pass threshold of these tests. */
    static LivenessPolicy.Builder policy() {
        return LivenessPolicy.builder().passThreshold(PASS);
    }

    final ScriptedAnalyzer analyzer = new ScriptedAnalyzer();
    final ManualClock clock = new ManualClock();
    final MetricsRecorder metrics = new MetricsRecorder();
    final List<LivenessUpdate> updates = new ArrayList<>();
    final List<LivenessEvent> events = new ArrayList<>();
    long t;

    SessionCore core(LivenessPolicy policy) {
        return new SessionCore(Workflow.RESIDENT, policy, analyzer, clock, new Random(7), "1.1.0",
            new LivenessEventListener() {
                @Override
                public void onUpdate(LivenessUpdate update) {
                    updates.add(update);
                }

                @Override
                public void onEvent(LivenessEvent event) {
                    events.add(event);
                }
            }, metrics);
    }

    /** Moves both clocks by 100 ms and feeds one frame whose analysis is {@code step}. */
    LivenessUpdate frame(SessionCore core, ScriptedAnalyzer.Step step) {
        t += 100;
        clock.advance(100);
        analyzer.then(step);
        core.onFrame(IMAGE, t);
        return core.current();
    }

    /** As {@link #frame}, with the passive score the device sent with the frame. */
    LivenessUpdate frame(SessionCore core, ScriptedAnalyzer.Step step, double deviceScore) {
        t += 100;
        clock.advance(100);
        analyzer.then(step);
        core.onFrame(IMAGE, t, deviceScore);
        return core.current();
    }

    LivenessUpdate frames(SessionCore core, int count, ScriptedAnalyzer.Step step, double deviceScore) {
        LivenessUpdate last = core.current();
        for (int i = 0; i < count; i++) {
            last = frame(core, step, deviceScore);
        }
        return last;
    }

    LivenessUpdate frames(SessionCore core, int count, ScriptedAnalyzer.Step step) {
        LivenessUpdate last = core.current();
        for (int i = 0; i < count; i++) {
            last = frame(core, step);
        }
        return last;
    }

    List<LivenessEvent.Type> eventTypes() {
        return events.stream().map(LivenessEvent::type).collect(Collectors.toList());
    }

    static ScriptedAnalyzer.Step scored(double score) {
        return t -> TestAnalyses.scored(t, BOX, score);
    }

    static ScriptedAnalyzer.Step scoredAt(FaceBox box, double score) {
        return t -> TestAnalyses.scored(t, box, score);
    }

    static ScriptedAnalyzer.Step noFace() {
        return TestAnalyses::noFace;
    }

    static ScriptedAnalyzer.Step twoFaces() {
        return t -> TestAnalyses.faces(t, 2);
    }

    static ScriptedAnalyzer.Step lowQuality(QualityChecker.Tip tip) {
        return t -> TestAnalyses.lowQuality(t, BOX, tip);
    }

    static ScriptedAnalyzer.Step active(FaceMeasures measures, double score) {
        return t -> TestAnalyses.active(t, BOX, measures, score);
    }

    static ScriptedAnalyzer.Step activeAt(FaceBox box, FaceMeasures measures, double score) {
        return t -> TestAnalyses.active(t, box, measures, score);
    }
}
