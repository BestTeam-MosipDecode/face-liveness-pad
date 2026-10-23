/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.mock.sbi.devicehelper.face;

/** Gives the tests of other packages the setting rule of {@link FacePassiveScorer}. */
public final class FacePassiveScorerTestAccess {

	private FacePassiveScorerTestAccess() {
	}

	public static boolean isOff(String setting) {
		return FacePassiveScorer.isOff(setting);
	}
}
