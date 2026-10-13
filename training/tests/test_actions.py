# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

"""Unit tests of the action detectors on synthetic measures.

Run with::

    python -m unittest discover -s training/tests
"""

import random
import sys
import unittest
from collections import Counter
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from liveness.actions import (BLINK, CHALLENGE_TYPES, DETECTED, FAILED, PENDING, SMILE, TIMEOUT,  # noqa: E402
                              TURN_LEFT, TURN_RIGHT, Challenge, Measures, draw_challenges, make_detector)

BASE = Measures(eye=0.30, mouth=0.40, yaw=0.50)


def run(challenge, sequence, baseline=BASE):
    detector = make_detector(challenge, baseline)
    result = PENDING
    for m in sequence:
        result = detector.update(m)
        if result != PENDING:
            break
    return result


def eyes(*values):
    return [Measures(v, BASE.mouth, BASE.yaw) for v in values]


def mouths(*values):
    return [Measures(BASE.eye, v, BASE.yaw) for v in values]


def yaws(*values):
    return [Measures(BASE.eye, BASE.mouth, v) for v in values]


class BlinkTest(unittest.TestCase):
    def test_blink_detected(self):
        self.assertEqual(run(BLINK, eyes(0.30, 0.15, 0.29)), DETECTED)

    def test_eyes_closed_without_reopening(self):
        self.assertEqual(run(BLINK, eyes(0.30, 0.15, 0.15, 0.20)), PENDING)

    def test_partial_closure_ignored(self):
        self.assertEqual(run(BLINK, eyes(0.30, 0.20, 0.30, 0.20, 0.30)), PENDING)


class SmileTest(unittest.TestCase):
    def test_smile_on_two_frames(self):
        self.assertEqual(run(SMILE, mouths(0.40, 0.45, 0.46)), DETECTED)

    def test_single_frame_is_not_enough(self):
        self.assertEqual(run(SMILE, mouths(0.45, 0.40, 0.45, 0.40)), PENDING)


class HeadTurnTest(unittest.TestCase):
    def test_turn_left_and_back(self):
        self.assertEqual(run(TURN_LEFT, yaws(0.55, 0.66, 0.60, 0.55)), DETECTED)

    def test_turn_left_without_return(self):
        self.assertEqual(run(TURN_LEFT, yaws(0.66, 0.70, 0.68)), PENDING)

    def test_turn_right_when_left_requested_fails(self):
        self.assertEqual(run(TURN_LEFT, yaws(0.40, 0.34)), FAILED)

    def test_turn_right_and_back(self):
        self.assertEqual(run(TURN_RIGHT, yaws(0.40, 0.34, 0.45)), DETECTED)

    def test_turn_left_when_right_requested_fails(self):
        self.assertEqual(run(TURN_RIGHT, yaws(0.66)), FAILED)


class ChallengeTest(unittest.TestCase):
    def test_baseline_then_detection(self):
        challenge = Challenge(BLINK, start_ms=0)
        for t in (0, 100, 200, 300, 400, 500, 600):
            self.assertEqual(challenge.update(t, BASE), PENDING)
        self.assertEqual(challenge.phase, Challenge.INSTRUCTION)
        self.assertEqual(challenge.update(700, eyes(0.1)[0]), PENDING)
        self.assertEqual(challenge.update(800, BASE), DETECTED)

    def test_action_held_from_the_start_raises_the_baseline(self):
        challenge = Challenge(SMILE, start_ms=0)
        smiling = mouths(0.46)[0]
        for t in range(0, 700, 100):
            challenge.update(t, smiling)
        self.assertEqual(challenge.update(700, smiling), PENDING)
        self.assertEqual(challenge.update(800, smiling), PENDING)

    def test_baseline_needs_three_frames(self):
        challenge = Challenge(BLINK, start_ms=0)
        challenge.update(0, BASE)
        challenge.update(700, BASE)
        self.assertEqual(challenge.phase, Challenge.HOLD_STILL)
        challenge.update(800, BASE)
        self.assertEqual(challenge.phase, Challenge.INSTRUCTION)

    def test_timeout(self):
        challenge = Challenge(BLINK, start_ms=0, timeout_ms=8000)
        for t in range(0, 8001, 100):
            self.assertEqual(challenge.update(t, BASE), PENDING)
        self.assertEqual(challenge.update(8100, BASE), TIMEOUT)

    def test_frames_without_face_do_not_count(self):
        challenge = Challenge(BLINK, start_ms=0)
        self.assertEqual(challenge.update(100, None), PENDING)
        self.assertEqual(challenge.samples, [])


class DrawTest(unittest.TestCase):
    def test_no_immediate_repetition(self):
        rng = random.Random(1)
        for _ in range(500):
            sequence = draw_challenges(4, rng=rng)
            self.assertTrue(all(a != b for a, b in zip(sequence, sequence[1:])))

    def test_single_type_repeats(self):
        self.assertEqual(draw_challenges(3, types=[BLINK], rng=random.Random(0)), [BLINK] * 3)

    def test_first_challenge_is_uniform(self):
        rng = random.Random(7)
        counts = Counter(draw_challenges(2, rng=rng)[0] for _ in range(8000))
        for challenge in CHALLENGE_TYPES:
            self.assertAlmostEqual(counts[challenge] / 8000, 0.25, delta=0.02)


if __name__ == "__main__":
    unittest.main()
