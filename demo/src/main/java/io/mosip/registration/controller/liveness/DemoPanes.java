/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.controller.liveness;

import java.util.function.Function;

import io.mosip.registration.liveness.api.Workflow;

/**
 * Creates the status pane of the client with given text sources. The public constructor of the pane reads the
 * application language and the bundles of a running client, which the demo does not have; this one takes the message
 * and label files directly.
 */
public final class DemoPanes {

	private DemoPanes() {
	}

	public static LivenessStatusPane create(Workflow workflow, Function<String, String> messages,
			Function<String, String> labels) {
		return new LivenessStatusPane(workflow, messages, labels);
	}
}
