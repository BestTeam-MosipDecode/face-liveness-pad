# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

"""Check the ONNX models and the golden vectors committed in this repository.

Unlike ``export_onnx.py``, this script needs no clone of the reference project. It verifies:

- the checksum of each model against ``models.json``;
- the network cases, by running ONNX Runtime on each stored input;
- the preprocessing cases, by rebuilding each synthetic image, applying the crop of
  ``training/liveness/crop.py`` and comparing the patch byte for byte, then the logits.

Usage::

    python training/scripts/verify_golden.py
"""

import hashlib
import json
import sys
from pathlib import Path

import numpy as np
import onnxruntime as ort

REPO_ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(REPO_ROOT / "training"))

from liveness.crop import crop_box, crop_patch  # noqa: E402

MODELS_DIR = REPO_ROOT / "liveness-engine" / "src" / "main" / "resources" / "models"
GOLDEN_DIR = REPO_ROOT / "liveness-engine" / "src" / "test" / "resources" / "golden"


def synthetic_pixel(x, y, c):
    return (7 * x + 13 * y + 31 * c + 5 * ((x * y) % 23) + 64 * ((x // 9 + y // 11) % 2)) % 256


def synthetic_image(width, height):
    """Built pixel by pixel, on purpose: an independent reading of the documented formula."""
    image = np.empty((height, width, 3), dtype=np.uint8)
    for y in range(height):
        for x in range(width):
            for c in range(3):
                image[y, x, c] = synthetic_pixel(x, y, c)
    return image


def main():
    manifest = json.loads((MODELS_DIR / "models.json").read_text(encoding="utf-8"))
    golden = json.loads((GOLDEN_DIR / "golden.json").read_text(encoding="utf-8"))
    tolerance = golden["tolerances"]["logits"]
    failures = 0
    sessions = {}

    for model in manifest["models"]:
        path = MODELS_DIR / model["file"]
        actual = hashlib.sha256(path.read_bytes()).hexdigest()
        ok = actual == model["sha256"]
        failures += not ok
        print("{:<7} checksum of {}".format("ok" if ok else "FAILED", model["file"]))
        sessions[model["name"]] = ort.InferenceSession(str(path), providers=["CPUExecutionProvider"])

    def logits_of(name, chw_uint8):
        batch = chw_uint8.astype(np.float32)[np.newaxis, ...]
        return sessions[name].run(["logits"], {"input": batch})[0][0]

    worst = 0.0
    for case in golden["network"]["cases"]:
        chw = np.frombuffer((GOLDEN_DIR / case["file"]).read_bytes(), dtype=np.uint8).reshape(3, 80, 80)
        for name, expected in case["outputs"].items():
            worst = max(worst, float(np.abs(logits_of(name, chw) - np.array(expected["logits"])).max()))
    ok = worst < tolerance
    failures += not ok
    print("{:<7} {} network cases, largest logit difference {:.3e}".format(
        "ok" if ok else "FAILED", len(golden["network"]["cases"]), worst))

    images, worst, patch_mismatches, box_mismatches = {}, 0.0, 0, 0
    for case in golden["preprocess"]["cases"]:
        size = (case["width"], case["height"])
        if size not in images:
            images[size] = synthetic_image(*size)
        box = list(crop_box(case["width"], case["height"], case["faceBox"], case["scale"]))
        box_mismatches += box != case["cropBox"]
        patch = crop_patch(images[size], case["faceBox"], case["scale"])
        expected_patch = np.frombuffer((GOLDEN_DIR / case["file"]).read_bytes(), dtype=np.uint8).reshape(80, 80, 3)
        patch_mismatches += not np.array_equal(patch, expected_patch)
        difference = np.abs(logits_of(case["model"], patch.transpose(2, 0, 1)) - np.array(case["logits"])).max()
        worst = max(worst, float(difference))
    ok = box_mismatches == 0 and patch_mismatches == 0 and worst < tolerance
    failures += not ok
    print("{:<7} {} preprocessing cases, {} crop box mismatches, {} patch mismatches, "
          "largest logit difference {:.3e}".format("ok" if ok else "FAILED", len(golden["preprocess"]["cases"]),
                                                   box_mismatches, patch_mismatches, worst))

    if failures:
        raise SystemExit("{} check(s) failed".format(failures))
    print("all checks passed")


if __name__ == "__main__":
    main()
