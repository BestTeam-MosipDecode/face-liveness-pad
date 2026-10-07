# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

"""Facial landmarks with MediaPipe Face Mesh V2 (478 points, Apache-2.0), converted to ONNX.

The face box comes from the detector. As in MediaPipe, the region of interest is a square
centred on the box, with a side of 1.5 times its longer edge (25 % margin on each side).
Unlike MediaPipe, the region is not rotated to level the eyes: the head roll has to stay small.

The Java engine reproduces this module.
"""

import cv2
import numpy as np
import onnxruntime as ort

INPUT_SIZE = 256
ROI_SCALE = 1.5

# Indices in the 478-point mesh. "Right" and "left" refer to the person.
RIGHT_EYE = (33, 160, 158, 133, 153, 144)   # outer corner, top, top, inner corner, bottom, bottom
LEFT_EYE = (263, 387, 385, 362, 380, 373)   # outer corner, top, top, inner corner, bottom, bottom
MOUTH_CORNERS = (61, 291)
INNER_LIPS = (13, 14)                        # upper, lower
FACE_SIDES = (234, 454)                      # right cheek edge, left cheek edge
NOSE_TIP = 1


def region_of_interest(box):
    """Square ``(x1, y1, side)`` in integer pixels around a face box ``[x, y, w, h]``."""
    x, y, w, h = box
    side = int(round(max(w, h) * ROI_SCALE))
    cx, cy = x + w / 2, y + h / 2
    return int(round(cx - side / 2)), int(round(cy - side / 2)), side


def crop_square(image, x1, y1, side):
    """Crop the square from ``image``, filling the part outside the image with black."""
    height, width = image.shape[:2]
    pad_left, pad_top = max(0, -x1), max(0, -y1)
    pad_right, pad_bottom = max(0, x1 + side - width), max(0, y1 + side - height)
    padded = cv2.copyMakeBorder(image, pad_top, pad_bottom, pad_left, pad_right, cv2.BORDER_CONSTANT, value=0)
    return padded[y1 + pad_top:y1 + pad_top + side, x1 + pad_left:x1 + pad_left + side]


def to_input_tensor(crop_bgr):
    """float32, 1 x 256 x 256 x 3 (NHWC), channel order R, G, B, values 0 to 1."""
    resized = cv2.resize(crop_bgr, (INPUT_SIZE, INPUT_SIZE))
    rgb = resized[:, :, ::-1].astype(np.float32) / 255.0
    return rgb[np.newaxis, ...]


def sigmoid(value):
    return 1.0 / (1.0 + np.exp(-value))


class FaceMesh:
    def __init__(self, model_path):
        self.session = ort.InferenceSession(str(model_path), providers=["CPUExecutionProvider"])

    def landmarks(self, image, box):
        """Return ``(points, presence)``: 478 x 3 points in pixels of ``image``, face presence in [0, 1].

        ``z`` is relative depth, in the same unit as ``x``.
        """
        x1, y1, side = region_of_interest(box)
        tensor = to_input_tensor(crop_square(image, x1, y1, side))
        points, flag = self.session.run(["landmarks", "face_flag"], {"input": tensor})
        points = points.reshape(-1, 3).astype(np.float64) * (side / INPUT_SIZE)
        points[:, 0] += x1
        points[:, 1] += y1
        return points, float(sigmoid(flag.reshape(-1)[0]))


def eye_aspect_ratio(points, eye):
    """Eye aspect ratio: mean eyelid opening divided by the eye width."""
    p1, p2, p3, p4, p5, p6 = (points[i, :2] for i in eye)
    return (np.linalg.norm(p2 - p6) + np.linalg.norm(p3 - p5)) / (2.0 * np.linalg.norm(p1 - p4))


def mouth_width_ratio(points):
    """Distance between the mouth corners divided by the face width."""
    return (np.linalg.norm(points[MOUTH_CORNERS[0], :2] - points[MOUTH_CORNERS[1], :2])
            / np.linalg.norm(points[FACE_SIDES[0], :2] - points[FACE_SIDES[1], :2]))


def yaw_ratio(points):
    """Horizontal position of the nose tip between the cheek edges: 0.5 when facing the camera."""
    right, left, nose = points[FACE_SIDES[0], 0], points[FACE_SIDES[1], 0], points[NOSE_TIP, 0]
    return float((nose - right) / (left - right))
