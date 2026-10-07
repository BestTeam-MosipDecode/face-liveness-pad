# training

Python side of the project: reference pipeline, evaluation, threshold calibration and ONNX export of the models used by the Java engine.

This first version documents the passive classifier taken as the starting point, and the exact preprocessing the Java engine has to reproduce.

## Reference implementation

| Item | Value |
| --- | --- |
| Project | Silent-Face-Anti-Spoofing, by Minivision |
| Source | <https://github.com/minivision-ai/Silent-Face-Anti-Spoofing> |
| Commit analysed | `b6d5f04ad78778917853b25c778acef6d5626d15` (5 August 2020) |
| Licence | Apache License 2.0, "Copyright 2020 Minivision". The repository has no `NOTICE` file |
| Reused here | the two pretrained classifiers and the preprocessing logic |

Apache-2.0 allows reuse and redistribution with attribution. Each reused element will be listed in `THIRD_PARTY_NOTICES.md` at the root of this repository.

## Models

| File | Architecture | Crop scale | Size (bytes) | SHA-256 |
| --- | --- | --- | --- | --- |
| `2.7_80x80_MiniFASNetV2.pth` | MiniFASNetV2 | 2.7 | 1 849 453 | `a5eb02e1843f19b5386b953cc4c9f011c3f985d0ee2bb9819eea9a142099bec0` |
| `4_0_0_80x80_MiniFASNetV1SE.pth` | MiniFASNetV1SE | 4.0 | 1 856 130 | `84ee1d37d96894d5e82de5a57df044ef80a58be2b218b5ed7cdfd875ec2f5990` |

The file name carries the configuration: crop scale, then input height and width, then architecture. `4_0_0` reads as 4.0.

Both networks are built with an embedding size of 128, three output classes and a final depthwise kernel of 5x5, computed as `((height + 15) // 16, (width + 15) // 16)` for an 80x80 input.

## Preprocessing specification

Verified by reading the code and by running it. The Java engine must follow these steps exactly.

1. **Decode** the image to 8 bits per channel, in BGR channel order, as OpenCV does.
2. **Face box**: `[x, y, w, h]` in pixels, from the face detector.
3. **Crop box**, for an image of size `src_w` x `src_h` and a model scale `s`:

   ```text
   s      = min((src_h - 1) / h, (src_w - 1) / w, s)
   new_w  = w * s
   new_h  = h * s
   cx, cy = x + w / 2, y + h / 2
   x1, y1 = cx - new_w / 2, cy - new_h / 2
   x2, y2 = cx + new_w / 2, cy + new_h / 2
   if x1 < 0:          x2 -= x1;               x1 = 0
   if y1 < 0:          y2 -= y1;               y1 = 0
   if x2 > src_w - 1:  x1 -= x2 - src_w + 1;   x2 = src_w - 1
   if y2 > src_h - 1:  y1 -= y2 - src_h + 1;   y2 = src_h - 1
   x1, y1, x2, y2 = int(x1), int(y1), int(x2), int(y2)
   ```

   `int()` truncates toward zero. The crop keeps rows `y1` to `y2` and columns `x1` to `x2`, both ends included.
4. **Resize** the crop to 80x80 with `cv2.resize` and its default interpolation, `INTER_LINEAR`. No anti-aliasing, even when the crop is much larger than 80 pixels.
5. **Tensor**: `float32`, shape `1 x 3 x 80 x 80`, channel order B, G, R. Values stay in the range 0 to 255. There is no division by 255 and no mean or standard deviation. The reference code has the division commented out in `src/data_io/functional.py`.
6. **Inference**: the network returns three logits. Apply a softmax over the three classes.
7. **Fusion**: add the softmax vectors of the two models. The predicted class is the index of the largest sum. Class 1 means real face. Classes 0 and 2 both mean fake face.

`test.py` prints the summed probability of the winning class divided by two. The engine will use the mean probability of class 1 as its liveness score, so that a single number can be compared with the pass and attack thresholds.

## Face detector of the reference implementation

The reference code detects the face with a RetinaFace model in Caffe format, loaded through `cv2.dnn`:

| File | Size (bytes) | SHA-256 |
| --- | --- | --- |
| `Widerface-RetinaFace.caffemodel` | 1 866 013 | `d08338a2c207df16a9c566f767fea67fb43ba6fff76ce11e938fe3fabefb9402` |
| `deploy.prototxt` | 43 286 | `5935df8926b4fd8f1be2d9425434e7eaf35d59c92401c72736a65081b3798d33` |

1. If the image has at least 192 x 192 pixels, resize it to `(int(192 * sqrt(r)), int(192 / sqrt(r)))` with `r = width / height`, bilinear.
2. Build the blob with `cv2.dnn.blobFromImage(img, 1, mean=(104, 117, 123))`: BGR order, no scaling.
3. Read the `detection_out` layer and keep the row with the highest confidence.
4. Scale the corners back to the original image size, then `box = [int(left), int(top), int(right - left + 1), int(bottom - top + 1)]`.

The origin of these detector weights is not documented in the repository beyond its own licence. The detector used by this project is chosen in a later step.

## Points to keep in mind

- **The scale is capped by the image size.** On the three sample images (480x640), the cap brings the scale down to between 1.9 and 2.3 for both models, so both receive the same crop. The two scales only differ when the face is small enough in the frame. Camera resolution and face distance therefore change what the 4.0 model sees.
- **No confidence threshold is applied.** `detector_confidence = 0.6` is defined but never used. The reference code returns its best box even when the image contains no face. The engine needs its own threshold and its own "no face" and "several faces" handling.
- **The 3:4 check belongs to the demo script.** `test.py` rejects images whose width to height ratio is not 3/4, to match the video stream of the Android demo application. The models do not require it.
- **Resize parity.** OpenCV's bilinear resize uses pixel centres and fixed-point arithmetic on 8-bit images. A Java implementation based on `Graphics2D` is not guaranteed to give the same pixels. The engine has to implement the same arithmetic, and the parity tests have to compare the 80x80 patches as well as the network outputs.
- **Timings in `test.py` include loading the weights**, because the model is reloaded at each prediction. They say nothing about inference time alone.

## Reference outputs

Obtained on 7 October 2026 with Python 3.10.8, PyTorch 2.14.0 (CPU), OpenCV 4.10.0 and NumPy 2.2.6, on the sample images shipped with the reference project. The original project pinned PyTorch 1.2.0.

| Image | Face box `[x, y, w, h]` | Crop box `[x1, y1, x2, y2]` | Result | Score |
| --- | --- | --- | --- | --- |
| `image_F1.jpg` | `[178, 136, 223, 225]` | `[0, 6, 479, 490]` | fake (class 2) | 0.731633 |
| `image_F2.jpg` | `[120, 252, 252, 256]` | `[0, 136, 479, 623]` | fake (class 2) | 0.817156 |
| `image_T1.jpg` | `[106, 147, 207, 213]` | `[0, 7, 479, 499]` | real (class 1) | 0.993568 |

Softmax outputs per model, in class order 0, 1, 2:

| Image | MiniFASNetV2 (2.7) | MiniFASNetV1SE (4.0) |
| --- | --- | --- |
| `image_F1.jpg` | 0.083704, 0.001333, 0.914963 | 0.308367, 0.143330, 0.548303 |
| `image_F2.jpg` | 0.000162, 0.000639, 0.999200 | 0.002955, 0.361932, 0.635112 |
| `image_T1.jpg` | 0.000141, 0.990223, 0.009636 | 0.000014, 0.996913, 0.003074 |

Sample images, identified by their SHA-256:

| Image | SHA-256 |
| --- | --- |
| `image_F1.jpg` | `4b11b5d7a8a8e4a88f5f16a5426a0a7692e39e5bb45bb03b4ebe5e1606336860` |
| `image_F2.jpg` | `fbbea73450ae9d9bb555c8ccac77bf39d234261fe3be4190e3ed2999690c485f` |
| `image_T1.jpg` | `f4455149f488f76205fdee5499ec5261d08ef6279a1cff7b778ea85405331e94` |

These images are not copied into this repository. They stay in the local clone of the reference project.

## Environment

```powershell
python -m venv training\.venv
training\.venv\Scripts\python.exe -m pip install -r training\requirements.txt
```

Python 3.10. The virtual environment is ignored by Git.

## ONNX export

`scripts/export_onnx.py` converts the two classifiers to ONNX for the Java engine. It reads the network definitions and the weights from a local clone of the reference project, given on the command line:

```powershell
training\.venv\Scripts\python.exe training\scripts\export_onnx.py --silent-face-dir ..\Silent-Face-Anti-Spoofing
```

It writes:

| Output | Location |
| --- | --- |
| `minifasnet_v2_2.7_80x80.onnx` and `minifasnet_v1se_4.0_80x80.onnx` | `liveness-engine/src/main/resources/models/` |
| `models.json`: checksums, input and output contract, source and licence of each model | same folder |
| Golden vectors for the Java parity tests | `liveness-engine/src/test/resources/golden/` |

The export uses opset 13 and the TorchScript-based exporter (`dynamo=False`). Input `input` is `float32`, `1 x 3 x 80 x 80`. Output `logits` is `float32`, `1 x 3`, before softmax. The script stops with an error if the largest logit difference between PyTorch and ONNX Runtime reaches 1e-4.

Results of the export of 7 October 2026 (PyTorch 2.14.0, onnx 1.23.2, ONNX Runtime 1.23.2):

| Model | Size (bytes) | Largest logit difference with PyTorch, 20 inputs |
| --- | --- | --- |
| `minifasnet_v2_2.7_80x80.onnx` | 1 743 495 | 1.3e-05 |
| `minifasnet_v1se_4.0_80x80.onnx` | 1 742 663 | 6.9e-06 |

Run twice, the export produces identical files. On the three sample images, the ONNX models give the scores of the reference outputs above (0.731634, 0.817156, 0.993568).

## Checking the committed models and vectors

`scripts/verify_golden.py` needs no clone of the reference project. It checks the model checksums, runs the network cases through ONNX Runtime, rebuilds the synthetic images, applies the crop of `liveness/crop.py` and compares each 80x80 patch byte for byte with the expected one.

```powershell
training\.venv\Scripts\python.exe training\scripts\verify_golden.py
```

Result on 7 October 2026: 4 network cases and 12 preprocessing cases pass, with no crop box or patch mismatch.

The golden inputs are synthetic. No face image is stored in the repository.
