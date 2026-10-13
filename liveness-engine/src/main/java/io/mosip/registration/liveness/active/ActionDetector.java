/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.active;

/** Judges one action against the baseline of its challenge, frame after frame. */
public interface ActionDetector {

    /** Result of one analysed frame. */
    enum Result {
        PENDING,
        DETECTED,
        FAILED
    }

    Result update(FaceMeasures measures);

    /** Detector of {@code type}, with the factors of section 5.3 of {@code docs/decision-flow.md}. */
    static ActionDetector create(ChallengeType type, FaceMeasures baseline) {
        switch (type) {
            case BLINK:
                return new BlinkDetector(baseline);
            case SMILE:
                return new SmileDetector(baseline);
            case TURN_LEFT:
                return new HeadTurnDetector(baseline, HeadTurnDetector.LEFT);
            case TURN_RIGHT:
                return new HeadTurnDetector(baseline, HeadTurnDetector.RIGHT);
            default:
                throw new IllegalArgumentException("unknown challenge " + type);
        }
    }
}
