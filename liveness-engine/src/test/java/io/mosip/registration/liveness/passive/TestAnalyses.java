/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.passive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.mosip.registration.liveness.active.FaceBox;
import io.mosip.registration.liveness.active.FaceMeasures;
import io.mosip.registration.liveness.face.FaceDetection;
import io.mosip.registration.liveness.passive.FrameAnalyzer.FrameAnalysis;

/** Builds frame analyses for the session tests, without models. Lives in this package for its constructors. */
public final class TestAnalyses {

    private static final double[][] KEYPOINTS = {{280, 200}, {360, 200}, {320, 240}, {290, 290}, {350, 290}};
    private static final QualityResult GOOD = new QualityResult(200, true, 120, 100, 0, null);

    private TestAnalyses() {
    }

    public static FaceDetection face(FaceBox box) {
        return new FaceDetection(box, KEYPOINTS, 0.95);
    }

    public static FrameAnalysis noFace(long t) {
        return new FrameAnalysis(t, Collections.emptyList(), null, null, null);
    }

    public static FrameAnalysis faces(long t, int count) {
        List<FaceDetection> faces = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            faces.add(face(new FaceBox(10 + 210 * i, 100, 200, 200)));
        }
        return new FrameAnalysis(t, faces, null, null, null);
    }

    public static FrameAnalysis lowQuality(long t, FaceBox box, QualityChecker.Tip tip) {
        return new FrameAnalysis(t, Collections.singletonList(face(box)),
            new QualityResult(200, true, 120, 100, 0, tip), null, null);
    }

    public static FrameAnalysis scored(long t, FaceBox box, double score) {
        return new FrameAnalysis(t, Collections.singletonList(face(box)), GOOD, score, null);
    }

    public static FrameAnalysis active(long t, FaceBox box, FaceMeasures measures, Double score) {
        return new FrameAnalysis(t, Collections.singletonList(face(box)), GOOD, score, measures);
    }
}
