/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.session;

import static io.mosip.registration.liveness.session.SessionFixture.IMAGE;
import static io.mosip.registration.liveness.session.SessionFixture.LIVE;
import static io.mosip.registration.liveness.session.SessionFixture.scored;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.mosip.registration.liveness.api.DeviceEvent;
import io.mosip.registration.liveness.api.FailureReason;
import io.mosip.registration.liveness.api.FeedbackCode;
import io.mosip.registration.liveness.api.FrameDecoder;
import io.mosip.registration.liveness.api.LivenessEventListener;
import io.mosip.registration.liveness.api.LivenessPolicy;
import io.mosip.registration.liveness.api.LivenessSession;
import io.mosip.registration.liveness.api.LivenessUpdate;
import io.mosip.registration.liveness.api.SessionState;
import io.mosip.registration.liveness.api.Workflow;

class DefaultLivenessSessionTest {

    /** Empty bytes do not decode; anything else gives the fixture image. */
    static final FrameDecoder DECODER = bytes -> bytes == null || bytes.length == 0 ? Optional.empty()
        : Optional.of(IMAGE);
    static final byte[] FRAME = {1};

    private final SessionFixture f = new SessionFixture();
    private final AtomicInteger closes = new AtomicInteger();

    private DefaultLivenessSession session(SessionCore core) {
        return new DefaultLivenessSession(Workflow.RESIDENT, core, new FrameWorker(Runnable::run, 100), DECODER,
            "1.1.0", closes::incrementAndGet);
    }

    @Test
    void framesAreDecodedThenAnalysed() {
        DefaultLivenessSession session = session(f.core(LivenessPolicy.defaults()));
        f.analyzer.then(SessionFixture.noFace());
        session.submit(FRAME, 100);
        assertEquals(FeedbackCode.NO_FACE, session.current().feedback());
        assertEquals(Workflow.RESIDENT, session.workflow());
    }

    @Test
    void anUndecodableFrameIsCountedOnly() {
        SessionCore core = f.core(LivenessPolicy.defaults());
        session(core).submit(new byte[0], 100);
        assertEquals(1, core.invalidFrames());
        assertEquals(0, f.analyzer.calls());
    }

    @Test
    void captureAndRetryGoThroughTheWorker() {
        DefaultLivenessSession session = session(f.core(LivenessPolicy.defaults()));
        for (int i = 1; i <= 5; i++) {
            f.analyzer.then(scored(LIVE));
            session.submit(FRAME, 100 * i);
        }
        assertEquals(SessionState.PASSED, session.current().state());
        f.analyzer.then(scored(LIVE));
        assertTrue(session.checkCapture(FRAME).accepted());
        assertFalse(session.retry());
    }

    @Test
    void deviceEventsReachTheSession() {
        DefaultLivenessSession session = session(f.core(LivenessPolicy.defaults()));
        session.deviceEvent(DeviceEvent.UNAVAILABLE, 10);
        assertEquals(FeedbackCode.DEVICE_ERROR, session.current().feedback());
    }

    @Test
    void listenerMayCallTheSessionBack() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        AtomicReference<LivenessSession> ref = new AtomicReference<>();
        AtomicBoolean retried = new AtomicBoolean();
        CountDownLatch done = new CountDownLatch(1);
        LivenessEventListener listener = new LivenessEventListener() {
            @Override
            public void onUpdate(LivenessUpdate update) {
                if (update.state() == SessionState.FAILED) {
                    retried.set(ref.get().retry());
                    done.countDown();
                }
            }
        };
        SessionCore core = new SessionCore(Workflow.SUPERVISOR, LivenessPolicy.defaults(), f.analyzer, f.clock,
            new Random(3), "1.1.0", listener, f.metrics);
        DefaultLivenessSession session = new DefaultLivenessSession(Workflow.SUPERVISOR, core,
            new FrameWorker(executor, 100), DECODER, "1.1.0", executor::shutdownNow);
        ref.set(session);
        try {
            f.analyzer.then(scored(LIVE));
            session.submit(FRAME, 100);
            session.deviceEvent(DeviceEvent.STREAM_INTERRUPTED, 150);
            assertTrue(done.await(2, TimeUnit.SECONDS), "the listener blocked");
            assertTrue(retried.get());
        } finally {
            session.close();
        }
    }

    @Test
    void closeStopsTheSession() {
        DefaultLivenessSession session = session(f.core(LivenessPolicy.defaults()));
        session.close();
        session.close();
        assertEquals(1, closes.get());
        session.submit(FRAME, 100);
        session.deviceEvent(DeviceEvent.UNAVAILABLE, 100);
        assertEquals(0, f.analyzer.calls());
        assertTrue(f.events.isEmpty());
        assertEquals(FailureReason.ENGINE_ERROR, session.checkCapture(FRAME).failureReason().get());
        assertFalse(session.retry());
    }
}
