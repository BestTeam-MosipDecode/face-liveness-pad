# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

"""Webcam demo of the passive check: prints the quality measures, the frame score and the decision.

Frames are analysed every ``--sampling-ms``, as in section 3.1 of ``docs/decision-flow.md``. The
passive window is cleared when no face, several faces or a new face box (overlap below 0.3) is seen.
Nothing is written to disk.

Usage::

    python training/scripts/demo_passive.py [--camera 0] [--no-preview] [--seconds 30]

Press q or Esc in the preview window to stop.
"""

import argparse
import sys
import time
from pathlib import Path

import cv2

REPO_ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(REPO_ROOT / "training"))

from liveness import quality  # noqa: E402
from liveness.minifas import MODELS_DIR, MiniFAS, load_manifest  # noqa: E402
from liveness.passive import ATTACK, PASSED, PassiveWindow  # noqa: E402
from liveness.yunet import YuNet, classifier_box, iou  # noqa: E402

MIN_TRACK_OVERLAP = 0.3
COLOURS = {PASSED: (0, 160, 0), ATTACK: (0, 0, 220)}


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--camera", type=int, default=0)
    parser.add_argument("--width", type=int, default=1280)
    parser.add_argument("--height", type=int, default=720)
    parser.add_argument("--sampling-ms", type=int, default=100)
    parser.add_argument("--seconds", type=float, default=0, help="stop after this time, 0 for no limit")
    parser.add_argument("--no-preview", action="store_true")
    args = parser.parse_args()

    detector = YuNet(MODELS_DIR / load_manifest()["yunet"]["file"])
    classifier = MiniFAS()
    window = PassiveWindow()

    camera = cv2.VideoCapture(args.camera)
    camera.set(cv2.CAP_PROP_FRAME_WIDTH, args.width)
    camera.set(cv2.CAP_PROP_FRAME_HEIGHT, args.height)
    if not camera.isOpened():
        sys.exit("cannot open camera {}".format(args.camera))

    start = time.monotonic()
    last_analysis = -args.sampling_ms
    track = None
    status = "starting"
    try:
        while True:
            ok, frame = camera.read()
            if not ok:
                print("INVALID_FRAME")
                continue
            now_ms = (time.monotonic() - start) * 1000
            if args.seconds and now_ms > args.seconds * 1000:
                break

            if now_ms - last_analysis >= args.sampling_ms:
                last_analysis = now_ms
                begin = time.perf_counter()
                faces = detector.detect(frame)
                if not faces:
                    window.clear()
                    track = None
                    status = "NO_FACE"
                elif len(faces) > 1:
                    window.clear()
                    track = None
                    status = "MULTIPLE_FACES"
                else:
                    face = faces[0]
                    if track is not None and iou(track, face.box) < MIN_TRACK_OVERLAP:
                        window.clear()
                    track = face.box
                    result = quality.check(frame, face)
                    if result.passed:
                        score = classifier.score(frame, classifier_box(face.box))
                        window.add(now_ms, score)
                        aggregate = window.aggregate()
                        status = "score {:.3f} median {} {}".format(
                            score, "-" if aggregate is None else "{:.3f}".format(aggregate), window.decision())
                    else:
                        status = "LOW_QUALITY ({})".format(result.tip)
                    print("{:8.0f} ms  size {:4.0f}px  light {:5.1f}  sharp {:7.1f}  yaw {:+5.1f}  {}  [{:.0f} ms]".format(
                        now_ms, result.face_px, result.brightness, result.sharpness, result.yaw_deg, status,
                        (time.perf_counter() - begin) * 1000))
                if not faces or len(faces) > 1:
                    print("{:8.0f} ms  {}".format(now_ms, status))

            if not args.no_preview:
                preview = frame.copy()
                if track is not None:
                    x, y, w, h = (int(v) for v in track)
                    cv2.rectangle(preview, (x, y), (x + w, y + h), (255, 255, 255), 2)
                colour = COLOURS.get(window.decision(), (0, 200, 255))
                cv2.putText(preview, status, (20, 40), cv2.FONT_HERSHEY_SIMPLEX, 0.9, colour, 2)
                cv2.imshow("passive liveness", preview)
                if cv2.waitKey(1) & 0xFF in (ord("q"), 27):
                    break
    finally:
        camera.release()
        cv2.destroyAllWindows()


if __name__ == "__main__":
    main()
