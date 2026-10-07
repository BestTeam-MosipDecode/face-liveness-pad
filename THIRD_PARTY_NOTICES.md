# Third-party notices

This file lists the third-party code, weights and models that this project reuses, with their source and licence. It is updated whenever an element is added to the repository.

## Silent-Face-Anti-Spoofing

- Source: <https://github.com/minivision-ai/Silent-Face-Anti-Spoofing>, commit `b6d5f04ad78778917853b25c778acef6d5626d15`
- Licence: Apache License 2.0, Copyright 2020 Minivision
- Licence text: [liveness-engine/src/main/resources/models/LICENSE-Silent-Face-Anti-Spoofing.txt](liveness-engine/src/main/resources/models/LICENSE-Silent-Face-Anti-Spoofing.txt)

| Element in this repository | Origin | Changes |
| --- | --- | --- |
| `liveness-engine/src/main/resources/models/minifasnet_v2_2.7_80x80.onnx` | Pretrained weights `resources/anti_spoof_models/2.7_80x80_MiniFASNetV2.pth` | Converted from PyTorch to ONNX. Weights unchanged |
| `liveness-engine/src/main/resources/models/minifasnet_v1se_4.0_80x80.onnx` | Pretrained weights `resources/anti_spoof_models/4_0_0_80x80_MiniFASNetV1SE.pth` | Converted from PyTorch to ONNX. Weights unchanged |
| `training/liveness/crop.py` | Crop logic of `src/generate_patches.py` | Re-implemented as two functions |
| `training/README.md`, preprocessing specification | `src/generate_patches.py`, `src/anti_spoof_predict.py`, `src/data_io/functional.py`, `test.py` | Description of the algorithm |
| `liveness-engine/src/test/resources/golden/preprocess/*.bin` | Computed by running the crop of `src/generate_patches.py` on synthetic images | Output data only |

The checksums of the original weight files and of the converted models are recorded in `liveness-engine/src/main/resources/models/models.json`.

The network definitions of the reference project (`src/model_lib/MiniFASNet.py`) are not copied here. The export script imports them from a local clone.
