/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.mock.sbi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import io.mosip.mock.sbi.devicehelper.face.FacePassiveScorerTestAccess;

/** The header of each frame of the stream, with the passive liveness score of the device when there is one. */
class StreamFrameHeaderTest {

	@Test
	void aScoredFrameCarriesItsScoreBeforeItsLength() {
		String header = new String(SBIServiceResponse.createHeader(1234, 0.97312), StandardCharsets.US_ASCII);
		assertTrue(header.contains("X-Liveness-Score:0.9731\r\nContent-Length:1234\r\n\r\n"), header);
	}

	@Test
	void theScoreIsWrittenWithADotWhateverTheLocale() {
		Locale before = Locale.getDefault();
		try {
			Locale.setDefault(Locale.FRANCE);
			String header = new String(SBIServiceResponse.createHeader(10, 0.5), StandardCharsets.US_ASCII);
			assertTrue(header.contains("X-Liveness-Score:0.5000\r\n"), header);
		} finally {
			Locale.setDefault(before);
		}
	}

	@Test
	void aFrameWithoutScoreKeepsTheHeaderOfMockMds() {
		String header = new String(SBIServiceResponse.createHeader(10, null), StandardCharsets.US_ASCII);
		assertFalse(header.contains("X-Liveness-Score"));
		assertEquals("--BoundaryString\r\nAccess-Control-Allow-Origin:*\r\nContent-Type:image/jpeg\r\nContent-Length:10\r\n\r\n",
			header);
	}

	@Test
	void theScoreIsOffOnlyWhenTheSettingSaysN() {
		assertTrue(FacePassiveScorerTestAccess.isOff("N"));
		assertTrue(FacePassiveScorerTestAccess.isOff(" n "));
		assertFalse(FacePassiveScorerTestAccess.isOff("Y"));
		assertFalse(FacePassiveScorerTestAccess.isOff(null));
	}
}
