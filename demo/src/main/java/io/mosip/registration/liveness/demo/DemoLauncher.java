/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.demo;

import javafx.application.Application;

/**
 * Entry point. JavaFX is on the class path, not the module path, and the Java launcher refuses a main class that
 * extends {@link Application} in that case, so the application is started from here.
 */
public final class DemoLauncher {

	private DemoLauncher() {
	}

	public static void main(String[] args) {
		Application.launch(DemoApp.class, args);
	}
}
