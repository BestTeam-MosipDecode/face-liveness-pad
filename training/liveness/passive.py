# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

"""Passive decision over a window of frame scores, as described in section 4 of ``docs/decision-flow.md``.

The window keeps the scored frames of the current track from the last ``window_ms``. Once it holds
``min_frames`` frames, the median score is compared with the pass and attack thresholds.
"""

from collections import deque

import numpy as np

PASS_THRESHOLD = 0.85
ATTACK_THRESHOLD = 0.30
MIN_FRAMES = 5
WINDOW_MS = 3000

PASSED = "PASSED"
ATTACK = "PAD_ATTACK_DETECTED"
CHALLENGE = "ACTIVE_CHALLENGE"
PENDING = "PENDING"


class PassiveWindow:
    def __init__(self, pass_threshold=PASS_THRESHOLD, attack_threshold=ATTACK_THRESHOLD,
                 min_frames=MIN_FRAMES, window_ms=WINDOW_MS):
        self.pass_threshold = pass_threshold
        self.attack_threshold = attack_threshold
        self.min_frames = min_frames
        self.window_ms = window_ms
        self.frames = deque()

    def clear(self):
        """Start a new track: the face box changed or the face was lost."""
        self.frames.clear()

    def add(self, timestamp_ms, score):
        self.frames.append((timestamp_ms, score))
        while self.frames and self.frames[0][0] < timestamp_ms - self.window_ms:
            self.frames.popleft()

    def aggregate(self):
        """Median score of the window, or ``None`` while it holds fewer than ``min_frames`` frames."""
        if len(self.frames) < self.min_frames:
            return None
        return float(np.median([score for _, score in self.frames]))

    def decision(self):
        score = self.aggregate()
        if score is None:
            return PENDING
        if score >= self.pass_threshold:
            return PASSED
        if score <= self.attack_threshold:
            return ATTACK
        return CHALLENGE
