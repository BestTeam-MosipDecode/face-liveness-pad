# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

"""Face detection with YuNet (OpenCV Zoo, ``face_detection_yunet_2023mar.onnx``, MIT licence).

The model has a fixed input of 1 x 3 x 640 x 640. The image is resized to fit in that square,
keeping its aspect ratio, and padded with zeros on the right and at the bottom. The decoding
follows ``FaceDetectorYN`` of OpenCV (``modules/objdetect/src/face_detect.cpp``).

The Java engine reproduces this module.
"""

import math

import cv2
import numpy as np
import onnxruntime as ort

INPUT_SIZE = 640
STRIDES = (8, 16, 32)
SCORE_THRESHOLD = 0.9
NMS_THRESHOLD = 0.3
TOP_K = 5000

# Order of the five keypoints returned by the model, as seen in the image: the "right eye" is
# the person's right eye, on the left side of a non-mirrored image.
KEYPOINTS = ("right_eye", "left_eye", "nose_tip", "right_mouth_corner", "left_mouth_corner")


class Detection:
    """One face: box ``[x, y, w, h]`` and keypoints in pixels of the original image, score in [0, 1]."""

    def __init__(self, box, keypoints, score):
        self.box = box
        self.keypoints = keypoints
        self.score = score

    def __repr__(self):
        return "Detection(box={}, score={:.4f})".format([round(v, 1) for v in self.box], self.score)


def letterbox(image):
    """Resize ``image`` (BGR) to fit in 640 x 640 and pad it. Returns the padded image and the scale."""
    height, width = image.shape[:2]
    scale = min(INPUT_SIZE / width, INPUT_SIZE / height)
    new_w, new_h = int(round(width * scale)), int(round(height * scale))
    resized = cv2.resize(image, (new_w, new_h))
    padded = np.zeros((INPUT_SIZE, INPUT_SIZE, 3), dtype=np.uint8)
    padded[:new_h, :new_w] = resized
    return padded, scale


def to_input_tensor(padded):
    """float32, 1 x 3 x 640 x 640, channel order B, G, R, values 0 to 255, no normalisation."""
    return padded.transpose(2, 0, 1)[np.newaxis, ...].astype(np.float32)


def decode(outputs, score_threshold=SCORE_THRESHOLD):
    """Turn the twelve raw outputs into candidate faces, in pixels of the 640 x 640 input.

    ``outputs`` maps output names (``cls_8`` ... ``kps_32``) to arrays.
    Returns a list of ``(box, keypoints, score)`` before non-maximum suppression.
    """
    candidates = []
    for stride in STRIDES:
        cols = INPUT_SIZE // stride
        cls = np.clip(outputs["cls_{}".format(stride)].reshape(-1), 0.0, 1.0)
        obj = np.clip(outputs["obj_{}".format(stride)].reshape(-1), 0.0, 1.0)
        bbox = outputs["bbox_{}".format(stride)].reshape(-1, 4)
        kps = outputs["kps_{}".format(stride)].reshape(-1, 10)
        scores = np.sqrt(cls * obj)
        # Anchor idx sits at row idx // cols and column idx % cols of the stride grid.
        for idx in np.flatnonzero(scores >= score_threshold):
            r, c = divmod(int(idx), cols)
            cx = (c + bbox[idx, 0]) * stride
            cy = (r + bbox[idx, 1]) * stride
            w = math.exp(bbox[idx, 2]) * stride
            h = math.exp(bbox[idx, 3]) * stride
            box = [float(cx - w / 2), float(cy - h / 2), float(w), float(h)]
            keypoints = [[float((kps[idx, 2 * n] + c) * stride), float((kps[idx, 2 * n + 1] + r) * stride)]
                         for n in range(5)]
            candidates.append((box, keypoints, float(scores[idx])))
    return candidates


def classifier_box(box):
    """Square box of the same area and centre, to feed the MiniFASNet crop.

    YuNet boxes are taller than wide, while the detector used to train MiniFASNet gives nearly
    square boxes. On the reference samples, the square box raises the overlap with the reference
    box from 0.70-0.77 to 0.78-0.86 and keeps the three decisions.
    """
    x, y, w, h = box
    side = math.sqrt(w * h)
    return [x + w / 2 - side / 2, y + h / 2 - side / 2, side, side]


def iou(a, b):
    """Intersection over union of two boxes ``[x, y, w, h]``."""
    x1, y1 = max(a[0], b[0]), max(a[1], b[1])
    x2, y2 = min(a[0] + a[2], b[0] + b[2]), min(a[1] + a[3], b[1] + b[3])
    inter = max(0.0, x2 - x1) * max(0.0, y2 - y1)
    union = a[2] * a[3] + b[2] * b[3] - inter
    return inter / union if union > 0 else 0.0


def nms(candidates, nms_threshold=NMS_THRESHOLD, top_k=TOP_K):
    """Greedy non-maximum suppression, highest score first."""
    ordered = sorted(candidates, key=lambda item: item[2], reverse=True)[:top_k]
    kept = []
    for candidate in ordered:
        if all(iou(candidate[0], other[0]) <= nms_threshold for other in kept):
            kept.append(candidate)
    return kept


class YuNet:
    def __init__(self, model_path, score_threshold=SCORE_THRESHOLD, nms_threshold=NMS_THRESHOLD):
        self.session = ort.InferenceSession(str(model_path), providers=["CPUExecutionProvider"])
        self.output_names = [output.name for output in self.session.get_outputs()]
        self.score_threshold = score_threshold
        self.nms_threshold = nms_threshold

    def detect(self, image):
        """Detect faces in ``image`` (BGR, OpenCV layout). Returns detections, best score first."""
        padded, scale = letterbox(image)
        raw = self.session.run(self.output_names, {"input": to_input_tensor(padded)})
        outputs = dict(zip(self.output_names, raw))
        kept = nms(decode(outputs, self.score_threshold), self.nms_threshold)
        return [Detection([v / scale for v in box], [[x / scale, y / scale] for x, y in keypoints], score)
                for box, keypoints, score in kept]
