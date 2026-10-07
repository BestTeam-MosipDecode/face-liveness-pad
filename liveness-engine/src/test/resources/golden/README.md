# Golden vectors

Reference data for the parity tests of the Java engine. Everything here is produced by `training/scripts/export_onnx.py`. Do not edit by hand: run the script again.

All inputs are synthetic. No file in this folder comes from a face image.

## Index

`golden.json` lists every case with its expected values and gives the tolerances: 1e-3 on logits, exact equality on patches.

## Network cases

`network/<case>.bin` holds one network input: 19 200 bytes, unsigned, in the order channel, row, column, with channels B, G, R. Convert each byte to `float32` without scaling to build the `1 x 3 x 80 x 80` tensor.

For each case and each model, `golden.json` gives the logits and the softmax computed with PyTorch.

## Preprocessing cases

The source images are not stored. Each one is rebuilt from its width and height with this formula, where `x` is the column, `y` the row and `c` the channel (0 for B, 1 for G, 2 for R):

```text
v(x, y, c) = (7*x + 13*y + 31*c + 5*((x*y) mod 23) + 64*((floor(x/9) + floor(y/11)) mod 2)) mod 256
```

For each case, `golden.json` gives the face box `[x, y, w, h]`, the model scale, the expected crop box `[x1, y1, x2, y2]` with both ends included, and the logits of the model that uses this scale.

`preprocess/<case>_scale<scale>.bin` holds the expected 80x80 patch: 19 200 bytes, unsigned, in the order row, column, channel, with channels B, G, R. It was computed with the crop of the reference project, which resizes with OpenCV `INTER_LINEAR`.

A parity test builds the synthetic image, applies the engine's crop and resize, compares the patch byte for byte, then compares the network output.
