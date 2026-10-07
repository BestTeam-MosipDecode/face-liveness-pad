# Third-party notices

This file lists the third-party code, weights and models that this project reuses, with their source and licence. It is updated whenever an element is added to the repository.

| Element | Use in this project | Source | Licence | In this repository |
| --- | --- | --- | --- | --- |
| Silent-Face-Anti-Spoofing, preprocessing logic (`src/generate_patches.py`, `src/anti_spoof_predict.py`) | Described in `training/README.md`, to be re-implemented in Python and Java | <https://github.com/minivision-ai/Silent-Face-Anti-Spoofing>, commit `b6d5f04` | Apache License 2.0, Copyright 2020 Minivision | Description only |
| Silent-Face-Anti-Spoofing, pretrained weights `2.7_80x80_MiniFASNetV2.pth` and `4_0_0_80x80_MiniFASNetV1SE.pth` | Passive liveness classifier, after export to ONNX | Same repository, `resources/anti_spoof_models/` | Apache License 2.0, Copyright 2020 Minivision | Not yet |

The Apache License 2.0 is available at <https://www.apache.org/licenses/LICENSE-2.0>.
