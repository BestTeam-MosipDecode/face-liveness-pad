# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

"""Package the face detector and the landmark model for the Java engine.

- YuNet (OpenCV Zoo, MIT): copied as published, after checking its SHA-256.
- MediaPipe Face Mesh V2 (Google, Apache-2.0): ``face_landmarks_detector.tflite`` is read from
  the ``face_landmarker.task`` bundle, converted to ONNX with tf2onnx, then compared with the
  TFLite interpreter.

The downloaded files are passed on the command line and checked against the SHA-256 pinned below.
The script writes into ``liveness-engine/src/main/resources/models/`` and updates ``models.json``.

Needs the conversion environment (``training/requirements-convert.txt``)::

    training\\.venv-convert\\Scripts\\python.exe training\\scripts\\prepare_face_models.py ^
        --yunet training\\downloads\\yunet\\face_detection_yunet_2023mar.onnx ^
        --yunet-license training\\downloads\\yunet\\LICENSE ^
        --face-landmarker training\\downloads\\mediapipe\\face_landmarker.task ^
        --mediapipe-license training\\downloads\\licenses\\mediapipe-LICENSE
"""

import argparse
import hashlib
import shutil
import sys
import tempfile
import zipfile
from pathlib import Path

import numpy as np
import onnx
import onnxruntime as ort
import tensorflow as tf
import tf2onnx

REPO_ROOT = Path(__file__).resolve().parents[2]
MODELS_DIR = REPO_ROOT / "liveness-engine" / "src" / "main" / "resources" / "models"
sys.path.insert(0, str(REPO_ROOT / "training"))

from liveness.manifest import update_manifest  # noqa: E402

OPSET = 13
SEED = 20261008

YUNET = {
    "url": "https://huggingface.co/opencv/face_detection_yunet/resolve/"
           "3cc26e7f1014a5ee5d74a42acee58bafc9d0a310/face_detection_yunet_2023mar.onnx",
    "sha256": "8f2383e4dd3cfbb4553ea8718107fc0423210dc964f9f4280604804ed2552fa4",
    "license_sha256": "c83b8120c50ccbd4c4f96edf53141bdd566ebb8f8e9227e415326aa1b1aba958",
    "target": "face_detection_yunet_2023mar.onnx",
    "license_target": "LICENSE-YuNet.txt",
}
FACE_MESH = {
    "url": "https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/"
           "face_landmarker.task",
    "sha256": "64184e229b263107bc2b804c6625db1341ff2bb731874b0bcc2fe6544e0bc9ff",
    "member": "face_landmarks_detector.tflite",
    "license_url": "https://raw.githubusercontent.com/google-ai-edge/mediapipe/master/LICENSE",
    "license_sha256": "8707eef0533987efc5b155d64761eeb6e20793f50b9bd1a68dad1cf4719d0ed8",
    "target": "face_mesh_v2_256.onnx",
    "license_target": "LICENSE-MediaPipe.txt",
}

# Largest accepted differences between the TFLite interpreter and ONNX Runtime.
LANDMARK_TOLERANCE = 1e-2   # pixels of the 256 x 256 input
FLAG_TOLERANCE = 1e-3       # logit


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def read_checked(path, expected, what):
    data = Path(path).read_bytes()
    actual = sha256(data)
    if actual != expected:
        raise SystemExit("{}: SHA-256 {} does not match the pinned value {}".format(what, actual, expected))
    return data


def package_yunet(model_path, license_path):
    data = read_checked(model_path, YUNET["sha256"], "YuNet model")
    read_checked(license_path, YUNET["license_sha256"], "YuNet licence")
    target = MODELS_DIR / YUNET["target"]
    target.write_bytes(data)
    shutil.copyfile(license_path, MODELS_DIR / YUNET["license_target"])
    model = onnx.load(str(target))
    onnx.checker.check_model(model)
    session = ort.InferenceSession(str(target), providers=["CPUExecutionProvider"])
    outputs = [output.name for output in session.get_outputs()]
    print("yunet: copied, {} outputs, opset {}".format(len(outputs), model.opset_import[0].version))
    return {
        "name": "yunet",
        "file": YUNET["target"],
        "sha256": sha256(data),
        "sizeBytes": len(data),
        "role": "face-detector",
        "opset": model.opset_import[0].version,
        "input": {"name": "input", "type": "float32", "shape": [1, 3, 640, 640], "layout": "NCHW",
                  "channelOrder": "BGR", "valueRange": [0, 255], "normalization": "none",
                  "preprocessing": "resize to fit 640 x 640 keeping the aspect ratio, bilinear, "
                                   "pad right and bottom with zeros"},
        "output": {"names": outputs,
                   "decoding": "see training/liveness/yunet.py (FaceDetectorYN of OpenCV)",
                   "keypoints": ["right_eye", "left_eye", "nose_tip", "right_mouth_corner",
                                 "left_mouth_corner"]},
        "source": {"project": "OpenCV Zoo, face_detection_yunet", "url": YUNET["url"],
                   "sha256": sha256(data)},
        "license": "MIT",
        "copyright": "Copyright (c) 2020 Shiqi Yu",
        "changes": "None. Published file copied as is.",
    }


def rename_io(model, renames):
    """Rename graph inputs and outputs, and every reference to them."""
    graph = model.graph
    for value in list(graph.input) + list(graph.output):
        if value.name in renames:
            value.name = renames[value.name]
    for node in graph.node:
        node.input[:] = [renames.get(name, name) for name in node.input]
        node.output[:] = [renames.get(name, name) for name in node.output]


def canonicalize(model):
    """Make the converted graph identical from one run to the next.

    tf2onnx names intermediate tensors and symbolic dimensions from a global counter whose value
    varies between runs. Tensors are renamed in order of first use, initializers are sorted in
    that order, and the batch dimension is fixed to 1, as in the TFLite model.
    """
    graph = model.graph
    keep = {value.name for value in list(graph.input) + list(graph.output)}
    renames = {}

    def canonical(name):
        if name and name not in keep and name not in renames:
            renames[name] = "t{:04d}".format(len(renames))
        return renames.get(name, name)

    for index, node in enumerate(graph.node):
        node.input[:] = [canonical(name) for name in node.input]
        node.output[:] = [canonical(name) for name in node.output]
        node.name = "n{:04d}_{}".format(index, node.op_type)
    for tensor in graph.initializer:
        tensor.name = canonical(tensor.name)
    ordered = sorted(graph.initializer, key=lambda tensor: tensor.name)
    del graph.initializer[:]
    graph.initializer.extend(ordered)
    del graph.value_info[:]
    for value in list(graph.input) + list(graph.output):
        dims = value.type.tensor_type.shape.dim
        if dims and dims[0].dim_param:
            dims[0].dim_value = 1


def package_face_mesh(task_path, license_path, work_dir):
    bundle = read_checked(task_path, FACE_MESH["sha256"], "Face Landmarker bundle")
    read_checked(license_path, FACE_MESH["license_sha256"], "MediaPipe licence")
    tflite_path = Path(work_dir) / FACE_MESH["member"]
    with zipfile.ZipFile(task_path) as archive:
        # Read the member by name: nothing else from the archive reaches the disk.
        tflite_path.write_bytes(archive.read(FACE_MESH["member"]))
    tflite_bytes = tflite_path.read_bytes()

    interpreter = tf.lite.Interpreter(model_path=str(tflite_path))
    interpreter.allocate_tensors()
    tfl_input = interpreter.get_input_details()[0]
    tfl_outputs = interpreter.get_output_details()
    print("face mesh tflite input: {} {}".format(tfl_input["name"], list(tfl_input["shape"])))
    for output in tfl_outputs:
        print("face mesh tflite output: {} {}".format(output["name"], list(output["shape"])))

    # Two outputs hold a single value. The face presence logit is the 1 x 1 x 1 x 1 one ("Identity_1"):
    # strongly positive on face crops, negative on noise and on uniform images. The 1 x 1 output
    # ("Identity_2") is something else and is not used.
    landmarks_out = next(output for output in tfl_outputs if int(np.prod(output["shape"])) == 478 * 3)
    flag_out = next(output for output in tfl_outputs if list(output["shape"]) == [1, 1, 1, 1])
    renames = {tfl_input["name"]: "input", landmarks_out["name"]: "landmarks", flag_out["name"]: "face_flag"}

    model, _ = tf2onnx.convert.from_tflite(str(tflite_path), opset=OPSET)
    rename_io(model, renames)
    kept = {"landmarks", "face_flag"}
    for output in [output for output in model.graph.output if output.name not in kept]:
        model.graph.output.remove(output)
    canonicalize(model)
    onnx.checker.check_model(model, full_check=True)
    target = MODELS_DIR / FACE_MESH["target"]
    onnx.save(model, str(target))
    session = ort.InferenceSession(str(target), providers=["CPUExecutionProvider"])

    rng = np.random.default_rng(SEED)
    inputs = [rng.random(tfl_input["shape"], dtype=np.float32) for _ in range(16)]
    inputs += [np.full(tfl_input["shape"], value, dtype=np.float32) for value in (0.0, 0.5, 1.0)]
    worst_points, worst_flag = 0.0, 0.0
    for tensor in inputs:
        interpreter.set_tensor(tfl_input["index"], tensor)
        interpreter.invoke()
        expected_points = interpreter.get_tensor(landmarks_out["index"]).reshape(-1)
        expected_flag = interpreter.get_tensor(flag_out["index"]).reshape(-1)
        points, flag = session.run(["landmarks", "face_flag"], {"input": tensor})
        worst_points = max(worst_points, float(np.abs(points.reshape(-1) - expected_points).max()))
        worst_flag = max(worst_flag, float(np.abs(flag.reshape(-1) - expected_flag).max()))
    print("face mesh: largest difference TFLite vs ONNX Runtime over {} inputs: "
          "landmarks {:.3e} px, face flag {:.3e}".format(len(inputs), worst_points, worst_flag))
    if worst_points >= LANDMARK_TOLERANCE or worst_flag >= FLAG_TOLERANCE:
        raise SystemExit("parity check failed for the face mesh model")

    shutil.copyfile(license_path, MODELS_DIR / FACE_MESH["license_target"])
    data = target.read_bytes()
    return {
        "name": "face_mesh_v2",
        "file": FACE_MESH["target"],
        "sha256": sha256(data),
        "sizeBytes": len(data),
        "role": "face-landmarks",
        "opset": OPSET,
        "input": {"name": "input", "type": "float32", "shape": [int(v) for v in tfl_input["shape"]],
                  "layout": "NHWC", "channelOrder": "RGB", "valueRange": [0, 1],
                  "normalization": "divide by 255",
                  "preprocessing": "square region centred on the face box, side 1.5 times its longer edge, "
                                   "outside of the image filled with black, bilinear resize to 256 x 256"},
        "output": {"landmarks": {"shape": [int(v) for v in landmarks_out["shape"]],
                                 "content": "478 points x, y, z in pixels of the 256 x 256 input"},
                   "face_flag": {"shape": [int(v) for v in flag_out["shape"]],
                                 "content": "face presence logit, apply a sigmoid"}},
        "parity": {"reference": "TFLite interpreter", "largestLandmarkDifference": worst_points,
                   "largestFlagDifference": worst_flag, "inputsChecked": len(inputs)},
        "source": {"project": "MediaPipe Face Landmarker, float16, version 1", "url": FACE_MESH["url"],
                   "sha256": FACE_MESH["sha256"], "member": FACE_MESH["member"],
                   "memberSha256": sha256(tflite_bytes)},
        "license": "Apache-2.0",
        "copyright": "Copyright Google LLC",
        "changes": "Converted from TFLite to ONNX with tf2onnx. Float16 weights stored as float32. "
                   "Inputs, outputs and intermediate tensors renamed, batch size fixed to 1. "
                   "Output Identity_2 removed.",
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--yunet", required=True, type=Path)
    parser.add_argument("--yunet-license", required=True, type=Path)
    parser.add_argument("--face-landmarker", required=True, type=Path)
    parser.add_argument("--mediapipe-license", required=True, type=Path)
    args = parser.parse_args()

    entries = [package_yunet(args.yunet, args.yunet_license)]
    with tempfile.TemporaryDirectory() as work_dir:
        entries.append(package_face_mesh(args.face_landmarker, args.mediapipe_license, work_dir))
    update_manifest(MODELS_DIR, entries, {"tensorflow": tf.__version__, "tf2onnx": tf2onnx.__version__,
                                          "onnx": onnx.__version__, "onnxruntime": ort.__version__,
                                          "numpy": np.__version__})
    print("models.json updated")


if __name__ == "__main__":
    main()
