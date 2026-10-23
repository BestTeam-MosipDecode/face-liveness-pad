/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.passive;

import java.util.List;

import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import io.mosip.registration.liveness.active.FaceMeasures;
import io.mosip.registration.liveness.active.FrameObservation;
import io.mosip.registration.liveness.face.FaceDetection;
import io.mosip.registration.liveness.face.FaceMesh;
import io.mosip.registration.liveness.face.YuNetDetector;
import io.mosip.registration.liveness.image.BgrImage;

/**
 * Processing of one frame (section 3 of {@code docs/decision-flow.md}): face detection, quality checks and, for a
 * single face that passes them, the frame score (section 3.6). In the active check, the Face Mesh measures are also
 * computed and the yaw limit does not block the measures, only the score.
 *
 * <p>Not thread-safe: the session uses one analyser from its single analysis thread.
 */
public final class FrameAnalyzer implements AutoCloseable {

    private final YuNetDetector detector;
    private final MiniFasClassifier classifier;
    private final FaceMesh mesh;
    private final QualityChecker quality;
    private final boolean ownsModels;

    public FrameAnalyzer(OrtEnvironment environment, QualityChecker quality) throws OrtException {
        this.detector = new YuNetDetector(environment);
        this.classifier = new MiniFasClassifier(environment);
        this.mesh = new FaceMesh(environment);
        this.quality = quality;
        this.ownsModels = true;
    }

    /**
     * Analyser on models loaded by the caller, such as the engine that shares them between its sessions. Its
     * {@link #close()} leaves the models open: they belong to the caller.
     */
    public FrameAnalyzer(YuNetDetector detector, MiniFasClassifier classifier, FaceMesh mesh, QualityChecker quality) {
        this.detector = detector;
        this.classifier = classifier;
        this.mesh = mesh;
        this.quality = quality;
        this.ownsModels = false;
    }

    /**
     * Analyses one decoded frame. {@code active} is true during the active check: the measures are computed and the
     * yaw limit only withholds the score.
     */
    public FrameAnalysis analyze(BgrImage image, long timestampMs, boolean active) throws OrtException {
        List<FaceDetection> faces = detector.detect(image);
        if (faces.size() != 1) {
            return new FrameAnalysis(timestampMs, faces, null, null, null);
        }
        FaceDetection face = faces.get(0);
        QualityResult result = quality.check(image, face, !active);
        Double score = null;
        if (result.passed() && Math.abs(result.yawDeg()) <= quality.maxYawDeg()) {
            score = classifier.score(image, face.classifierBox());
        }
        FaceMeasures measures = active ? mesh.landmarks(image, face.box()).measures() : null;
        return new FrameAnalysis(timestampMs, faces, result, score, measures);
    }

    /** Result of {@link #analyze}. */
    public static final class FrameAnalysis {
        private final long timestampMs;
        private final List<FaceDetection> faces;
        private final QualityResult quality;
        private final Double score;
        private final FaceMeasures measures;

        FrameAnalysis(long timestampMs, List<FaceDetection> faces, QualityResult quality, Double score,
                FaceMeasures measures) {
            this.timestampMs = timestampMs;
            this.faces = faces;
            this.quality = quality;
            this.score = score;
            this.measures = measures;
        }

        public long timestampMs() {
            return timestampMs;
        }

        public List<FaceDetection> faces() {
            return faces;
        }

        /** Quality of the single face, or null when the frame holds no face or several faces. */
        public QualityResult quality() {
            return quality;
        }

        /** Frame score, or null when the frame was not scored. */
        public Double score() {
            return score;
        }

        /** The same analysis with another frame score, for a score computed by the face device. */
        public FrameAnalysis withScore(double frameScore) {
            return new FrameAnalysis(timestampMs, faces, quality, frameScore, measures);
        }

        /** Face Mesh measures, computed in the active check only. */
        public FaceMeasures measures() {
            return measures;
        }

        /** The observation fed to the {@code ChallengeRunner} of the active check. */
        public FrameObservation toObservation() {
            if (faces.isEmpty()) {
                return FrameObservation.noFace(timestampMs);
            }
            if (faces.size() > 1) {
                return FrameObservation.multipleFaces(timestampMs, faces.size());
            }
            return FrameObservation.oneFace(timestampMs, faces.get(0).box(), measures, score);
        }
    }

    @Override
    public void close() throws OrtException {
        if (!ownsModels) {
            return;
        }
        detector.close();
        classifier.close();
        mesh.close();
    }
}
