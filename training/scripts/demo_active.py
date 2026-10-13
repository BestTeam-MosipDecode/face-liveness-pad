# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

"""Webcam demo of the active check: random challenges, judged against a baseline.

Each round draws ``--challenges`` challenges (section 5.1 of ``docs/decision-flow.md``). For each one
the person holds still, then performs the instruction. A round fails on a timeout, a turn in the wrong
direction, a lost face (more than 1 000 ms without a face, several faces or a new face box) or a passive
score at the attack threshold or below.

``--log`` writes the measures of every analysed frame to a CSV file, to calibrate the factors. It holds
numbers only, no image.

Usage::

    python training/scripts/demo_active.py [--camera 0] [--rounds 5] [--challenges 2] [--log measures.csv]

Press q or Esc in the preview window to stop.
"""

import argparse
import csv
import sys
import time
from pathlib import Path

import cv2
import numpy as np

REPO_ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(REPO_ROOT / "training"))

from liveness import actions, quality  # noqa: E402
from liveness.face_mesh import FaceMesh  # noqa: E402
from liveness.minifas import MODELS_DIR, MiniFAS, load_manifest  # noqa: E402
from liveness.passive import ATTACK_THRESHOLD, MIN_FRAMES  # noqa: E402
from liveness.yunet import YuNet, classifier_box, iou  # noqa: E402

MIN_TRACK_OVERLAP = 0.3
MAX_NO_FACE_MS = 1000
PAUSE_MS = 2000


class Stop(Exception):
    pass


class Demo:
    def __init__(self, args):
        manifest = load_manifest()
        self.detector = YuNet(MODELS_DIR / manifest["yunet"]["file"])
        self.mesh = FaceMesh(MODELS_DIR / manifest["face_mesh_v2"]["file"])
        self.classifier = MiniFAS()
        self.args = args
        self.camera = cv2.VideoCapture(args.camera)
        self.camera.set(cv2.CAP_PROP_FRAME_WIDTH, args.width)
        self.camera.set(cv2.CAP_PROP_FRAME_HEIGHT, args.height)
        if not self.camera.isOpened():
            sys.exit("cannot open camera {}".format(args.camera))
        self.start = time.monotonic()
        self.writer = None
        if args.log:
            self.log_file = open(args.log, "w", newline="", encoding="utf-8")
            self.writer = csv.writer(self.log_file)
            self.writer.writerow(["round", "challenge", "phase", "t_ms", "eye", "mouth", "yaw", "score", "result"])

    def now(self):
        return (time.monotonic() - self.start) * 1000

    def frames(self):
        """Yield ``(timestamp_ms, frame)`` every ``--sampling-ms``, showing the preview between analyses."""
        last = -self.args.sampling_ms
        while True:
            ok, frame = self.camera.read()
            if not ok:
                continue
            t = self.now()
            if t - last >= self.args.sampling_ms:
                last = t
                yield t, frame

    def show(self, frame, lines, colour=(255, 255, 255)):
        if self.args.no_preview:
            return
        preview = frame.copy()
        for i, line in enumerate(lines):
            cv2.putText(preview, line, (20, 45 + 40 * i), cv2.FONT_HERSHEY_SIMPLEX, 1.0, colour, 2)
        cv2.imshow("active liveness", preview)
        if cv2.waitKey(1) & 0xFF in (ord("q"), 27):
            raise Stop()

    def pause(self, text, colour):
        end = self.now() + PAUSE_MS
        for t, frame in self.frames():
            self.show(frame, [text], colour)
            if t >= end:
                return

    def run_round(self, number):
        sequence = actions.draw_challenges(self.args.challenges)
        print("\nround {}: {}".format(number, ", ".join(sequence)))
        scores, track, last_face = [], None, self.now()
        for challenge_type in sequence:
            challenge = actions.Challenge(challenge_type, self.now())
            for t, frame in self.frames():
                faces = self.detector.detect(frame)
                measures, score = None, None
                if len(faces) > 1:
                    return "FACE_CONTINUITY_LOST (several faces)"
                if faces:
                    face = faces[0]
                    if track is not None and iou(track, face.box) < MIN_TRACK_OVERLAP:
                        return "FACE_CONTINUITY_LOST (new face box)"
                    track, last_face = face.box, t
                    points, presence = self.mesh.landmarks(frame, face.box)
                    measures = actions.Measures.from_landmarks(points)
                    q = quality.check(frame, face, check_yaw=False)
                    if q.passed and abs(q.yaw_deg) <= quality.MAX_YAW_DEG:
                        score = self.classifier.score(frame, classifier_box(face.box))
                        scores.append(score)
                elif t - last_face > MAX_NO_FACE_MS:
                    return "FACE_CONTINUITY_LOST (no face)"

                if len(scores) >= MIN_FRAMES and np.median(scores[-MIN_FRAMES:]) <= ATTACK_THRESHOLD:
                    return "PAD_ATTACK_DETECTED"
                result = challenge.update(t, measures)
                if self.writer:
                    self.writer.writerow([number, challenge_type, challenge.phase, round(t)]
                                         + (["{:.4f}".format(v) for v in (measures.eye, measures.mouth, measures.yaw)]
                                            if measures else ["", "", ""])
                                         + ["" if score is None else "{:.4f}".format(score), result])
                if measures:
                    print("  {:8.0f} ms  {:<11} {}  score {}  {}".format(
                        t, challenge.phase, measures, "-" if score is None else "{:.3f}".format(score), result))
                self.show(frame, [challenge.prompt if result == actions.PENDING else result,
                                  "{} / {}".format(sequence.index(challenge_type) + 1, len(sequence))])
                if result == actions.DETECTED:
                    print("  baseline {}, detected {:.0f} ms after the instruction".format(
                        challenge.baseline, t - challenge.instruction_ms))
                    break
                if result != actions.PENDING:
                    return result
        if scores and np.mean(scores) <= ATTACK_THRESHOLD:
            return "PAD_ATTACK_DETECTED"
        return "PASSED"

    def run(self):
        outcomes = []
        try:
            for number in range(1, self.args.rounds + 1):
                outcome = self.run_round(number)
                outcomes.append(outcome)
                print("round {}: {}".format(number, outcome))
                self.pause(outcome, (0, 160, 0) if outcome == "PASSED" else (0, 0, 220))
        except Stop:
            pass
        finally:
            self.camera.release()
            cv2.destroyAllWindows()
            if self.writer:
                self.log_file.close()
        print("\n{} of {} rounds passed".format(outcomes.count("PASSED"), len(outcomes)))


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--camera", type=int, default=0)
    parser.add_argument("--width", type=int, default=1280)
    parser.add_argument("--height", type=int, default=720)
    parser.add_argument("--sampling-ms", type=int, default=100)
    parser.add_argument("--rounds", type=int, default=5)
    parser.add_argument("--challenges", type=int, default=2)
    parser.add_argument("--log", type=Path, help="CSV file for the measures of each analysed frame")
    parser.add_argument("--no-preview", action="store_true")
    Demo(parser.parse_args()).run()


if __name__ == "__main__":
    main()
