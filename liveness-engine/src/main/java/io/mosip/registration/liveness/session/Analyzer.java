/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.session;

import io.mosip.registration.liveness.image.BgrImage;
import io.mosip.registration.liveness.passive.FrameAnalyzer.FrameAnalysis;

/**
 * Analysis of one frame, as done by {@code FrameAnalyzer.analyze}. The session depends on this interface so that its
 * tests replace the models with scripted results.
 */
@FunctionalInterface
interface Analyzer {

    FrameAnalysis analyze(BgrImage image, long timestampMs, boolean active) throws Exception;
}
