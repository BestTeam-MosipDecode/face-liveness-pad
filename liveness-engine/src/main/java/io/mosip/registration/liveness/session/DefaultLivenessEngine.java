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
import java.util.concurrent.TimeUnit;

import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import io.mosip.registration.liveness.ModelResources;
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

    /** Longest wait for the worker to leave an inference before the models are released. */
    static final long CLOSE_WAIT_MS = 2000;

    private final FrameDecoder decoder;
    private final Clock clock;
    private final String modelSetVersion;
    private final YuNetDetector detector;
    private final MiniFasClassifier classifier;
    private final FaceMesh mesh;
    private final MetricsRecorder metrics = new MetricsRecorder();
    private DefaultLivenessSession active;
    private ExecutorService activeExecutor;
    private volatile Thread activeWorker;
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
            // The sessions are built from the bytes verified against models.json, not read again.
            detector = new YuNetDetector(environment, manifest.bytes(ModelResources.YUNET));
            classifier = new MiniFasClassifier(environment, manifest.bytes(ModelResources.MINIFASNET_V2),
                manifest.bytes(ModelResources.MINIFASNET_V1SE));
            FaceMesh mesh = new FaceMesh(environment, manifest.bytes(ModelResources.FACE_MESH));
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
            // The previous worker may still be inside an inference: let it finish before the engine can close.
            // Not from the worker itself, a listener opening the next session: it would wait for its own end.
            if (Thread.currentThread() != activeWorker) {
                stopAndWait(activeExecutor, CLOSE_WAIT_MS);
            }
        }
        FrameAnalyzer analyzer = new FrameAnalyzer(detector, classifier, mesh,
            new QualityChecker(policy.minFacePx(), policy.maxYawDeg()));
        SessionCore core = new SessionCore(workflow, policy, analyzer::analyze, clock, new SecureRandom(),
            modelSetVersion, listener, metrics);
        ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "liveness-worker");
            activeWorker = thread;
            thread.setDaemon(true);
            return thread;
        });
        activeExecutor = executor;
        active = new DefaultLivenessSession(workflow, core, new FrameWorker(executor, policy.samplingMs()), decoder,
            modelSetVersion, executor::shutdown);
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
        // Closing an ONNX session during a native run would crash the JVM: keep the models open if the worker is
        // still inside an inference after the wait. The process is ending anyway when the engine is closed.
        if (Thread.currentThread() == activeWorker || !stopAndWait(activeExecutor, CLOSE_WAIT_MS)) {
            return;
        }
        closeQuietly(mesh);
        closeQuietly(classifier);
        closeQuietly(detector);
    }

    /** Stops a worker and waits for its current task. True when it has stopped, or when there is none. */
    static boolean stopAndWait(ExecutorService executor, long timeoutMs) {
        if (executor == null) {
            return true;
        }
        // shutdown, not shutdownNow: an interrupt could break an audit write done by the listener.
        executor.shutdown();
        try {
            return executor.awaitTermination(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
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
