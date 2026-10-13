# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

"""Quality checks of one frame, as described in section 3.5 of ``docs/decision-flow.md``.

A frame that fails a check gives guidance and does not count for the passive score. The face
size and yaw limits are configuration keys (``quality.min_face_px``, ``quality.max_yaw_deg``).
The lighting and sharpness bounds are engine constants, provisional until the calibration on
the team captures (issue #7).

The Java engine reproduces this module.
"""

import math

import cv2

MIN_FACE_PX = 120
MAX_YAW_DEG = 20.0

# Provisional bounds, to calibrate on the team captures.
MIN_BRIGHTNESS = 60.0
MAX_BRIGHTNESS = 200.0
MIN_SHARPNESS = 40.0
SHARPNESS_SIZE = 128

# Depth of the nose tip in front of the eyes divided by the distance between the eyes (d / D in
# section 3.5). To calibrate.
NOSE_DEPTH_RATIO = 0.3

TIP_FRAME = "keep your face inside the frame"
TIP_LIGHT = "improve the lighting"
TIP_STILL = "hold still"
TIP_LOOK = "look directly at the camera"


def face_region(image, box):
    """Pixels of the face box, clipped to the image."""
    height, width = image.shape[:2]
    x, y, w, h = box
    x1, y1 = max(0, int(round(x))), max(0, int(round(y)))
    x2, y2 = min(width, int(round(x + w))), min(height, int(round(y + h)))
    return image[y1:y2, x1:x2]


def brightness(region):
    """Mean grey level of the face region, 0 to 255."""
    return float(cv2.cvtColor(region, cv2.COLOR_BGR2GRAY).mean())


def sharpness(region):
    """Variance of the Laplacian of the face region, resized to a fixed size so that it does not depend
    on the distance to the camera."""
    grey = cv2.cvtColor(cv2.resize(region, (SHARPNESS_SIZE, SHARPNESS_SIZE)), cv2.COLOR_BGR2GRAY)
    return float(cv2.Laplacian(grey, cv2.CV_64F).var())


def yaw_ratio(keypoints):
    """Horizontal position of the nose tip between the two eyes, from the YuNet keypoints: 0.5 when
    facing the camera."""
    right_eye, left_eye, nose = keypoints[0][0], keypoints[1][0], keypoints[2][0]
    return (nose - right_eye) / (left_eye - right_eye) if left_eye != right_eye else 0.5


def yaw_degrees(keypoints, depth_ratio=NOSE_DEPTH_RATIO):
    """Estimated yaw in degrees, from ``r - 0.5 = (d / D) * tan(yaw)``. Sign follows ``yaw_ratio``."""
    return math.degrees(math.atan((yaw_ratio(keypoints) - 0.5) / depth_ratio))


class QualityResult:
    """Measures of one frame and the first failed check, if any."""

    def __init__(self, face_px, inside, brightness, sharpness, yaw_deg, tip):
        self.face_px = face_px
        self.inside = inside
        self.brightness = brightness
        self.sharpness = sharpness
        self.yaw_deg = yaw_deg
        self.tip = tip

    @property
    def passed(self):
        return self.tip is None

    def __repr__(self):
        return ("QualityResult(face_px={:.0f}, inside={}, brightness={:.1f}, sharpness={:.1f}, yaw_deg={:.1f}, "
                "tip={!r})".format(self.face_px, self.inside, self.brightness, self.sharpness, self.yaw_deg,
                                   self.tip))


def check(image, detection, min_face_px=MIN_FACE_PX, max_yaw_deg=MAX_YAW_DEG, check_yaw=True):
    """Run the checks of section 3.5 on one detection. ``check_yaw`` is false during the active check."""
    height, width = image.shape[:2]
    x, y, w, h = detection.box
    face_px = min(w, h)
    inside = x >= 0 and y >= 0 and x + w <= width and y + h <= height
    region = face_region(image, detection.box)
    light = brightness(region) if region.size else 0.0
    sharp = sharpness(region) if region.size else 0.0
    yaw = yaw_degrees(detection.keypoints)

    if face_px < min_face_px or not inside:
        tip = TIP_FRAME
    elif not MIN_BRIGHTNESS <= light <= MAX_BRIGHTNESS:
        tip = TIP_LIGHT
    elif sharp < MIN_SHARPNESS:
        tip = TIP_STILL
    elif check_yaw and abs(yaw) > max_yaw_deg:
        tip = TIP_LOOK
    else:
        tip = None
    return QualityResult(face_px, inside, light, sharp, yaw, tip)
