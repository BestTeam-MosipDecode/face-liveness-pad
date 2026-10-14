/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.passive;

import java.nio.FloatBuffer;
import java.util.Collections;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import io.mosip.registration.liveness.ModelResources;
import io.mosip.registration.liveness.active.FaceBox;
import io.mosip.registration.liveness.image.BgrImage;

/**
 * Passive classifier: the two MiniFASNet models of Silent-Face-Anti-Spoofing. Each model receives its own 80 x 80
 * crop and returns three logits. The score of a face is the mean probability of class 1 (real face) over the two
 * models, as in {@code training/liveness/minifas.py}.
 */
public final class MiniFasClassifier implements AutoCloseable {

    public static final int REAL_CLASS = 1;
    static final double SCALE_V2 = 2.7;
    static final double SCALE_V1SE = 4.0;

    private final OrtEnvironment environment;
    private final OrtSession v2;
    private final OrtSession v1se;

    public MiniFasClassifier(OrtEnvironment environment) throws OrtException {
        this.environment = environment;
        this.v2 = ModelResources.open(environment, ModelResources.MINIFASNET_V2);
        this.v1se = ModelResources.open(environment, ModelResources.MINIFASNET_V1SE);
    }

    /** Real-class score of the face, between 0 and 1. {@code face} is the square classifier box. */
    public double score(BgrImage image, FaceBox face) throws OrtException {
        double[] a = softmax(logits(v2, ClassifierCrop.patch(image, face, SCALE_V2)));
        double[] b = softmax(logits(v1se, ClassifierCrop.patch(image, face, SCALE_V1SE)));
        return (a[REAL_CLASS] + b[REAL_CLASS]) / 2;
    }

    /** Logits of the 2.7 model ({@code v2 = true}) or of the 4.0 model for an 80 x 80 patch. */
    public float[] logits(boolean useV2, BgrImage patch) throws OrtException {
        return logits(useV2 ? v2 : v1se, patch);
    }

    /** Logits for a tensor already in the order channel, row, column, values 0 to 255. */
    public float[] logits(boolean useV2, float[] chw) throws OrtException {
        return run(useV2 ? v2 : v1se, chw);
    }

    private float[] logits(OrtSession session, BgrImage patch) throws OrtException {
        return run(session, toChw(patch));
    }

    private float[] run(OrtSession session, float[] chw) throws OrtException {
        long[] shape = {1, 3, ClassifierCrop.PATCH_SIZE, ClassifierCrop.PATCH_SIZE};
        try (OnnxTensor input = OnnxTensor.createTensor(environment, FloatBuffer.wrap(chw), shape);
                OrtSession.Result result = session.run(Collections.singletonMap("input", input))) {
            return ((float[][]) result.get(0).getValue())[0];
        }
    }

    /** float32 tensor 3 x H x W, channel order B, G, R, values 0 to 255, no normalisation. */
    static float[] toChw(BgrImage patch) {
        int plane = patch.width() * patch.height();
        float[] chw = new float[3 * plane];
        byte[] p = patch.pixels();
        for (int i = 0; i < plane; i++) {
            for (int c = 0; c < 3; c++) {
                chw[c * plane + i] = p[i * 3 + c] & 0xFF;
            }
        }
        return chw;
    }

    static double[] softmax(float[] logits) {
        double max = Double.NEGATIVE_INFINITY;
        for (float v : logits) {
            max = Math.max(max, v);
        }
        double sum = 0;
        double[] out = new double[logits.length];
        for (int i = 0; i < logits.length; i++) {
            out[i] = Math.exp(logits[i] - max);
            sum += out[i];
        }
        for (int i = 0; i < out.length; i++) {
            out[i] /= sum;
        }
        return out;
    }

    @Override
    public void close() throws OrtException {
        v2.close();
        v1se.close();
    }
}
