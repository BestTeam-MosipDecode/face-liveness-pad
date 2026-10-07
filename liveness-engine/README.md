# liveness-engine

Java module that decides whether the face in a frame stream belongs to a live person. The Registration Client calls it during resident face capture, operator authentication and supervisor authentication.

Status: Maven skeleton with the models and the parity test data. No Java source code yet.

## Models

`src/main/resources/models/` holds four ONNX models and `models.json`, their manifest: file, SHA-256, size, role, input and output contract, source and licence. The licence text of each source is stored next to the models.

| Model | Role | Source, licence | Size |
| --- | --- | --- | --- |
| `face_detection_yunet_2023mar.onnx` | Face detection, 5 keypoints | OpenCV Zoo, MIT | 0.23 MB |
| `face_mesh_v2_256.onnx` | 478 landmarks for the active check | MediaPipe, Apache-2.0 | 4.8 MB |
| `minifasnet_v2_2.7_80x80.onnx` | Passive classifier, crop scale 2.7 | Silent-Face-Anti-Spoofing, Apache-2.0 | 1.7 MB |
| `minifasnet_v1se_4.0_80x80.onnx` | Passive classifier, crop scale 4.0 | Silent-Face-Anti-Spoofing, Apache-2.0 | 1.7 MB |

The preprocessing and output decoding of each model are specified in `training/README.md`, with a Python reference in `training/liveness/`. Below, the classifier contract.

The input is a `float32` tensor of shape `1 x 3 x 80 x 80`, in B, G, R order, with values from 0 to 255 and no normalisation. The output is three logits. Class 1 is the real face.

`src/test/resources/golden/` holds the golden vectors for the parity tests. Its README describes the file formats. Models and vectors are produced by `training/scripts/export_onnx.py`.

## Design constraints

- **Java 11 bytecode.** Registration Client 1.2.0.2 runs on Java 11, so the module is compiled with `maven.compiler.release` set to 11.
- **No dependency on Registration Client code.** The module receives JPEG frames and a policy, and returns a state and a result. This keeps it reusable by another client.
- **Local processing only.** Models are packaged in the JAR under `src/main/resources/models/`. No network call is made.

## Build

```powershell
mvn clean verify
```

To make the module available to the Registration Client build:

```powershell
mvn install
```
