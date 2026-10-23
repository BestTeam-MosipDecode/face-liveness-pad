/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.mock.sbi.devicehelper.face;

import java.util.OptionalDouble;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mosip.mock.sbi.util.ApplicationPropertyHelper;
import io.mosip.registration.liveness.api.FrameScorer;
import io.mosip.registration.liveness.api.LivenessEngine;
import io.mosip.registration.liveness.api.LivenessPolicy;
import io.mosip.registration.liveness.platform.desktop.ImageIoFrameDecoder;

/**
 * Passive liveness score of the face frames, computed in the device with the models of the liveness engine and sent
 * with each frame of the stream (header {@code X-Liveness-Score}). MOSIP asked on 23 October 2026 that the passive score
 * come from the device, the Registration Client performing the active challenges when it does not reach the threshold.
 * Off with {@code mosip.mock.sbi.face.liveness.score=N}, or when the engine cannot start: the frames then go without a
 * score, and the client computes it.
 */
public final class FacePassiveScorer {

	private static final Logger LOGGER = LoggerFactory.getLogger(FacePassiveScorer.class);

	public static final String KEY_SCORE = "mosip.mock.sbi.face.liveness.score";

	private static FacePassiveScorer instance;
	private static boolean initialised;

	private final FrameScorer scorer;

	private FacePassiveScorer(FrameScorer scorer) {
		this.scorer = scorer;
	}

	/** The scorer of the device, or null when the score is off or the engine cannot start. Loads the models once. */
	public static synchronized FacePassiveScorer get() {
		if (!initialised) {
			initialised = true;
			if (isOff(ApplicationPropertyHelper.getPropertyKeyValue(KEY_SCORE))) {
				LOGGER.info("Face liveness score of the device: off");
			} else {
				try {
					// The engine stays loaded for the life of the device, as its models are checked and loaded once.
					LivenessEngine engine = LivenessEngine.create(new ImageIoFrameDecoder());
					instance = new FacePassiveScorer(engine.newFrameScorer(LivenessPolicy.defaults()));
					LOGGER.info("Face liveness score of the device: on, models {}", engine.modelSetVersion());
				} catch (Exception | LinkageError e) {
					LOGGER.error("Face liveness score of the device: off, the liveness engine could not start", e);
				}
			}
		}
		return instance;
	}

	static boolean isOff(String setting) {
		return setting != null && setting.trim().equalsIgnoreCase("N");
	}

	/** Score of one frame, or null when it holds no single face that passes the quality checks. */
	public Double score(byte[] frame) {
		OptionalDouble score = scorer.score(frame);
		return score.isPresent() ? score.getAsDouble() : null;
	}
}
