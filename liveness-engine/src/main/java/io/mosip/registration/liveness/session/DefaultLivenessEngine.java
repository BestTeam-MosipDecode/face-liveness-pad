/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.session;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import io.mosip.registration.liveness.api.FrameDecoder;
import io.mosip.registration.liveness.api.LivenessEngine;
import io.mosip.registration.liveness.api.LivenessEventListener;
import io.mosip.registration.liveness.api.LivenessMetrics;
import io.mosip.registration.liveness.api.LivenessPolicy;
import io.mosip.registration.liveness.api.LivenessSession;
import io.mosip.registration.liveness.api.ModelIntegrityException;
import io.mosip.registration.liveness.api.Workflow;
import io.mosip.registration.liveness.face.FaceMesh;
import io.mosip.registration.liveness.face.YuNetDetector;
import io.mosip.registration.liveness.model.ModelManifest;
import io.mosip.registration.liveness.passive.FrameAnalyzer;
import io.mosip.registration.liveness.passive.MiniFasClassifier;
import io.mosip.registration.liveness.passive.QualityChecker;

/**
 * The engine: checks the models against models.json, loads them once and runs one session at a time on them.
 * Clients use {@link LivenessEngine#create}.
 */
public final class DefaultLivenessEngine implements LivenessEngine {

    private final FrameDecoder decoder;
    private final Clock clock;
    private final String modelSetVersion;
    private final YuNetDetector detector;
    private final MiniFasClassifier classifier;
    private final FaceMesh mesh;
    private final MetricsRecorder metrics = new MetricsRecorder();
    private DefaultLivenessSession active;
    private boolean closed;

    private DefaultLivenessEngine(FrameDecoder decoder, Clock clock, String modelSetVersion, YuNetDetector detector,
            MiniFasClassifier classifier, FaceMesh mesh) {
        this.decoder = decoder;
        this.clock = clock;
        this.modelSetVersion = modelSetVersion;
        this.detector = detector;
        this.classifier = classifier;
        this.mesh = mesh;
    }

    /** See {@link LivenessEngine#create(FrameDecoder, Clock)}. */
    public static LivenessEngine create(FrameDecoder decoder, Clock clock) throws ModelIntegrityException {
        Objects.requireNonNull(decoder, "decoder");
        Objects.requireNonNull(clock, "clock");
        ModelManifest manifest = ModelManifest.load();
        OrtEnvironment environment = OrtEnvironment.getEnvironment();
        YuNetDetector detector = null;
        MiniFasClassifier classifier = null;
        try {
            detector = new YuNetDetector(environment);
            classifier = new MiniFasClassifier(environment);
            FaceMesh mesh = new FaceMesh(environment);
            return new DefaultLivenessEngine(decoder, clock, manifest.modelSetVersion(), detector, classifier, mesh);
        } catch (OrtException e) {
            closeQuietly(classifier);
            closeQuietly(detector);
            throw new IllegalStateException("the liveness models could not be loaded", e);
        }
    }

    @Override
    public synchronized LivenessSession newSession(Workflow workflow, LivenessPolicy policy,
            LivenessEventListener listener) {
        if (closed) {
            throw new IllegalStateException("the liveness engine is closed");
        }
        Objects.requireNonNull(workflow, "workflow");
        Objects.requireNonNull(policy, "policy");
        Objects.requireNonNull(listener, "listener");
        if (active != null) {
            active.close();
        }
        FrameAnalyzer analyzer = new FrameAnalyzer(detector, classifier, mesh,
            new QualityChecker(policy.minFacePx(), policy.maxYawDeg()));
        SessionCore core = new SessionCore(workflow, policy, analyzer::analyze, clock, new SecureRandom(),
            modelSetVersion, listener, metrics);
        ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "liveness-worker");
            thread.setDaemon(true);
            return thread;
        });
        active = new DefaultLivenessSession(workflow, core, new FrameWorker(executor, policy.samplingMs()), decoder,
            modelSetVersion, executor::shutdownNow);
        metrics.sessionStarted();
        return active;
    }

    @Override
    public String modelSetVersion() {
        return modelSetVersion;
    }

    @Override
    public LivenessMetrics metrics() {
        return metrics.snapshot();
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (active != null) {
            active.close();
        }
        closeQuietly(mesh);
        closeQuietly(classifier);
        closeQuietly(detector);
    }

    private static void closeQuietly(AutoCloseable resource) {
        if (resource == null) {
            return;
        }
        try {
            resource.close();
        } catch (Exception ignored) {
            // Nothing more can be done while releasing native resources.
        }
    }
}
