/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.session;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import io.mosip.registration.liveness.api.CaptureCheck;
import io.mosip.registration.liveness.api.DeviceEvent;
import io.mosip.registration.liveness.api.FailureReason;
import io.mosip.registration.liveness.api.FrameDecoder;
import io.mosip.registration.liveness.api.LivenessSession;
import io.mosip.registration.liveness.api.LivenessUpdate;
import io.mosip.registration.liveness.api.Workflow;
import io.mosip.registration.liveness.image.BgrImage;

/** {@link LivenessSession} over a {@link SessionCore} run by a {@link FrameWorker}. */
final class DefaultLivenessSession implements LivenessSession {

    /** Longest wait of {@link #checkCapture} and {@link #retry}. */
    static final long CALL_TIMEOUT_MS = 5000;

    private final Workflow workflow;
    private final SessionCore core;
    private final FrameWorker worker;
    private final FrameDecoder decoder;
    private final String modelSetVersion;
    private final Runnable onClose;
    private final AtomicBoolean closed = new AtomicBoolean();

    DefaultLivenessSession(Workflow workflow, SessionCore core, FrameWorker worker, FrameDecoder decoder,
            String modelSetVersion, Runnable onClose) {
        this.workflow = workflow;
        this.core = core;
        this.worker = worker;
        this.decoder = decoder;
        this.modelSetVersion = modelSetVersion;
        this.onClose = onClose;
    }

    @Override
    public void submit(byte[] encodedFrame, long timestampMs) {
        worker.offerFrame(timestampMs, () -> core.onFrame(decode(encodedFrame), timestampMs));
    }

    @Override
    public void submit(byte[] encodedFrame, long timestampMs, double deviceScore) {
        worker.offerFrame(timestampMs, () -> core.onFrame(decode(encodedFrame), timestampMs, deviceScore));
    }

    @Override
    public void deviceEvent(DeviceEvent event, long timestampMs) {
        Objects.requireNonNull(event, "event");
        worker.run(() -> core.onDeviceEvent(event, timestampMs));
    }

    @Override
    public CaptureCheck checkCapture(byte[] encodedImage) {
        return worker.call(() -> core.checkCapture(decode(encodedImage)), CALL_TIMEOUT_MS,
            CaptureCheck.rejected(FailureReason.ENGINE_ERROR, modelSetVersion));
    }

    @Override
    public CaptureCheck checkCapture(BgrImage capturedImage) {
        return worker.call(() -> core.checkCapture(capturedImage), CALL_TIMEOUT_MS,
            CaptureCheck.rejected(FailureReason.ENGINE_ERROR, modelSetVersion));
    }

    @Override
    public boolean retry() {
        return worker.call(core::retry, CALL_TIMEOUT_MS, Boolean.FALSE);
    }

    @Override
    public LivenessUpdate current() {
        return core.current();
    }

    @Override
    public Workflow workflow() {
        return workflow;
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            worker.close();
            core.close();
            onClose.run();
        }
    }

    private BgrImage decode(byte[] bytes) {
        try {
            return decoder.decode(bytes).orElse(null);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
