/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.mosip.registration.liveness.api.LivenessPolicy;
import io.mosip.registration.liveness.api.Workflow;

/**
 * Settings of the demo, given as the configuration keys of the client (docs/configuration.md), and the engine policy
 * they produce. Every value the demo does not change keeps the default of the engine, except the pass threshold, which
 * the demo always sets.
 */
final class DemoSettings {

	static final String PREFIX = "mosip.registration.face.liveness.";
	/** Default since 21 October 2026 (#7): every session goes to the active check. */
	static final double CHALLENGE_EVERY_SESSION = 1.0;
	/** Former default: a clear live face passes on the passive score alone. */
	static final double PASSIVE_PASS = 0.85;

	private boolean activeEnabled = true;
	private boolean passivePass;
	private int minChallenges = 2;
	private int maxAttempts = 3;
	private boolean exceptionAfterLastAttempt = true;
	private boolean diagnostic;

	DemoSettings activeEnabled(boolean value) {
		activeEnabled = value;
		return this;
	}

	/** Allows a passive pass for clear scores; without the active check, it is the only way to pass. */
	DemoSettings passivePass(boolean value) {
		passivePass = value;
		return this;
	}

	DemoSettings minChallenges(int value) {
		minChallenges = value;
		return this;
	}

	DemoSettings maxAttempts(int value) {
		maxAttempts = value;
		return this;
	}

	DemoSettings exceptionAfterLastAttempt(boolean value) {
		exceptionAfterLastAttempt = value;
		return this;
	}

	DemoSettings diagnostic(boolean value) {
		diagnostic = value;
		return this;
	}

	LivenessPolicy policy() {
		boolean challengeEverySession = activeEnabled && !passivePass;
		return LivenessPolicy.builder().activeEnabled(activeEnabled).minChallenges(minChallenges)
				.maxAttempts(maxAttempts).diagnostic(diagnostic)
				.passThreshold(challengeEverySession ? CHALLENGE_EVERY_SESSION : PASSIVE_PASS).build();
	}

	/** The exception process follows the last attempt of a resident only, as in the client. */
	boolean exceptionAllowed(Workflow workflow) {
		return workflow == Workflow.RESIDENT && exceptionAfterLastAttempt;
	}

	/** The keys an administrator would set on the server to get the same behaviour. */
	List<String> configurationLines(Workflow workflow) {
		LivenessPolicy policy = policy();
		List<String> lines = new ArrayList<>();
		lines.add(PREFIX + "enabled=true");
		lines.add(PREFIX + "passive.threshold.pass=" + String.format(Locale.ROOT, "%.2f", policy.passThreshold()));
		lines.add(PREFIX + "passive.threshold.attack=" + String.format(Locale.ROOT, "%.2f", policy.attackThreshold()));
		lines.add(PREFIX + "active.enabled=" + activeEnabled);
		lines.add(PREFIX + "active.min_challenges=" + minChallenges);
		lines.add(PREFIX + "max_attempts=" + maxAttempts);
		lines.add(PREFIX + "on_max_attempts=" + (exceptionAllowed(workflow) ? "EXCEPTION" : "BLOCK"));
		lines.add(PREFIX + "diagnostic.enabled=" + diagnostic);
		return lines;
	}
}
