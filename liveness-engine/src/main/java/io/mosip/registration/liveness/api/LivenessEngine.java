/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.api;

import java.time.Clock;

/**
 * Entry point of the engine. Create it once: it checks the model files against models.json and loads them. It runs
 * one session at a time.
 */
public interface LivenessEngine extends AutoCloseable {

    /**
     * Checks and loads the models, with the system clock.
     *
     * @param decoder the platform decoder, {@code new ImageIoFrameDecoder()} on the desktop
     * @throws ModelIntegrityException when a model file is missing or differs from models.json
     */
    static LivenessEngine create(FrameDecoder decoder) throws ModelIntegrityException {
        return create(decoder, Clock.systemUTC());
    }

    /** Same as {@link #create(FrameDecoder)} with the clock used for durations and metrics. */
    static LivenessEngine create(FrameDecoder decoder, Clock clock) throws ModelIntegrityException {
        throw new UnsupportedOperationException("implemented with the session, issue #14");
    }

    /**
     * Opens a session and closes the previous one, if any.
     *
     * @throws IllegalStateException when the engine is closed
     */
    LivenessSession newSession(Workflow workflow, LivenessPolicy policy, LivenessEventListener listener);

    /** Version of the model set, from models.json, for results and audit. */
    String modelSetVersion();

    /** Anonymous counters since the engine started. */
    LivenessMetrics metrics();

    /** Closes the active session and releases the models. */
    @Override
    void close();
}
