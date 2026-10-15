/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

/** Loads the ONNX models packaged under {@code /models/} in the JAR. No network access. */
public final class ModelResources {

    public static final String YUNET = "face_detection_yunet_2023mar.onnx";
    public static final String FACE_MESH = "face_mesh_v2_256.onnx";
    public static final String MINIFASNET_V2 = "minifasnet_v2_2.7_80x80.onnx";
    public static final String MINIFASNET_V1SE = "minifasnet_v1se_4.0_80x80.onnx";

    /** Threads used by each session. Kept low: the client runs on ordinary office machines. */
    private static final int INTRA_OP_THREADS = 2;

    private ModelResources() {
    }

    public static byte[] read(String file) {
        try (InputStream in = ModelResources.class.getResourceAsStream("/models/" + file)) {
            if (in == null) {
                throw new IllegalStateException("model not found in the JAR: " + file);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[64 * 1024];
            int n;
            while ((n = in.read(buffer)) > 0) {
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static OrtSession open(OrtEnvironment environment, String file) throws OrtException {
        return open(environment, read(file));
    }

    /** Session on model bytes already read, for example the bytes verified against models.json. */
    public static OrtSession open(OrtEnvironment environment, byte[] model) throws OrtException {
        try (OrtSession.SessionOptions options = new OrtSession.SessionOptions()) {
            options.setIntraOpNumThreads(INTRA_OP_THREADS);
            options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
            return environment.createSession(model, options);
        }
    }
}
