# Third-party notices

This file lists the third-party code, weights and models that this project reuses, with their source and licence. It is updated whenever an element is added to the repository.

The checksums of the original files and of the packaged models are recorded in `liveness-engine/src/main/resources/models/models.json`.

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

The network definitions of the reference project (`src/model_lib/MiniFASNet.py`) are not copied here. The export script imports them from a local clone.

## YuNet

- Source: OpenCV Zoo, `models/face_detection_yunet`, file `face_detection_yunet_2023mar.onnx`, downloaded from <https://huggingface.co/opencv/face_detection_yunet> at revision `3cc26e7f1014a5ee5d74a42acee58bafc9d0a310`
- Licence: MIT License, Copyright (c) 2020 Shiqi Yu
- Licence text: [liveness-engine/src/main/resources/models/LICENSE-YuNet.txt](liveness-engine/src/main/resources/models/LICENSE-YuNet.txt)

| Element in this repository | Origin | Changes |
| --- | --- | --- |
| `liveness-engine/src/main/resources/models/face_detection_yunet_2023mar.onnx` | `face_detection_yunet_2023mar.onnx` | None |
| `training/liveness/yunet.py`, output decoding | `FaceDetectorYN` of OpenCV, `modules/objdetect/src/face_detect.cpp` (Apache-2.0) | Re-implemented in Python |

## MediaPipe Face Mesh V2

- Source: MediaPipe Face Landmarker bundle, float16, version 1, <https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task>, member `face_landmarks_detector.tflite`
- Licence: Apache License 2.0, as stated in the [Face Mesh V2 model card](https://storage.googleapis.com/mediapipe-assets/Model%20Card%20MediaPipe%20Face%20Mesh%20V2.pdf); Copyright Google LLC
- Licence text: [liveness-engine/src/main/resources/models/LICENSE-MediaPipe.txt](liveness-engine/src/main/resources/models/LICENSE-MediaPipe.txt), from the MediaPipe repository

| Element in this repository | Origin | Changes |
| --- | --- | --- |
| `liveness-engine/src/main/resources/models/face_mesh_v2_256.onnx` | `face_landmarks_detector.tflite` | Converted from TFLite to ONNX with tf2onnx. Float16 weights stored as float32. Inputs, outputs and intermediate tensors renamed, batch size fixed to 1, output `Identity_2` removed |
| `training/liveness/face_mesh.py`, landmark indices and region of interest | MediaPipe face mesh topology and face landmarker graph | Indices and scale factor reused, region not rotated |

The other members of the bundle (`face_detector.tflite`, `face_blendshapes.tflite`, geometry metadata) are not used and not redistributed.

The Apache License 2.0 is available at <https://www.apache.org/licenses/LICENSE-2.0>. The MIT License text is in the file listed above.
