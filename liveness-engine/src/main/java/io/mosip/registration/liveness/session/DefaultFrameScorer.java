/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.session;

import java.time.Clock;
import java.util.Optional;
import java.util.OptionalDouble;

import io.mosip.registration.liveness.api.FrameDecoder;
import io.mosip.registration.liveness.api.FrameScorer;
import io.mosip.registration.liveness.image.BgrImage;
import io.mosip.registration.liveness.passive.FrameAnalyzer;
import io.mosip.registration.liveness.passive.FrameAnalyzer.FrameAnalysis;

/** Scores single frames with the analyser of a session, out of any session. One frame at a time. */
final class DefaultFrameScorer implements FrameScorer {

    private final FrameAnalyzer analyzer;
    private final FrameDecoder decoder;
    private final Clock clock;

    DefaultFrameScorer(FrameAnalyzer analyzer, FrameDecoder decoder, Clock clock) {
        this.analyzer = analyzer;
        this.decoder = decoder;
        this.clock = clock;
    }

    @Override
    public synchronized OptionalDouble score(byte[] encodedFrame) {
        try {
            Optional<BgrImage> image = encodedFrame == null ? Optional.empty() : decoder.decode(encodedFrame);
            if (!image.isPresent()) {
                return OptionalDouble.empty();
            }
            FrameAnalysis analysis = analyzer.analyze(image.get(), clock.millis(), false);
            return analysis.score() == null ? OptionalDouble.empty() : OptionalDouble.of(analysis.score());
        } catch (Exception e) {
            return OptionalDouble.empty();
        }
    }

    /** The models belong to the engine, which closes them. */
    @Override
    public void close() {
    }
}
