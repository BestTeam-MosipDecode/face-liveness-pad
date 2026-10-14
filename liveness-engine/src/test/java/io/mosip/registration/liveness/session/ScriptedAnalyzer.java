/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.session;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import io.mosip.registration.liveness.image.BgrImage;
import io.mosip.registration.liveness.passive.FrameAnalyzer.FrameAnalysis;

/** Analyser that returns scripted results, one per call. */
final class ScriptedAnalyzer implements Analyzer {

    /** Result of one analysis, built with the frame timestamp. */
    interface Step {
        FrameAnalysis at(long timestampMs) throws Exception;
    }

    private final Deque<Step> steps = new ArrayDeque<>();
    final List<Boolean> activeFlags = new ArrayList<>();

    void then(Step step) {
        steps.add(step);
    }

    int calls() {
        return activeFlags.size();
    }

    @Override
    public FrameAnalysis analyze(BgrImage image, long timestampMs, boolean active) throws Exception {
        activeFlags.add(active);
        Step step = steps.poll();
        if (step == null) {
            throw new AssertionError("no scripted analysis at t=" + timestampMs);
        }
        return step.at(timestampMs);
    }
}
