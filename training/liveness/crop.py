# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

"""Face crop fed to the MiniFASNet classifiers.

Re-implementation of the crop of Silent-Face-Anti-Spoofing (``src/generate_patches.py``,
Apache-2.0, Copyright 2020 Minivision), as specified in ``training/README.md``.
"""

import cv2

PATCH_SIZE = 80


def crop_box(src_w, src_h, face_box, scale):
    """Return the crop box ``(x1, y1, x2, y2)``, both ends included.

    ``face_box`` is ``[x, y, w, h]`` in pixels. The scale is capped so that the crop fits
    in the image, then the box is shifted back inside the image when it overflows.
    """
    x, y, box_w, box_h = face_box
    scale = min((src_h - 1) / box_h, min((src_w - 1) / box_w, scale))

    new_w = box_w * scale
    new_h = box_h * scale
    center_x, center_y = box_w / 2 + x, box_h / 2 + y

    x1 = center_x - new_w / 2
    y1 = center_y - new_h / 2
    x2 = center_x + new_w / 2
    y2 = center_y + new_h / 2

    if x1 < 0:
        x2 -= x1
        x1 = 0
    if y1 < 0:
        y2 -= y1
        y1 = 0
    if x2 > src_w - 1:
        x1 -= x2 - src_w + 1
        x2 = src_w - 1
    if y2 > src_h - 1:
        y1 -= y2 - src_h + 1
        y2 = src_h - 1

    return int(x1), int(y1), int(x2), int(y2)


def crop_patch(image, face_box, scale, size=PATCH_SIZE):
    """Crop ``image`` (OpenCV layout, BGR) around the face and resize it to ``size`` x ``size``."""
    src_h, src_w = image.shape[:2]
    x1, y1, x2, y2 = crop_box(src_w, src_h, face_box, scale)
    return cv2.resize(image[y1:y2 + 1, x1:x2 + 1], (size, size))
