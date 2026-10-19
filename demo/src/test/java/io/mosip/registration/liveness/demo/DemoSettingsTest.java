/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.mosip.registration.liveness.api.LivenessPolicy;
import io.mosip.registration.liveness.api.Workflow;

class DemoSettingsTest {

	@Test
	void defaultsAreTheEngineDefaults() {
		LivenessPolicy policy = new DemoSettings().policy();
		LivenessPolicy defaults = LivenessPolicy.defaults();
		assertEquals(defaults.passThreshold(), policy.passThreshold());
		assertEquals(defaults.minChallenges(), policy.minChallenges());
		assertEquals(defaults.maxAttempts(), policy.maxAttempts());
		assertEquals(defaults.activeEnabled(), policy.activeEnabled());
		assertFalse(policy.diagnostic());
	}

	@Test
	void alwaysChallengeRaisesThePassThresholdOnlyWithTheActiveCheck() {
		assertEquals(1.0, new DemoSettings().alwaysChallenge(true).policy().passThreshold());
		assertEquals(LivenessPolicy.defaults().passThreshold(),
				new DemoSettings().alwaysChallenge(true).activeEnabled(false).policy().passThreshold());
	}

	@Test
	void theExceptionProcessIsForResidentsOnly() {
		DemoSettings settings = new DemoSettings().exceptionAfterLastAttempt(true);
		assertTrue(settings.exceptionAllowed(Workflow.RESIDENT));
		assertFalse(settings.exceptionAllowed(Workflow.OPERATOR));
		assertFalse(settings.exceptionAllowed(Workflow.SUPERVISOR));
		assertFalse(new DemoSettings().exceptionAfterLastAttempt(false).exceptionAllowed(Workflow.RESIDENT));
	}

	@Test
	void configurationLinesUseTheKeysOfTheClient() {
		List<String> lines = new DemoSettings().minChallenges(1).maxAttempts(2).configurationLines(Workflow.OPERATOR);
		assertTrue(lines.contains("mosip.registration.face.liveness.active.min_challenges=1"));
		assertTrue(lines.contains("mosip.registration.face.liveness.max_attempts=2"));
		assertTrue(lines.contains("mosip.registration.face.liveness.on_max_attempts=BLOCK"));
		assertTrue(lines.contains("mosip.registration.face.liveness.passive.threshold.pass=0.85"));
	}
}
