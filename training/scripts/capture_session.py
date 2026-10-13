# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

"""Record one capture sequence of a pseudonymised subject from a webcam, for the test protocol of docs/test-plan.md.

The sequence is written to ``<output-root>/<subject>/<type>/<light>_<YYYYmmdd-HHMMSS>/``:

- ``frame_00000.jpg``, ``frame_00001.jpg``... as delivered by the camera, not mirrored, with no text on them;
- ``metadata.json``: subject pseudonym, capture type, lighting, camera, actual resolution and rate, start time,
  timestamp of each frame, script version and operating system. No personal data.

The default output root is ``captures/`` at the root of the repository, which Git ignores. An output root inside
the repository is refused unless it lies under ``captures/`` and ``git check-ignore`` confirms that it is ignored.

The preview is mirrored for comfort and shows the countdown, a REC indicator and, for the action types, the
instruction after 2 seconds of holding still. Press q or Esc to abort: the partial sequence is deleted.
``--dry-run`` opens no camera and only prints the target folder.

Usage::

    python training/scripts/capture_session.py --subject S01 --type bona_fide --light normal
    python training/scripts/capture_session.py --subject S02 --type action_blink --light low --camera 1
    python training/scripts/capture_session.py --subject S01 --type print_matte --light backlight --presenter S03
    python training/scripts/capture_session.py --subject S01 --type bona_fide --light normal --dry-run
"""

import argparse
import json
import os
import platform
import re
import shutil
import subprocess
import sys
import time
from datetime import datetime
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_OUTPUT_ROOT = REPO_ROOT / "captures"
SCRIPT_VERSION = "1.0"

SUBJECT_PATTERN = re.compile(r"^S\d{2,3}$")
BONA_FIDE_TYPES = ("bona_fide",)
ACTION_TYPES = ("action_blink", "action_smile", "action_turn_left", "action_turn_right")
ATTACK_TYPES = ("print_matte", "print_glossy", "screen_phone", "screen_laptop", "replay_video", "paper_mask")
CAPTURE_TYPES = BONA_FIDE_TYPES + ACTION_TYPES + ATTACK_TYPES
LIGHTS = ("normal", "low", "backlight")

# Instructions shown on the preview only. "Left" and "right" refer to the person (docs/decision-flow.md, section 5.3).
ACTION_INSTRUCTIONS = {
    "action_blink": "Please blink",
    "action_smile": "Please smile",
    "action_turn_left": "Turn your head to your left, then back",
    "action_turn_right": "Turn your head to your right, then back",
}
HOLD_STILL_S = 2.0
JPEG_QUALITY = 95
MAX_READ_FAILURES = 30
WINDOW_NAME = "capture_session (q or Esc to abort)"


def pseudonym(value):
    """Accept only a pseudonym such as S01 or S123, never a name."""
    if not SUBJECT_PATTERN.match(value):
        raise argparse.ArgumentTypeError(
            "{!r} is not a pseudonym. Use S followed by 2 or 3 digits, for example S01. Never use a name".format(value))
    return value


def bounded(kind, low, high):
    def parse(value):
        number = kind(value)
        if not low <= number <= high:
            raise argparse.ArgumentTypeError("{} is outside {} to {}".format(value, low, high))
        return number
    return parse


def parse_args():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--subject", required=True, type=pseudonym, help="pseudonym of the subject, for example S01")
    parser.add_argument("--type", required=True, choices=CAPTURE_TYPES, help="capture type, used as folder name")
    parser.add_argument("--light", required=True, choices=LIGHTS, help="lighting condition")
    parser.add_argument("--presenter", type=pseudonym, help="pseudonym of the attack presenter, for attack types")
    parser.add_argument("--camera", type=int, default=0, help="camera index (default 0)")
    parser.add_argument("--camera-model", default="", help="webcam model, for example \"Logitech C270\"")
    parser.add_argument("--seconds", type=bounded(float, 1, 60), default=10.0, help="recording length (default 10)")
    parser.add_argument("--width", type=int, default=1280, help="requested frame width (default 1280)")
    parser.add_argument("--height", type=int, default=720, help="requested frame height (default 720)")
    parser.add_argument("--fps-save", type=bounded(float, 1, 30), default=10.0,
                        help="frames saved per second (default 10)")
    parser.add_argument("--output-root", type=Path, default=DEFAULT_OUTPUT_ROOT,
                        help="root folder of the captures (default: captures/ of the repository, ignored by Git)")
    parser.add_argument("--countdown", type=bounded(float, 0, 30), default=3.0,
                        help="seconds before recording starts (default 3)")
    parser.add_argument("--no-preview", action="store_true", help="no preview window; Ctrl+C aborts")
    parser.add_argument("--dry-run", action="store_true", help="open nothing, print the target folder and exit")
    args = parser.parse_args()
    if len(args.camera_model) > 60:
        parser.error("--camera-model is limited to 60 characters")
    if args.presenter and args.type not in ATTACK_TYPES:
        parser.error("--presenter only applies to attack types")
    return args


def check_output_root(root, sequence_dir):
    """Refuse to write inside the repository, except into a folder under captures/ that Git ignores."""
    root = root.resolve()
    if root != REPO_ROOT and REPO_ROOT not in root.parents:
        return
    if root != DEFAULT_OUTPUT_ROOT and DEFAULT_OUTPUT_ROOT not in root.parents:
        sys.exit("Refused: {} is inside the repository. Use captures/ or a folder outside the repository".format(root))
    # captures/ only matches directories, so a file path inside the sequence folder is checked.
    probe = sequence_dir / "frame_00000.jpg"
    try:
        result = subprocess.run(["git", "-C", str(REPO_ROOT), "check-ignore", "-q", str(probe)],
                                stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=False)
    except OSError as error:
        sys.exit("Refused: cannot run git to check that {} is ignored ({})".format(root, error))
    if result.returncode != 0:
        sys.exit("Refused: Git does not ignore {}. Captures must never be committed".format(probe))


def draw_text(cv2, image, text, origin, scale=0.9, colour=(255, 255, 255)):
    cv2.putText(image, text, origin, cv2.FONT_HERSHEY_SIMPLEX, scale, (0, 0, 0), 4, cv2.LINE_AA)
    cv2.putText(image, text, origin, cv2.FONT_HERSHEY_SIMPLEX, scale, colour, 2, cv2.LINE_AA)


def guidance(capture_type, elapsed):
    if capture_type in ACTION_TYPES:
        return "Hold still" if elapsed < HOLD_STILL_S else ACTION_INSTRUCTIONS[capture_type]
    if capture_type in ATTACK_TYPES:
        return "Present the artefact"
    return "Look at the camera"


def show(cv2, frame, lines, recording, remaining):
    """Show a mirrored copy of the frame with overlay text. The frame itself is not modified."""
    preview = cv2.flip(frame, 1)
    if recording:
        cv2.circle(preview, (30, 30), 12, (0, 0, 255), -1)
        draw_text(cv2, preview, "REC {:4.1f} s".format(remaining), (52, 40), colour=(0, 0, 255))
    for row, text in enumerate(lines):
        draw_text(cv2, preview, text, (20, preview.shape[0] - 30 - 40 * (len(lines) - 1 - row)))
    cv2.imshow(WINDOW_NAME, preview)
    return cv2.waitKey(1) & 0xFF in (ord("q"), 27)


def record(args, sequence_dir):
    """Record the sequence. Returns the metadata, or None when aborted."""
    import cv2  # imported here so that --dry-run works without OpenCV

    backend = cv2.CAP_DSHOW if os.name == "nt" else cv2.CAP_ANY
    camera = cv2.VideoCapture(args.camera, backend)
    if not camera.isOpened():
        sys.exit("Cannot open camera {}".format(args.camera))
    camera.set(cv2.CAP_PROP_FRAME_WIDTH, args.width)
    camera.set(cv2.CAP_PROP_FRAME_HEIGHT, args.height)
    ok, frame = camera.read()
    if not ok:
        camera.release()
        sys.exit("Camera {} returned no frame".format(args.camera))
    height, width = frame.shape[:2]
    print("Camera {}: {} x {} (requested {} x {})".format(args.camera, width, height, args.width, args.height))

    timestamps_ms = []
    frames_read = 0
    failures = 0
    aborted = False
    interval = 1.0 / args.fps_save
    try:
        # Countdown: frames are read to keep exposure settled, nothing is saved.
        start = time.monotonic()
        last_printed = None
        while time.monotonic() - start < args.countdown:
            ok, frame = camera.read()
            left = args.countdown - (time.monotonic() - start)
            if args.no_preview:
                if last_printed != int(left) + 1:
                    last_printed = int(left) + 1
                    print("Starting in {}".format(last_printed))
            elif ok and show(cv2, frame, ["Starting in {}".format(int(left) + 1), guidance(args.type, 0)],
                             False, args.seconds):
                aborted = True
                break

        if not aborted:
            sequence_dir.mkdir(parents=True, exist_ok=False)
            print("Recording {} s into {}".format(args.seconds, sequence_dir))
            start_time = datetime.now().astimezone()
            start = time.monotonic()
            next_save = start
            while True:
                ok, frame = camera.read()
                now = time.monotonic()
                elapsed = now - start
                if elapsed >= args.seconds:
                    break
                if not ok:
                    failures += 1
                    if failures >= MAX_READ_FAILURES:
                        print("The camera stopped delivering frames", file=sys.stderr)
                        aborted = True
                        break
                    continue
                failures = 0
                frames_read += 1
                if now >= next_save:
                    path = sequence_dir / "frame_{:05d}.jpg".format(len(timestamps_ms))
                    if not cv2.imwrite(str(path), frame, [cv2.IMWRITE_JPEG_QUALITY, JPEG_QUALITY]):
                        print("Cannot write {}".format(path), file=sys.stderr)
                        aborted = True
                        break
                    timestamps_ms.append(round(elapsed * 1000, 1))
                    # Keep a regular grid of save times, without bursts after a slow frame.
                    next_save = max(next_save + interval, now + interval / 2)
                if not args.no_preview and show(cv2, frame, [guidance(args.type, elapsed)], True,
                                                args.seconds - elapsed):
                    aborted = True
                    break
    except KeyboardInterrupt:
        aborted = True
    finally:
        fps_reported = camera.get(cv2.CAP_PROP_FPS)
        camera.release()
        if not args.no_preview:
            cv2.destroyAllWindows()

    if aborted:
        if sequence_dir.exists():
            shutil.rmtree(sequence_dir)
        print("Aborted. Partial sequence deleted")
        return None

    return {
        "subject": args.subject,
        "presenter": args.presenter,
        "type": args.type,
        "light": args.light,
        "camera_index": args.camera,
        "camera_model": args.camera_model,
        "requested_resolution": [args.width, args.height],
        "resolution": [width, height],
        "camera_fps_reported": round(fps_reported, 2),
        "camera_fps_measured": round(frames_read / args.seconds, 2),
        "save_fps": args.fps_save,
        "jpeg_quality": JPEG_QUALITY,
        "mirrored": False,
        "start_time": start_time.isoformat(timespec="milliseconds"),
        "duration_s": args.seconds,
        "frame_count": len(timestamps_ms),
        "timestamps_ms": timestamps_ms,
        "script_version": SCRIPT_VERSION,
        "opencv_version": cv2.__version__,
        "python_version": platform.python_version(),
        "host_os": "{} {}".format(platform.system(), platform.release()),
    }


def main():
    args = parse_args()
    output_root = args.output_root.resolve()
    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    sequence_dir = output_root / args.subject / args.type / "{}_{}".format(args.light, stamp)
    check_output_root(output_root, sequence_dir)
    if sequence_dir.exists():
        sys.exit("{} already exists".format(sequence_dir))

    if args.dry_run:
        print("Dry run: nothing opened, nothing written")
        print("Target folder: {}".format(sequence_dir))
        print("Frames: about {} at {} per second over {} s, metadata.json".format(
            int(args.seconds * args.fps_save), args.fps_save, args.seconds))
        return

    metadata = record(args, sequence_dir)
    if metadata is None:
        sys.exit(1)
    (sequence_dir / "metadata.json").write_text(json.dumps(metadata, indent=2) + "\n", encoding="utf-8")
    print("Saved {} frames and metadata.json in {}".format(metadata["frame_count"], sequence_dir))


if __name__ == "__main__":
    main()
