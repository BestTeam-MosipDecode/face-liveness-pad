# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

"""Active check: challenge drawing, baseline and action detectors, as described in section 5 of
``docs/decision-flow.md``.

Measures come from the Face Mesh V2 landmarks (``liveness/face_mesh.py``). Each detector judges
the action against a baseline taken while the person holds still, so that an action already in
progress before the instruction (a smile held from the start) is not counted.

The factors are starting values, to calibrate on the team captures. The Java engine ports this
module (``BlinkDetector``, ``SmileDetector``, ``HeadTurnDetector``, ``ChallengeGenerator``).
"""

import random

import numpy as np

from liveness.face_mesh import LEFT_EYE, RIGHT_EYE, eye_aspect_ratio, mouth_width_ratio, yaw_ratio

BLINK = "BLINK"
SMILE = "SMILE"
TURN_LEFT = "TURN_LEFT"
TURN_RIGHT = "TURN_RIGHT"
CHALLENGE_TYPES = (BLINK, SMILE, TURN_LEFT, TURN_RIGHT)

INSTRUCTIONS = {
    BLINK: "Please blink",
    SMILE: "Please smile",
    TURN_LEFT: "Turn your head to the left",
    TURN_RIGHT: "Turn your head to the right",
}

BLINK_CLOSED = 0.6
BLINK_OPEN = 0.85
SMILE_FACTOR = 1.10
SMILE_FRAMES = 2
TURN_AMPLITUDE = 0.15
TURN_RETURN = 0.08

MIN_BASELINE_FRAMES = 3
BASELINE_MS = 600
CHALLENGE_TIMEOUT_MS = 8000

PENDING = "PENDING"
DETECTED = "DETECTED"
FAILED = "CHALLENGE_FAILED"
TIMEOUT = "CHALLENGE_TIMEOUT"


class Measures:
    """Measures of one frame used by the detectors."""

    def __init__(self, eye, mouth, yaw):
        self.eye = eye
        self.mouth = mouth
        self.yaw = yaw

    @classmethod
    def from_landmarks(cls, points):
        eye = (eye_aspect_ratio(points, RIGHT_EYE) + eye_aspect_ratio(points, LEFT_EYE)) / 2
        return cls(float(eye), float(mouth_width_ratio(points)), float(yaw_ratio(points)))

    @classmethod
    def median(cls, samples):
        return cls(*(float(np.median([getattr(s, name) for s in samples])) for name in ("eye", "mouth", "yaw")))

    def __repr__(self):
        return "Measures(eye={:.3f}, mouth={:.3f}, yaw={:.3f})".format(self.eye, self.mouth, self.yaw)


def draw_challenges(count, types=CHALLENGE_TYPES, rng=None):
    """Draw ``count`` challenges among ``types``, never the same type twice in a row unless only one type
    is enabled. ``rng`` defaults to the operating system generator; tests pass a seeded ``random.Random``."""
    rng = rng or random.SystemRandom()
    sequence = []
    for _ in range(count):
        choices = [t for t in types if not sequence or t != sequence[-1]] or list(types)
        sequence.append(rng.choice(choices))
    return sequence


class BlinkDetector:
    """Mean eye aspect ratio drops to at most 0.6 x baseline, then rises back to at least 0.85 x baseline."""

    def __init__(self, baseline, closed=BLINK_CLOSED, reopened=BLINK_OPEN):
        self.closed_level = baseline.eye * closed
        self.open_level = baseline.eye * reopened
        self.closed = False

    def update(self, m):
        if m.eye <= self.closed_level:
            self.closed = True
        elif self.closed and m.eye >= self.open_level:
            return DETECTED
        return PENDING


class SmileDetector:
    """Mouth width ratio reaches at least 1.10 x baseline on 2 consecutive analysed frames."""

    def __init__(self, baseline, factor=SMILE_FACTOR, frames=SMILE_FRAMES):
        self.level = baseline.mouth * factor
        self.frames = frames
        self.streak = 0

    def update(self, m):
        self.streak = self.streak + 1 if m.mouth >= self.level else 0
        return DETECTED if self.streak >= self.frames else PENDING


class HeadTurnDetector:
    """Yaw ratio moves by the amplitude in the requested direction, then comes back near the baseline.

    ``direction`` is +1 for a turn to the person's left (the ratio rises in a non-mirrored image) and -1
    for a turn to the right. A turn of the same amplitude in the opposite direction fails the challenge.
    """

    def __init__(self, baseline, direction, amplitude=TURN_AMPLITUDE, back=TURN_RETURN):
        self.baseline = baseline.yaw
        self.direction = direction
        self.amplitude = amplitude
        self.back = back
        self.turned = False

    def update(self, m):
        offset = (m.yaw - self.baseline) * self.direction
        if offset <= -self.amplitude:
            return FAILED
        if offset >= self.amplitude:
            self.turned = True
        elif self.turned and abs(offset) <= self.back:
            return DETECTED
        return PENDING


def make_detector(challenge, baseline):
    if challenge == BLINK:
        return BlinkDetector(baseline)
    if challenge == SMILE:
        return SmileDetector(baseline)
    if challenge == TURN_LEFT:
        return HeadTurnDetector(baseline, +1)
    if challenge == TURN_RIGHT:
        return HeadTurnDetector(baseline, -1)
    raise ValueError("unknown challenge {}".format(challenge))


class Challenge:
    """One challenge: hold still for the baseline, then show the instruction and wait for the action.

    The deadline counts from the start of the hold-still phase, which counts towards the timeout.
    ``update`` returns ``PENDING``, ``DETECTED``, ``CHALLENGE_FAILED`` or ``CHALLENGE_TIMEOUT``.
    """

    HOLD_STILL = "HOLD_STILL"
    INSTRUCTION = "INSTRUCTION"

    def __init__(self, challenge, start_ms, baseline_ms=BASELINE_MS, timeout_ms=CHALLENGE_TIMEOUT_MS,
                 min_baseline_frames=MIN_BASELINE_FRAMES):
        self.challenge = challenge
        self.start_ms = start_ms
        self.baseline_ms = baseline_ms
        self.deadline_ms = start_ms + timeout_ms
        self.min_baseline_frames = min_baseline_frames
        self.phase = self.HOLD_STILL
        self.samples = []
        self.baseline = None
        self.detector = None
        self.instruction_ms = None

    @property
    def prompt(self):
        return "Hold still" if self.phase == self.HOLD_STILL else INSTRUCTIONS[self.challenge]

    def update(self, timestamp_ms, measures):
        """Feed the measures of one analysed frame, or ``None`` when the frame had no usable face."""
        if timestamp_ms > self.deadline_ms:
            return TIMEOUT
        if measures is None:
            return PENDING
        if self.phase == self.HOLD_STILL:
            self.samples.append(measures)
            if (timestamp_ms - self.start_ms >= self.baseline_ms
                    and len(self.samples) >= self.min_baseline_frames):
                self.baseline = Measures.median(self.samples)
                self.detector = make_detector(self.challenge, self.baseline)
                self.phase = self.INSTRUCTION
                self.instruction_ms = timestamp_ms
            return PENDING
        return self.detector.update(measures)
