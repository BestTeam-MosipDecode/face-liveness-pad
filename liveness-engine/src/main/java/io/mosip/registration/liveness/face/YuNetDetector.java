/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.face;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import io.mosip.registration.liveness.ModelResources;
import io.mosip.registration.liveness.active.FaceBox;
import io.mosip.registration.liveness.image.BgrImage;
import io.mosip.registration.liveness.image.Resize;

/**
 * Face detection with YuNet (OpenCV Zoo, MIT licence), as in {@code training/liveness/yunet.py}. The image is resized
 * to fit in 640 x 640 keeping its aspect ratio and padded with zeros on the right and at the bottom; the outputs are
 * decoded as in OpenCV {@code FaceDetectorYN}, then filtered by non-maximum suppression.
 */
public final class YuNetDetector implements AutoCloseable {

    public static final int INPUT_SIZE = 640;
    public static final double SCORE_THRESHOLD = 0.9;
    public static final double NMS_THRESHOLD = 0.3;
    private static final int[] STRIDES = {8, 16, 32};

    private final OrtEnvironment environment;
    private final OrtSession session;
    private final double scoreThreshold;
    private final double nmsThreshold;

    public YuNetDetector(OrtEnvironment environment) throws OrtException {
        this(environment, SCORE_THRESHOLD, NMS_THRESHOLD);
    }

    public YuNetDetector(OrtEnvironment environment, double scoreThreshold, double nmsThreshold)
            throws OrtException {
        this.environment = environment;
        this.session = ModelResources.open(environment, ModelResources.YUNET);
        this.scoreThreshold = scoreThreshold;
        this.nmsThreshold = nmsThreshold;
    }

    /** Faces in {@code image}, best score first. */
    public List<FaceDetection> detect(BgrImage image) throws OrtException {
        double scale = Math.min((double) INPUT_SIZE / image.width(), (double) INPUT_SIZE / image.height());
        int newWidth = (int) Math.rint(image.width() * scale);
        int newHeight = (int) Math.rint(image.height() * scale);
        BgrImage resized = Resize.linear(image, newWidth, newHeight);

        int plane = INPUT_SIZE * INPUT_SIZE;
        float[] chw = new float[3 * plane];
        byte[] p = resized.pixels();
        for (int y = 0; y < newHeight; y++) {
            for (int x = 0; x < newWidth; x++) {
                int src = (y * newWidth + x) * 3;
                int dst = y * INPUT_SIZE + x;
                for (int c = 0; c < 3; c++) {
                    chw[c * plane + dst] = p[src + c] & 0xFF;
                }
            }
        }

        Map<String, float[]> outputs = new HashMap<>();
        long[] shape = {1, 3, INPUT_SIZE, INPUT_SIZE};
        try (OnnxTensor input = OnnxTensor.createTensor(environment, FloatBuffer.wrap(chw), shape);
                OrtSession.Result result = session.run(Collections.singletonMap("input", input))) {
            for (Map.Entry<String, ai.onnxruntime.OnnxValue> entry : result) {
                outputs.put(entry.getKey(), flatten((OnnxTensor) entry.getValue()));
            }
        }

        List<FaceDetection> kept = nms(decode(outputs, scoreThreshold), nmsThreshold);
        List<FaceDetection> faces = new ArrayList<>(kept.size());
        for (FaceDetection d : kept) {
            FaceBox b = d.box();
            double[][] k = new double[5][];
            for (int i = 0; i < 5; i++) {
                k[i] = new double[] {d.keypoints()[i][0] / scale, d.keypoints()[i][1] / scale};
            }
            faces.add(new FaceDetection(
                    new FaceBox(b.x() / scale, b.y() / scale, b.width() / scale, b.height() / scale), k, d.score()));
        }
        return faces;
    }

    private static float[] flatten(OnnxTensor tensor) {
        FloatBuffer buffer = tensor.getFloatBuffer();
        float[] values = new float[buffer.remaining()];
        buffer.get(values);
        return values;
    }

    /** Candidate faces in pixels of the 640 x 640 input, before non-maximum suppression. */
    static List<FaceDetection> decode(Map<String, float[]> outputs, double scoreThreshold) {
        List<FaceDetection> candidates = new ArrayList<>();
        for (int stride : STRIDES) {
            int cols = INPUT_SIZE / stride;
            float[] cls = outputs.get("cls_" + stride);
            float[] obj = outputs.get("obj_" + stride);
            float[] bbox = outputs.get("bbox_" + stride);
            float[] kps = outputs.get("kps_" + stride);
            for (int idx = 0; idx < cls.length; idx++) {
                double score = Math.sqrt(clip(cls[idx]) * clip(obj[idx]));
                if (score < scoreThreshold) {
                    continue;
                }
                int r = idx / cols;
                int c = idx % cols;
                double cx = (c + bbox[idx * 4]) * stride;
                double cy = (r + bbox[idx * 4 + 1]) * stride;
                double w = Math.exp(bbox[idx * 4 + 2]) * stride;
                double h = Math.exp(bbox[idx * 4 + 3]) * stride;
                double[][] keypoints = new double[5][];
                for (int n = 0; n < 5; n++) {
                    keypoints[n] = new double[] {(kps[idx * 10 + 2 * n] + c) * stride,
                        (kps[idx * 10 + 2 * n + 1] + r) * stride};
                }
                candidates.add(new FaceDetection(new FaceBox(cx - w / 2, cy - h / 2, w, h), keypoints, score));
            }
        }
        return candidates;
    }

    private static double clip(float v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    /** Greedy non-maximum suppression, highest score first. */
    static List<FaceDetection> nms(List<FaceDetection> candidates, double threshold) {
        List<FaceDetection> ordered = new ArrayList<>(candidates);
        ordered.sort(Comparator.comparingDouble(FaceDetection::score).reversed());
        List<FaceDetection> kept = new ArrayList<>();
        for (FaceDetection candidate : ordered) {
            boolean keep = true;
            for (FaceDetection other : kept) {
                if (candidate.box().iou(other.box()) > threshold) {
                    keep = false;
                    break;
                }
            }
            if (keep) {
                kept.add(candidate);
            }
        }
        return kept;
    }

    @Override
    public void close() throws OrtException {
        session.close();
    }
}
