/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import io.mosip.registration.liveness.active.ChallengeType;

class LivenessPolicyTest {

    @Test
    void defaultsFollowTheDecisionFlow() {
        LivenessPolicy p = LivenessPolicy.defaults();
        assertEquals(1.00, p.passThreshold());
        assertEquals(0.30, p.attackThreshold());
        assertEquals(5, p.minFrames());
        assertEquals(3000, p.windowMs());
        assertEquals(100, p.samplingMs());
        assertEquals(120, p.minFacePx());
        assertEquals(20, p.maxYawDeg());
        assertTrue(p.activeEnabled());
        assertEquals(2, p.minChallenges());
        assertEquals(EnumSet.of(ChallengeType.BLINK, ChallengeType.TURN_LEFT, ChallengeType.TURN_RIGHT), p.challengeTypes(),
            "SMILE is left out of the default, still available in the configuration (#5)");
        assertEquals(8000, p.challengeTimeoutMs());
        assertEquals(600, p.baselineMs());
        assertEquals(3, p.maxAttempts());
        assertEquals(20000, p.passiveTimeoutMs());
        assertFalse(p.diagnostic());
    }

    @Test
    void builderSetsEveryValue() {
        LivenessPolicy p = LivenessPolicy.builder().passThreshold(0.9).attackThreshold(0.2).minFrames(7)
            .windowMs(4000).samplingMs(0).minFacePx(100).maxYawDeg(15).activeEnabled(false).minChallenges(3)
            .challengeTypes(Arrays.asList(ChallengeType.BLINK, ChallengeType.SMILE)).challengeTimeoutMs(9000)
            .baselineMs(700).maxAttempts(5).passiveTimeoutMs(15000).diagnostic(true).build();
        assertEquals(0.9, p.passThreshold());
        assertEquals(0.2, p.attackThreshold());
        assertEquals(7, p.minFrames());
        assertEquals(4000, p.windowMs());
        assertEquals(0, p.samplingMs());
        assertEquals(100, p.minFacePx());
        assertEquals(15, p.maxYawDeg());
        assertFalse(p.activeEnabled());
        assertEquals(3, p.minChallenges());
        assertEquals(EnumSet.of(ChallengeType.BLINK, ChallengeType.SMILE), p.challengeTypes());
        assertEquals(9000, p.challengeTimeoutMs());
        assertEquals(700, p.baselineMs());
        assertEquals(5, p.maxAttempts());
        assertEquals(15000, p.passiveTimeoutMs());
        assertTrue(p.diagnostic());
    }

    @Test
    void refusesInvalidValues() {
        LivenessPolicy.Builder b = LivenessPolicy.builder();
        assertThrows(IllegalArgumentException.class, () -> b.passThreshold(1.1));
        assertThrows(IllegalArgumentException.class, () -> b.attackThreshold(-0.1));
        assertThrows(IllegalArgumentException.class, () -> b.passThreshold(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> b.minFrames(0));
        assertThrows(IllegalArgumentException.class, () -> b.windowMs(0));
        assertThrows(IllegalArgumentException.class, () -> b.samplingMs(-1));
        assertThrows(IllegalArgumentException.class, () -> b.minFacePx(0));
        assertThrows(IllegalArgumentException.class, () -> b.maxYawDeg(0));
        assertThrows(IllegalArgumentException.class, () -> b.maxYawDeg(91));
        assertThrows(IllegalArgumentException.class, () -> b.minChallenges(0));
        assertThrows(IllegalArgumentException.class, () -> b.challengeTypes(Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> b.challengeTypes(null));
        assertThrows(IllegalArgumentException.class,
            () -> b.challengeTypes(Arrays.asList(ChallengeType.BLINK, null)));
        assertThrows(IllegalArgumentException.class, () -> b.challengeTimeoutMs(0));
        assertThrows(IllegalArgumentException.class, () -> b.baselineMs(0));
        assertThrows(IllegalArgumentException.class, () -> b.maxAttempts(0));
        assertThrows(IllegalArgumentException.class, () -> b.passiveTimeoutMs(0));
    }

    @Test
    void refusesInconsistentCombinations() {
        assertThrows(IllegalArgumentException.class,
            () -> LivenessPolicy.builder().passThreshold(0.5).attackThreshold(0.5).build());
        assertThrows(IllegalArgumentException.class,
            () -> LivenessPolicy.builder().challengeTimeoutMs(600).baselineMs(600).build());
    }

    @Test
    void challengeTypesCannotBeChangedFromOutside() {
        LivenessPolicy p = LivenessPolicy.defaults();
        assertThrows(UnsupportedOperationException.class, () -> p.challengeTypes().clear());
    }

    @Test
    void refusesAWindowTooShortForItsFrames() {
        assertThrows(IllegalArgumentException.class,
            () -> LivenessPolicy.builder().minFrames(10).samplingMs(500).windowMs(3000).build());
        LivenessPolicy.builder().minFrames(10).samplingMs(300).windowMs(3000).build();
    }
}
