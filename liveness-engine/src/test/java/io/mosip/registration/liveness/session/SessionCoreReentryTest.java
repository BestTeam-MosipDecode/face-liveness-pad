/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.session;

import static io.mosip.registration.liveness.session.SessionFixture.IMAGE;
import static io.mosip.registration.liveness.session.SessionFixture.LIVE;
import static io.mosip.registration.liveness.session.SessionFixture.SPOOF;
import static io.mosip.registration.liveness.session.SessionFixture.scored;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.mosip.registration.liveness.api.DeviceEvent;
import io.mosip.registration.liveness.api.FailureReason;
import io.mosip.registration.liveness.api.LivenessEvent;
import io.mosip.registration.liveness.api.LivenessEventListener;
import io.mosip.registration.liveness.api.LivenessPolicy;
import io.mosip.registration.liveness.api.SessionState;
import io.mosip.registration.liveness.api.Workflow;

/** Listeners that call the session back, and streams whose timestamps go back. */
class SessionCoreReentryTest {

    private final SessionFixture f = new SessionFixture();

    /** A core whose listener calls retry() when it receives the FAILED event; the result goes to {@code result}. */
    private SessionCore retryingOnFailedEvent(LivenessPolicy policy, AtomicReference<Boolean> result) {
        AtomicReference<SessionCore> self = new AtomicReference<>();
        SessionCore core = new SessionCore(Workflow.RESIDENT, policy, f.analyzer, f.clock, new Random(5), "1.1.0",
            new LivenessEventListener() {
                @Override
                public void onEvent(LivenessEvent event) {
                    if (event.type() == LivenessEvent.Type.FAILED) {
                        result.set(self.get().retry());
                    }
                }
            }, f.metrics);
        self.set(core);
        return core;
    }

    @Test
    void aRetryFromTheFailedEventOfTheLastAttemptIsRefused() {
        AtomicReference<Boolean> result = new AtomicReference<>();
        SessionCore core = retryingOnFailedEvent(LivenessPolicy.builder().maxAttempts(1).build(), result);
        f.frames(core, 5, scored(SPOOF));
        assertFalse(result.get());
        assertEquals(SessionState.FAILED, core.current().state());
        assertTrue(core.current().finished());
    }

    @Test
    void aRetryFromTheFailedEventLeavesAConsistentSession() {
        AtomicReference<Boolean> result = new AtomicReference<>();
        SessionCore core = retryingOnFailedEvent(LivenessPolicy.defaults(), result);
        f.frames(core, 5, scored(SPOOF));
        assertTrue(result.get());
        assertEquals(SessionState.INIT, core.current().state());
        assertEquals(2, f.frame(core, scored(LIVE)).attempt());
    }

    @Test
    void aTimestampThatGoesBackEndsTheAttemptAsAStreamInterruption() {
        SessionCore core = f.core(LivenessPolicy.defaults());
        f.frames(core, 2, scored(LIVE));
        int calls = f.analyzer.calls();
        core.onFrame(IMAGE, 50);
        assertEquals(calls, f.analyzer.calls(), "the frame is not analysed");
        assertEquals(SessionState.FAILED, core.current().state());
        assertEquals(FailureReason.DEVICE_ERROR, core.current().failureReason().get());
        LivenessEvent device = f.events.stream().filter(e -> e.type() == LivenessEvent.Type.DEVICE_ERROR)
            .findFirst().get();
        assertEquals(DeviceEvent.STREAM_INTERRUPTED, device.deviceEvent().get());
    }

    @Test
    void anAttemptAfterARestartAcceptsTheNewTimestamps() {
        SessionCore core = f.core(LivenessPolicy.defaults());
        f.frames(core, 2, scored(LIVE));
        core.onFrame(IMAGE, 50);
        assertTrue(core.retry());
        f.t = 0;
        assertEquals(SessionState.PASSED, f.frames(core, 5, scored(LIVE)).state());
    }

    @Test
    void aClosedSessionNeitherAnalysesNorPublishes() {
        SessionCore core = f.core(LivenessPolicy.defaults());
        f.frame(core, scored(LIVE));
        int updates = f.updates.size();
        core.close();
        f.frame(core, scored(LIVE));
        core.onDeviceEvent(DeviceEvent.UNAVAILABLE, 1000);
        assertEquals(1, f.analyzer.calls());
        assertEquals(updates, f.updates.size());
    }
}
