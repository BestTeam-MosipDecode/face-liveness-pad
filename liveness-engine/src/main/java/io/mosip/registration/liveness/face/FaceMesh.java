/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.face;

import java.nio.FloatBuffer;
import java.util.Collections;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import io.mosip.registration.liveness.ModelResources;
import io.mosip.registration.liveness.active.FaceBox;
import io.mosip.registration.liveness.active.FaceMeasures;
import io.mosip.registration.liveness.image.BgrImage;
import io.mosip.registration.liveness.image.Resize;

/**
 * Facial landmarks with MediaPipe Face Mesh V2 (478 points, Apache-2.0), converted to ONNX, as in
 * {@code training/liveness/face_mesh.py}. The region of interest is a square centred on the face box, with a side of
 * 1.5 times its longer edge, not rotated: the head roll has to stay small.
 */
public final class FaceMesh implements AutoCloseable {

    public static final int INPUT_SIZE = 256;
    static final double ROI_SCALE = 1.5;

    // Indices in the 478-point mesh. "Right" and "left" refer to the person.
    static final int[] RIGHT_EYE = {33, 160, 158, 133, 153, 144};
    static final int[] LEFT_EYE = {263, 387, 385, 362, 380, 373};
    static final int[] MOUTH_CORNERS = {61, 291};
    static final int[] FACE_SIDES = {234, 454};
    static final int NOSE_TIP = 1;

    /** Landmarks in pixels of the source image, and the face presence probability. */
    public static final class Landmarks {
        private final double[][] points;
        private final double presence;

        Landmarks(double[][] points, double presence) {
            this.points = points;
            this.presence = presence;
        }

        /** {@code points()[i]} is {@code {x, y, z}} of landmark {@code i}; {@code z} is a relative depth. */
        public double[][] points() {
            return points;
        }

        public double presence() {
            return presence;
        }

        /** Mean eye aspect ratio of both eyes, mouth width ratio and yaw ratio. */
        public FaceMeasures measures() {
            double eye = (eyeAspectRatio(points, RIGHT_EYE) + eyeAspectRatio(points, LEFT_EYE)) / 2;
            return new FaceMeasures(eye, mouthWidthRatio(points), yawRatio(points));
        }
    }

    private final OrtEnvironment environment;
    private final OrtSession session;

    public FaceMesh(OrtEnvironment environment) throws OrtException {
        this(environment, ModelResources.read(ModelResources.FACE_MESH));
    }

    /** Landmark model on bytes already verified by the engine. */
    public FaceMesh(OrtEnvironment environment, byte[] model) throws OrtException {
        this.environment = environment;
        this.session = ModelResources.open(environment, model);
    }

    public Landmarks landmarks(BgrImage image, FaceBox box) throws OrtException {
        int side = (int) Math.rint(Math.max(box.width(), box.height()) * ROI_SCALE);
        double cx = box.x() + box.width() / 2;
        double cy = box.y() + box.height() / 2;
        int x1 = (int) Math.rint(cx - side / 2.0);
        int y1 = (int) Math.rint(cy - side / 2.0);
        BgrImage roi = Resize.linear(cropSquare(image, x1, y1, side), INPUT_SIZE, INPUT_SIZE);

        float[] nhwc = new float[INPUT_SIZE * INPUT_SIZE * 3];
        byte[] p = roi.pixels();
        for (int i = 0; i < INPUT_SIZE * INPUT_SIZE; i++) {
            nhwc[i * 3] = (p[i * 3 + 2] & 0xFF) / 255.0f;
            nhwc[i * 3 + 1] = (p[i * 3 + 1] & 0xFF) / 255.0f;
            nhwc[i * 3 + 2] = (p[i * 3] & 0xFF) / 255.0f;
        }

        long[] shape = {1, INPUT_SIZE, INPUT_SIZE, 3};
        try (OnnxTensor input = OnnxTensor.createTensor(environment, FloatBuffer.wrap(nhwc), shape);
                OrtSession.Result result = session.run(Collections.singletonMap("input", input))) {
            FloatBuffer raw = ((OnnxTensor) result.get("landmarks").get()).getFloatBuffer();
            FloatBuffer flag = ((OnnxTensor) result.get("face_flag").get()).getFloatBuffer();
            double factor = (double) side / INPUT_SIZE;
            double[][] points = new double[raw.remaining() / 3][];
            for (int i = 0; i < points.length; i++) {
                points[i] = new double[] {raw.get() * factor + x1, raw.get() * factor + y1, raw.get() * factor};
            }
            return new Landmarks(points, 1.0 / (1.0 + Math.exp(-flag.get(0))));
        }
    }

    /** The square region, black where it falls outside the image. */
    static BgrImage cropSquare(BgrImage image, int x1, int y1, int side) {
        byte[] out = new byte[side * side * 3];
        byte[] p = image.pixels();
        int fromX = Math.max(0, x1);
        int toX = Math.min(image.width(), x1 + side);
        if (toX > fromX) {
            for (int y = Math.max(0, y1); y < Math.min(image.height(), y1 + side); y++) {
                System.arraycopy(p, (y * image.width() + fromX) * 3, out, ((y - y1) * side + fromX - x1) * 3,
                        (toX - fromX) * 3);
            }
        }
        return new BgrImage(side, side, out);
    }

    static double distance(double[] a, double[] b) {
        return Math.hypot(a[0] - b[0], a[1] - b[1]);
    }

    /** Mean eyelid opening divided by the eye width. */
    static double eyeAspectRatio(double[][] points, int[] eye) {
        double[] p1 = points[eye[0]];
        double[] p2 = points[eye[1]];
        double[] p3 = points[eye[2]];
        double[] p4 = points[eye[3]];
        double[] p5 = points[eye[4]];
        double[] p6 = points[eye[5]];
        return (distance(p2, p6) + distance(p3, p5)) / (2.0 * distance(p1, p4));
    }

    /** Distance between the mouth corners divided by the face width. */
    static double mouthWidthRatio(double[][] points) {
        return distance(points[MOUTH_CORNERS[0]], points[MOUTH_CORNERS[1]])
                / distance(points[FACE_SIDES[0]], points[FACE_SIDES[1]]);
    }

    /** Horizontal position of the nose tip between the cheek edges: 0.5 when facing the camera. */
    static double yawRatio(double[][] points) {
        double right = points[FACE_SIDES[0]][0];
        double left = points[FACE_SIDES[1]][0];
        return (points[NOSE_TIP][0] - right) / (left - right);
    }

    @Override
    public void close() throws OrtException {
        session.close();
    }
}
