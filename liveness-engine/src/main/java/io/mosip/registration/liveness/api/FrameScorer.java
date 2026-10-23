/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.api;

import java.util.OptionalDouble;

/**
 * Scores single frames as the passive check does, for a face device that computes the passive score itself and sends
 * it with its frames (section 4 of docs/decision-flow.md). Obtained from {@link LivenessEngine#newFrameScorer}; it
 * uses the models of that engine, which closes them.
 */
public interface FrameScorer extends AutoCloseable {

    /**
     * Passive score of one frame, from 0 (attack) to 1 (live face), or empty when the frame holds no single face that
     * passes the quality checks and the head angle limit of the policy, or cannot be decoded.
     *
     * @param encodedFrame JPEG or PNG bytes
     */
    OptionalDouble score(byte[] encodedFrame);

    @Override
    void close();
}
