/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.active;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

/**
 * Draws the challenges of one attempt among the enabled types (section 5.1 of {@code docs/decision-flow.md}). The
 * same type never appears twice in a row, unless only one type is enabled. Each attempt draws a new sequence, so a
 * replayed video cannot perform the right actions in the right order.
 */
public final class ChallengeGenerator {

    private final List<ChallengeType> types;
    private final int count;
    private final Random random;

    public ChallengeGenerator(Collection<ChallengeType> types, int count) {
        this(types, count, new SecureRandom());
    }

    /** {@code random} is a {@link SecureRandom} in production; tests inject a seeded generator. */
    public ChallengeGenerator(Collection<ChallengeType> types, int count, Random random) {
        if (types == null || types.isEmpty()) {
            throw new IllegalArgumentException("at least one challenge type must be enabled");
        }
        if (count < 1) {
            throw new IllegalArgumentException("at least one challenge must be drawn");
        }
        this.types = Collections.unmodifiableList(new ArrayList<>(EnumSet.copyOf(types)));
        this.count = count;
        this.random = random;
    }

    public List<ChallengeType> next() {
        List<ChallengeType> sequence = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            List<ChallengeType> choices = new ArrayList<>(types);
            if (!sequence.isEmpty() && choices.size() > 1) {
                choices.remove(sequence.get(sequence.size() - 1));
            }
            sequence.add(choices.get(random.nextInt(choices.size())));
        }
        return sequence;
    }
}
