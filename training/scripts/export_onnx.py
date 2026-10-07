# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

"""Export the MiniFASNet classifiers of Silent-Face-Anti-Spoofing to ONNX.

The network definitions and the pretrained weights are read from a local clone of the
reference project, which is not part of this repository. For the Java engine, the script
writes:

- the two ONNX models and their manifest, ``models.json``;
- golden vectors for the parity tests: network inputs with their expected outputs, and
  synthetic images with the expected crop boxes and 80x80 patches.

No face image is written. The golden inputs are synthetic.

Usage::

    python training/scripts/export_onnx.py --silent-face-dir ../Silent-Face-Anti-Spoofing
"""

import argparse
import hashlib
import importlib
import json
import subprocess
import sys
from pathlib import Path

import numpy as np
import onnx
import onnxruntime as ort
import torch

REPO_ROOT = Path(__file__).resolve().parents[2]
MODELS_DIR = REPO_ROOT / "liveness-engine" / "src" / "main" / "resources" / "models"
GOLDEN_DIR = REPO_ROOT / "liveness-engine" / "src" / "test" / "resources" / "golden"
sys.path.insert(0, str(REPO_ROOT / "training"))

from liveness.manifest import update_manifest  # noqa: E402

OPSET = 13
INPUT_SIZE = 80
PYTORCH_TOLERANCE = 1e-4
SEED = 20261007
SOURCE_URL = "https://github.com/minivision-ai/Silent-Face-Anti-Spoofing"

MODEL_SPECS = [
    {"name": "minifasnet_v2", "factory": "MiniFASNetV2", "scale": 2.7,
     "source": "2.7_80x80_MiniFASNetV2.pth", "target": "minifasnet_v2_2.7_80x80.onnx"},
    {"name": "minifasnet_v1se", "factory": "MiniFASNetV1SE", "scale": 4.0,
     "source": "4_0_0_80x80_MiniFASNetV1SE.pth", "target": "minifasnet_v1se_4.0_80x80.onnx"},
]

# Face boxes [x, y, w, h] found by the reference detector on the sample images of the
# reference project. Used for a local sanity check only: nothing from these images is saved.
SAMPLE_FACE_BOXES = {
    "image_F1.jpg": [178, 136, 223, 225],
    "image_F2.jpg": [120, 252, 252, 256],
    "image_T1.jpg": [106, 147, 207, 213],
}

# Synthetic source images for the preprocessing vectors: name, width, height, face box, scales.
PREPROCESS_CASES = [
    ("portrait_large_face", 480, 640, [178, 136, 223, 225], [2.7, 4.0]),
    ("portrait_small_face", 480, 640, [200, 260, 90, 100], [2.7, 4.0]),
    ("portrait_top_left", 480, 640, [10, 20, 100, 110], [2.7]),
    ("portrait_bottom_right", 480, 640, [370, 520, 100, 110], [4.0]),
    ("landscape_center", 640, 480, [250, 150, 140, 160], [2.7, 4.0]),
    ("landscape_small_face", 640, 480, [300, 200, 60, 70], [2.7, 4.0]),
    ("hd_frame", 1280, 720, [560, 220, 180, 200], [2.7, 4.0]),
]

SYNTHETIC_FORMULA = ("v(x, y, c) = (7*x + 13*y + 31*c + 5*((x*y) mod 23) "
                     "+ 64*((floor(x/9) + floor(y/11)) mod 2)) mod 256")


def sha256(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def synthetic_image(width, height):
    """Deterministic image in OpenCV layout: array[y, x, c] with c = 0 (B), 1 (G), 2 (R)."""
    ys, xs = np.mgrid[0:height, 0:width].astype(np.int64)
    channels = [(7 * xs + 13 * ys + 31 * c + 5 * ((xs * ys) % 23) + 64 * ((xs // 9 + ys // 11) % 2)) % 256
                for c in range(3)]
    return np.stack(channels, axis=-1).astype(np.uint8)


def network_cases():
    """Synthetic network inputs as uint8 arrays of shape (3, 80, 80), channel order B, G, R."""
    rng = np.random.default_rng(SEED)
    ys, xs = np.mgrid[0:INPUT_SIZE, 0:INPUT_SIZE]
    gradient = np.stack([(2 * xs + ys + 60 * c) % 256 for c in range(3)]).astype(np.uint8)
    blocks = np.kron(rng.integers(0, 256, (3, 10, 10), dtype=np.uint8), np.ones((1, 8, 8), dtype=np.uint8))
    return {
        "noise": rng.integers(0, 256, (3, INPUT_SIZE, INPUT_SIZE), dtype=np.uint8),
        "gradient": gradient,
        "gray128": np.full((3, INPUT_SIZE, INPUT_SIZE), 128, dtype=np.uint8),
        "blocks": blocks,
    }


def to_tensor_input(chw_uint8):
    """float32, shape 1 x 3 x 80 x 80, values 0 to 255, as the reference code feeds the network."""
    return chw_uint8.astype(np.float32)[np.newaxis, ...]


def softmax(logits):
    shifted = logits - logits.max(axis=-1, keepdims=True)
    exp = np.exp(shifted)
    return exp / exp.sum(axis=-1, keepdims=True)


def load_reference(silent_face_dir):
    """Import the network definitions and the crop of the reference project, without writing into it."""
    sys.dont_write_bytecode = True
    sys.path.insert(0, str(silent_face_dir))
    # Resolved at run time: these modules live in the external clone, not in this repository.
    nets = importlib.import_module("src.model_lib.MiniFASNet")
    crop_image = importlib.import_module("src.generate_patches").CropImage
    get_kernel = importlib.import_module("src.utility").get_kernel
    return nets, crop_image(), get_kernel


def load_model(nets, get_kernel, weights_path, factory):
    model = getattr(nets, factory)(conv6_kernel=get_kernel(INPUT_SIZE, INPUT_SIZE))
    # Third-party file: load tensors only, never arbitrary pickled objects.
    state = torch.load(str(weights_path), map_location="cpu", weights_only=True)
    state = {key[7:] if key.startswith("module.") else key: value for key, value in state.items()}
    model.load_state_dict(state)
    model.eval()
    return model


def torch_logits(model, batch):
    with torch.no_grad():
        return model(torch.from_numpy(batch)).numpy()


def git_commit(directory):
    result = subprocess.run(["git", "-C", str(directory), "rev-parse", "HEAD"], capture_output=True, text=True)
    return result.stdout.strip() if result.returncode == 0 else "unknown"


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--silent-face-dir", required=True, type=Path,
                        help="local clone of Silent-Face-Anti-Spoofing")
    args = parser.parse_args()
    silent_face_dir = args.silent_face_dir.resolve()
    weights_dir = silent_face_dir / "resources" / "anti_spoof_models"

    nets, cropper, get_kernel = load_reference(silent_face_dir)
    MODELS_DIR.mkdir(parents=True, exist_ok=True)
    (GOLDEN_DIR / "network").mkdir(parents=True, exist_ok=True)
    (GOLDEN_DIR / "preprocess").mkdir(parents=True, exist_ok=True)

    cases = network_cases()
    rng = np.random.default_rng(SEED + 1)
    extra_inputs = [rng.integers(0, 256, (3, INPUT_SIZE, INPUT_SIZE), dtype=np.uint8) for _ in range(16)]

    models, sessions, manifest_models = {}, {}, []
    for spec in MODEL_SPECS:
        weights_path = weights_dir / spec["source"]
        target = MODELS_DIR / spec["target"]
        model = load_model(nets, get_kernel, weights_path, spec["factory"])
        dummy = torch.zeros(1, 3, INPUT_SIZE, INPUT_SIZE, dtype=torch.float32)
        torch.onnx.export(model, dummy, str(target), opset_version=OPSET, input_names=["input"],
                          output_names=["logits"], do_constant_folding=True, dynamo=False)
        onnx.checker.check_model(onnx.load(str(target)))
        session = ort.InferenceSession(str(target), providers=["CPUExecutionProvider"])

        worst = 0.0
        for chw in list(cases.values()) + extra_inputs:
            batch = to_tensor_input(chw)
            difference = np.abs(torch_logits(model, batch) - session.run(["logits"], {"input": batch})[0]).max()
            worst = max(worst, float(difference))
        print("{}: exported to {}, largest logit difference PyTorch vs ONNX Runtime = {:.3e}".format(
            spec["name"], target.name, worst))
        if worst >= PYTORCH_TOLERANCE:
            raise SystemExit("parity check failed for {}".format(spec["name"]))

        models[spec["name"]], sessions[spec["name"]] = model, session
        manifest_models.append({
            "name": spec["name"],
            "file": spec["target"],
            "sha256": sha256(target),
            "sizeBytes": target.stat().st_size,
            "role": "passive-liveness-classifier",
            "cropScale": spec["scale"],
            "opset": OPSET,
            "input": {"name": "input", "type": "float32", "shape": [1, 3, INPUT_SIZE, INPUT_SIZE],
                      "layout": "NCHW", "channelOrder": "BGR", "valueRange": [0, 255],
                      "normalization": "none"},
            "output": {"name": "logits", "type": "float32", "shape": [1, 3], "activation": "none, apply softmax",
                       "classes": ["fake", "real", "fake"], "realClassIndex": 1},
            "parity": {"reference": "PyTorch", "largestLogitDifference": worst,
                       "inputsChecked": len(cases) + len(extra_inputs)},
            "source": {"project": "Silent-Face-Anti-Spoofing", "url": SOURCE_URL,
                       "commit": git_commit(silent_face_dir),
                       "file": "resources/anti_spoof_models/" + spec["source"],
                       "sha256": sha256(weights_path)},
            "license": "Apache-2.0",
            "copyright": "Copyright 2020 Minivision",
            "changes": "Converted from PyTorch weights to ONNX. Weights unchanged.",
        })

    update_manifest(MODELS_DIR, manifest_models,
                    {"torch": torch.__version__, "onnx": onnx.__version__,
                     "onnxruntime": ort.__version__, "numpy": np.__version__})

    golden = {
        "description": "Golden vectors for the parity tests of the Java engine. All inputs are synthetic.",
        "modelSha256": {entry["name"]: entry["sha256"] for entry in manifest_models},
        "tolerances": {"logits": 1e-3, "patch": 0},
        "network": {
            "inputLayout": "uint8, 19200 bytes, order c, y, x with c = 0 (B), 1 (G), 2 (R). "
                           "Convert to float32 without scaling.",
            "expected": "logits and softmax computed with PyTorch",
            "cases": [],
        },
        "preprocess": {
            "syntheticImage": {"formula": SYNTHETIC_FORMULA,
                               "layout": "pixel (x, y), channel c = 0 (B), 1 (G), 2 (R)"},
            "patchLayout": "uint8, 19200 bytes, order y, x, c with c = 0 (B), 1 (G), 2 (R)",
            "expected": "crop box and patch computed with the reference crop, logits with PyTorch "
                        "on the model that uses this scale",
            "cases": [],
        },
    }

    for name, chw in cases.items():
        (GOLDEN_DIR / "network" / (name + ".bin")).write_bytes(chw.tobytes())
        outputs = {}
        for spec in MODEL_SPECS:
            logits = torch_logits(models[spec["name"]], to_tensor_input(chw))
            outputs[spec["name"]] = {"logits": [float(v) for v in logits[0]],
                                     "softmax": [float(v) for v in softmax(logits)[0]]}
        golden["network"]["cases"].append({"case": name, "file": "network/{}.bin".format(name), "outputs": outputs})

    model_by_scale = {spec["scale"]: spec["name"] for spec in MODEL_SPECS}
    for name, width, height, face_box, scales in PREPROCESS_CASES:
        image = synthetic_image(width, height)
        for scale in scales:
            crop_box = [int(v) for v in cropper._get_new_box(width, height, face_box, scale)]
            patch = cropper.crop(org_img=image, bbox=face_box, scale=scale, out_w=INPUT_SIZE, out_h=INPUT_SIZE)
            file_name = "preprocess/{}_scale{}.bin".format(name, scale)
            (GOLDEN_DIR / file_name).write_bytes(np.ascontiguousarray(patch).tobytes())
            model_name = model_by_scale[scale]
            logits = torch_logits(models[model_name], to_tensor_input(patch.transpose(2, 0, 1)))
            golden["preprocess"]["cases"].append({
                "case": name, "width": width, "height": height, "faceBox": face_box, "scale": scale,
                "cropBox": crop_box, "file": file_name, "model": model_name,
                "logits": [float(v) for v in logits[0]],
            })
    (GOLDEN_DIR / "golden.json").write_text(json.dumps(golden, indent=2) + "\n", encoding="utf-8")
    print("golden vectors: {} network cases, {} preprocessing cases".format(
        len(golden["network"]["cases"]), len(golden["preprocess"]["cases"])))

    check_reference_samples(silent_face_dir, cropper, sessions)


def check_reference_samples(silent_face_dir, cropper, sessions):
    """Run the exported models on the sample images of the reference project, when available."""
    import cv2

    sample_dir = silent_face_dir / "images" / "sample"
    for file_name, face_box in SAMPLE_FACE_BOXES.items():
        image = cv2.imread(str(sample_dir / file_name))
        if image is None:
            print("{}: not found, sanity check skipped".format(file_name))
            continue
        total = np.zeros(3)
        for spec in MODEL_SPECS:
            patch = cropper.crop(org_img=image, bbox=face_box, scale=spec["scale"],
                                 out_w=INPUT_SIZE, out_h=INPUT_SIZE)
            batch = to_tensor_input(patch.transpose(2, 0, 1))
            total += softmax(sessions[spec["name"]].run(["logits"], {"input": batch})[0])[0]
        label = int(np.argmax(total))
        print("{}: {} (class {}), score {:.6f}, mean probability of the real class {:.6f}".format(
            file_name, "real" if label == 1 else "fake", label, total[label] / 2, total[1] / 2))


if __name__ == "__main__":
    main()
