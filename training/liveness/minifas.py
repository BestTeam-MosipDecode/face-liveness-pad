# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

"""Passive classifier: the two MiniFASNet models of Silent-Face-Anti-Spoofing, in ONNX format.

Each model receives its own 80 x 80 crop (``liveness/crop.py``) and returns three logits. The
score of a face is the mean probability of class 1 (real face) over the two models, as stated
in section 3.6 of ``docs/decision-flow.md``. The engine computes it on the square box of the
same area as the YuNet box (``liveness.yunet.classifier_box``).

The Java engine runs the same ONNX files.
"""

import json
from pathlib import Path

import numpy as np
import onnxruntime as ort

from liveness.crop import crop_patch

MODELS_DIR = Path(__file__).resolve().parents[2] / "liveness-engine" / "src" / "main" / "resources" / "models"
CLASSIFIERS = ("minifasnet_v2", "minifasnet_v1se")
REAL_CLASS = 1


def load_manifest(models_dir=MODELS_DIR):
    """Entries of ``models.json``, by model name."""
    manifest = json.loads((Path(models_dir) / "models.json").read_text(encoding="utf-8"))
    return {model["name"]: model for model in manifest["models"]}


def softmax(logits):
    exp = np.exp(logits - logits.max())
    return exp / exp.sum()


class MiniFAS:
    def __init__(self, models_dir=MODELS_DIR):
        manifest = load_manifest(models_dir)
        self.models = [(manifest[name]["cropScale"],
                        ort.InferenceSession(str(Path(models_dir) / manifest[name]["file"]),
                                             providers=["CPUExecutionProvider"]))
                       for name in CLASSIFIERS]

    def probabilities(self, image, box):
        """Softmax vector of each model, in the order of ``CLASSIFIERS``, for a face box ``[x, y, w, h]``."""
        result = []
        for scale, session in self.models:
            patch = crop_patch(image, [int(v) for v in box], scale)
            tensor = patch.transpose(2, 0, 1)[np.newaxis].astype(np.float32)
            result.append(softmax(session.run(["logits"], {"input": tensor})[0][0]))
        return result

    def score(self, image, box):
        """Mean probability of the real class over the two models, between 0 and 1."""
        return float(np.mean([p[REAL_CLASS] for p in self.probabilities(image, box)]))
