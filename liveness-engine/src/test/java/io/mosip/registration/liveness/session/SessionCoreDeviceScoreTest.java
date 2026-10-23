/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.session;

import static io.mosip.registration.liveness.session.SessionFixture.LIVE;
import static io.mosip.registration.liveness.session.SessionFixture.SPOOF;
import static io.mosip.registration.liveness.session.SessionFixture.lowQuality;
import static io.mosip.registration.liveness.session.SessionFixture.scored;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.mosip.registration.liveness.api.FailureReason;
import io.mosip.registration.liveness.api.FeedbackCode;
import io.mosip.registration.liveness.api.LivenessUpdate;
import io.mosip.registration.liveness.api.SessionState;
import io.mosip.registration.liveness.passive.QualityChecker;

/** The passive score computed by the face device and sent with its frames (section 4 of docs/decision-flow.md). */
class SessionCoreDeviceScoreTest {

    private final SessionFixture f = new SessionFixture();

    @Test
    void theScoreOfTheDeviceDecidesInsteadOfTheScoreOfTheClient() {
        SessionCore attack = f.core(SessionFixture.PASSIVE);
        LivenessUpdate rejected = f.frames(attack, 5, scored(LIVE), SPOOF);
        assertEquals(SessionState.FAILED, rejected.state());
        assertEquals(FailureReason.PAD_ATTACK_DETECTED, rejected.failureReason().get());

        SessionFixture g = new SessionFixture();
        SessionCore live = g.core(SessionFixture.PASSIVE);
        assertEquals(SessionState.PASSED, g.frames(live, 5, scored(SPOOF), LIVE).state());
    }

    @Test
    void aFrameTheClientCannotUseStaysUnscoredWhateverTheDeviceSends() {
        SessionCore core = f.core(SessionFixture.PASSIVE);
        LivenessUpdate last = f.frames(core, 8, lowQuality(QualityChecker.Tip.LIGHTING), LIVE);
        assertEquals(SessionState.PASSIVE_CHECK, last.state());
        assertEquals(FeedbackCode.LOW_QUALITY, last.feedback());
    }

    @Test
    void aDeviceScoreOutsideZeroToOneIsIgnored() {
        SessionCore core = f.core(SessionFixture.PASSIVE);
        assertEquals(SessionState.PASSED, f.frames(core, 5, scored(LIVE), 7.0).state());

        SessionFixture g = new SessionFixture();
        SessionCore notANumber = g.core(SessionFixture.PASSIVE);
        assertEquals(SessionState.PASSED, g.frames(notANumber, 5, scored(LIVE), Double.NaN).state());
    }
}
