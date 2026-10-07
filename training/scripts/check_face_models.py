# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

"""Check the packaged detector and landmark model on the sample images of Silent-Face-Anti-Spoofing.

For each image, the script prints numbers only and writes nothing:

- the YuNet detection and its overlap with the box of the reference detector;
- the MiniFASNet scores computed with each of the two boxes;
- the face presence, eye aspect ratios, mouth width ratio and yaw ratio from Face Mesh;
- the distance between the Face Mesh eye centres and the YuNet eye keypoints;
- processing times.

Usage::

    python training/scripts/check_face_models.py --silent-face-dir ../Silent-Face-Anti-Spoofing
"""

import argparse
import json
import sys
import time
from pathlib import Path

import cv2
import numpy as np
import onnxruntime as ort

REPO_ROOT = Path(__file__).resolve().parents[2]
MODELS_DIR = REPO_ROOT / "liveness-engine" / "src" / "main" / "resources" / "models"
sys.path.insert(0, str(REPO_ROOT / "training"))

from liveness.crop import crop_patch  # noqa: E402
from liveness.face_mesh import (LEFT_EYE, RIGHT_EYE, FaceMesh, eye_aspect_ratio,  # noqa: E402
                                mouth_width_ratio, yaw_ratio)
from liveness.yunet import YuNet, classifier_box, iou  # noqa: E402

# Boxes [x, y, w, h] of the reference detector, from training/README.md.
REFERENCE_BOXES = {
    "image_F1.jpg": [178, 136, 223, 225],
    "image_F2.jpg": [120, 252, 252, 256],
    "image_T1.jpg": [106, 147, 207, 213],
}
TIMING_RUNS = 20


def real_score(sessions, image, box):
    """Mean probability of the real class over the two MiniFASNet models."""
    total = 0.0
    for scale, session in sessions:
        patch = crop_patch(image, [int(v) for v in box], scale)
        logits = session.run(["logits"], {"input": patch.transpose(2, 0, 1)[np.newaxis].astype(np.float32)})[0][0]
        probabilities = np.exp(logits - logits.max())
        total += probabilities[1] / probabilities.sum()
    return total / len(sessions)


def mean_ms(function, runs=TIMING_RUNS):
    function()
    start = time.perf_counter()
    for _ in range(runs):
        function()
    return (time.perf_counter() - start) * 1000 / runs


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--silent-face-dir", required=True, type=Path)
    args = parser.parse_args()

    manifest = {m["name"]: m for m in json.loads((MODELS_DIR / "models.json").read_text(encoding="utf-8"))["models"]}
    detector = YuNet(MODELS_DIR / manifest["yunet"]["file"])
    mesh = FaceMesh(MODELS_DIR / manifest["face_mesh_v2"]["file"])
    sessions = [(manifest[name]["cropScale"],
                 ort.InferenceSession(str(MODELS_DIR / manifest[name]["file"]), providers=["CPUExecutionProvider"]))
                for name in ("minifasnet_v2", "minifasnet_v1se")]

    for name, reference_box in REFERENCE_BOXES.items():
        image = cv2.imread(str(args.silent_face_dir / "images" / "sample" / name))
        faces = detector.detect(image)
        print("\n{}: {} face(s) found by YuNet".format(name, len(faces)))
        if not faces:
            continue
        face = faces[0]
        square = classifier_box(face.box)
        print("  YuNet box {} score {:.4f}, reference box {}, IoU {:.3f}, IoU of the square box {:.3f}".format(
            [round(v) for v in face.box], face.score, reference_box, iou(face.box, reference_box),
            iou(square, reference_box)))
        print("  real-class score: reference box {:.4f}, YuNet box {:.4f}, square YuNet box {:.4f}".format(
            real_score(sessions, image, reference_box), real_score(sessions, image, face.box),
            real_score(sessions, image, square)))

        points, presence = mesh.landmarks(image, face.box)
        right_ear, left_ear = eye_aspect_ratio(points, RIGHT_EYE), eye_aspect_ratio(points, LEFT_EYE)
        mesh_right_eye = points[list(RIGHT_EYE), :2].mean(axis=0)
        mesh_left_eye = points[list(LEFT_EYE), :2].mean(axis=0)
        yunet_right_eye, yunet_left_eye = np.array(face.keypoints[0]), np.array(face.keypoints[1])
        eye_distance = np.linalg.norm(yunet_right_eye - yunet_left_eye)
        offset = max(np.linalg.norm(mesh_right_eye - yunet_right_eye),
                     np.linalg.norm(mesh_left_eye - yunet_left_eye)) / eye_distance
        print("  Face Mesh: presence {:.4f}, eye aspect ratio right {:.3f} left {:.3f}, mouth width ratio {:.3f}, "
              "yaw ratio {:.3f}".format(presence, right_ear, left_ear, mouth_width_ratio(points), yaw_ratio(points)))
        print("  eye centres: largest offset to YuNet keypoints = {:.1%} of the distance between the eyes".format(offset))
        print("  time: YuNet {:.1f} ms, Face Mesh {:.1f} ms, two MiniFASNet {:.1f} ms".format(
            mean_ms(lambda: detector.detect(image)), mean_ms(lambda: mesh.landmarks(image, face.box)),
            mean_ms(lambda: real_score(sessions, image, square))))


if __name__ == "__main__":
    main()
