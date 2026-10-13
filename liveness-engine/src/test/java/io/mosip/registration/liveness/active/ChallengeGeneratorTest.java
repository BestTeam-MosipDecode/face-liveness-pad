/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.active;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

class ChallengeGeneratorTest {

    static final EnumSet<ChallengeType> ALL = EnumSet.allOf(ChallengeType.class);

    @Test
    void drawsTheConfiguredNumber() {
        assertEquals(3, new ChallengeGenerator(ALL, 3, new Random(1)).next().size());
    }

    @Test
    void noImmediateRepetition() {
        ChallengeGenerator generator = new ChallengeGenerator(ALL, 4, new Random(1));
        for (int i = 0; i < 2000; i++) {
            List<ChallengeType> sequence = generator.next();
            for (int j = 1; j < sequence.size(); j++) {
                assertNotEquals(sequence.get(j - 1), sequence.get(j));
            }
        }
    }

    @Test
    void singleTypeRepeats() {
        List<ChallengeType> sequence = new ChallengeGenerator(Collections.singleton(ChallengeType.BLINK), 3,
                new Random(0)).next();
        assertEquals(Arrays.asList(ChallengeType.BLINK, ChallengeType.BLINK, ChallengeType.BLINK), sequence);
    }

    @Test
    void onlyEnabledTypesAreDrawn() {
        EnumSet<ChallengeType> enabled = EnumSet.of(ChallengeType.SMILE, ChallengeType.TURN_LEFT);
        ChallengeGenerator generator = new ChallengeGenerator(enabled, 5, new Random(3));
        for (int i = 0; i < 200; i++) {
            assertTrue(enabled.containsAll(generator.next()));
        }
    }

    @Test
    void noBiasOnEitherPosition() {
        ChallengeGenerator generator = new ChallengeGenerator(ALL, 2, new Random(7));
        Map<ChallengeType, Integer> first = new EnumMap<>(ChallengeType.class);
        Map<ChallengeType, Integer> second = new EnumMap<>(ChallengeType.class);
        int draws = 20000;
        for (int i = 0; i < draws; i++) {
            List<ChallengeType> sequence = generator.next();
            first.merge(sequence.get(0), 1, Integer::sum);
            second.merge(sequence.get(1), 1, Integer::sum);
        }
        for (ChallengeType type : ALL) {
            assertEquals(0.25, first.get(type) / (double) draws, 0.015, "first " + type);
            assertEquals(0.25, second.get(type) / (double) draws, 0.015, "second " + type);
        }
    }

    @Test
    void sequencesDifferBetweenAttempts() {
        ChallengeGenerator generator = new ChallengeGenerator(ALL, 2);
        int distinct = (int) java.util.stream.IntStream.range(0, 50).mapToObj(i -> generator.next()).distinct().count();
        assertTrue(distinct > 1);
    }

    @Test
    void invalidArgumentsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ChallengeGenerator(EnumSet.noneOf(ChallengeType.class), 2));
        assertThrows(IllegalArgumentException.class, () -> new ChallengeGenerator(ALL, 0));
    }
}
