# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

"""Check that ``liveness.minifas`` reproduces the reference outputs listed in ``training/README.md``.

The sample images and the boxes of the reference detector come from a local clone of
Silent-Face-Anti-Spoofing. The script checks the image checksums, then compares the softmax of
each model and the predicted class with the reference values. It exits with status 1 on any
difference above the tolerance.

Usage::

    python training/scripts/check_reference_outputs.py --silent-face-dir ../Silent-Face-Anti-Spoofing
"""

import argparse
import hashlib
import sys
from pathlib import Path

import cv2
import numpy as np

REPO_ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(REPO_ROOT / "training"))

from liveness.minifas import MiniFAS  # noqa: E402

TOLERANCE = 1e-4

# From training/README.md: image SHA-256, reference box [x, y, w, h], expected class, softmax of
# MiniFASNetV2 (2.7) and MiniFASNetV1SE (4.0) in class order 0, 1, 2.
REFERENCE = {
    "image_F1.jpg": ("4b11b5d7a8a8e4a88f5f16a5426a0a7692e39e5bb45bb03b4ebe5e1606336860", [178, 136, 223, 225], 2,
                     [0.083704, 0.001333, 0.914963], [0.308367, 0.143330, 0.548303]),
    "image_F2.jpg": ("fbbea73450ae9d9bb555c8ccac77bf39d234261fe3be4190e3ed2999690c485f", [120, 252, 252, 256], 2,
                     [0.000162, 0.000639, 0.999200], [0.002955, 0.361932, 0.635112]),
    "image_T1.jpg": ("f4455149f488f76205fdee5499ec5261d08ef6279a1cff7b778ea85405331e94", [106, 147, 207, 213], 1,
                     [0.000141, 0.990223, 0.009636], [0.000014, 0.996913, 0.003074]),
}


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--silent-face-dir", required=True, type=Path)
    args = parser.parse_args()

    classifier = MiniFAS()
    failures = 0
    for name, (sha256, box, expected_class, expected_v2, expected_v1se) in REFERENCE.items():
        path = args.silent_face_dir / "images" / "sample" / name
        if hashlib.sha256(path.read_bytes()).hexdigest() != sha256:
            print("{}: checksum differs from training/README.md".format(name))
            failures += 1
            continue
        image = cv2.imread(str(path))
        v2, v1se = classifier.probabilities(image, box)
        predicted = int(np.argmax(v2 + v1se))
        difference = max(np.abs(v2 - expected_v2).max(), np.abs(v1se - expected_v1se).max())
        ok = predicted == expected_class and difference < TOLERANCE
        failures += not ok
        print("{}: class {} (expected {}), real-class score {:.6f}, largest softmax difference {:.1e}  {}".format(
            name, predicted, expected_class, classifier.score(image, box), difference, "ok" if ok else "FAILED"))

    print("\n{} of {} images match the reference outputs".format(len(REFERENCE) - failures, len(REFERENCE)))
    sys.exit(1 if failures else 0)


if __name__ == "__main__":
    main()
