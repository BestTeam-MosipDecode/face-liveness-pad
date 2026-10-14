/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.face;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ai.onnxruntime.OrtEnvironment;
import io.mosip.registration.liveness.active.FaceBox;
import io.mosip.registration.liveness.active.FaceMeasures;
import io.mosip.registration.liveness.image.BgrImage;

class FaceModelsTest {

    static Map<String, float[]> emptyOutputs() {
        Map<String, float[]> outputs = new HashMap<>();
        for (int stride : new int[] {8, 16, 32}) {
            int n = (640 / stride) * (640 / stride);
            outputs.put("cls_" + stride, new float[n]);
            outputs.put("obj_" + stride, new float[n]);
            outputs.put("bbox_" + stride, new float[n * 4]);
            outputs.put("kps_" + stride, new float[n * 10]);
        }
        return outputs;
    }

    @Test
    void decodeOneAnchor() {
        Map<String, float[]> outputs = emptyOutputs();
        int idx = 3 * 40 + 5;
        outputs.get("cls_16")[idx] = 0.95f;
        outputs.get("obj_16")[idx] = 1.2f;
        float[] bbox = outputs.get("bbox_16");
        bbox[idx * 4] = 0.5f;
        bbox[idx * 4 + 1] = 0.25f;
        bbox[idx * 4 + 2] = (float) Math.log(4);
        bbox[idx * 4 + 3] = (float) Math.log(5);
        outputs.get("kps_16")[idx * 10] = 0.1f;
        outputs.get("kps_16")[idx * 10 + 1] = 0.2f;

        List<FaceDetection> faces = YuNetDetector.decode(outputs, 0.9);
        assertEquals(1, faces.size());
        FaceDetection face = faces.get(0);
        assertEquals(Math.sqrt(0.95f), face.score(), 1e-6);
        assertEquals((5 + 0.5) * 16 - 32, face.box().x(), 1e-4);
        assertEquals((3 + 0.25) * 16 - 40, face.box().y(), 1e-4);
        assertEquals(64, face.box().width(), 1e-4);
        assertEquals(80, face.box().height(), 1e-4);
        assertEquals((0.1 + 5) * 16, face.keypoints()[0][0], 1e-4);
        assertEquals((0.2 + 3) * 16, face.keypoints()[0][1], 1e-4);
    }

    @Test
    void decodeSkipsLowScores() {
        Map<String, float[]> outputs = emptyOutputs();
        outputs.get("cls_8")[0] = 0.8f;
        outputs.get("obj_8")[0] = 0.8f;
        assertTrue(YuNetDetector.decode(outputs, 0.9).isEmpty());
    }

    @Test
    void nmsKeepsTheBestOfOverlappingBoxes() {
        double[][] k = new double[5][2];
        FaceDetection a = new FaceDetection(new FaceBox(0, 0, 100, 100), k, 0.92);
        FaceDetection b = new FaceDetection(new FaceBox(5, 5, 100, 100), k, 0.97);
        FaceDetection c = new FaceDetection(new FaceBox(300, 300, 100, 100), k, 0.91);
        List<FaceDetection> kept = YuNetDetector.nms(Arrays.asList(a, b, c), 0.3);
        assertEquals(Arrays.asList(b, c), kept);
    }

    @Test
    void noFaceInASyntheticPattern() throws Exception {
        byte[] p = new byte[640 * 480 * 3];
        for (int i = 0; i < p.length; i++) {
            p[i] = (byte) (i * 31 % 251);
        }
        try (YuNetDetector detector = new YuNetDetector(OrtEnvironment.getEnvironment())) {
            assertTrue(detector.detect(new BgrImage(640, 480, p)).isEmpty());
        }
    }

    @Test
    void faceMeshRunsAndReportsNoFaceOnAUniformImage() throws Exception {
        byte[] p = new byte[320 * 240 * 3];
        Arrays.fill(p, (byte) 128);
        try (FaceMesh mesh = new FaceMesh(OrtEnvironment.getEnvironment())) {
            FaceMesh.Landmarks landmarks = mesh.landmarks(new BgrImage(320, 240, p), new FaceBox(100, 60, 120, 140));
            assertEquals(478, landmarks.points().length);
            assertTrue(landmarks.presence() < 0.5, "presence " + landmarks.presence());
        }
    }

    @Test
    void cropSquarePadsWithBlack() {
        byte[] p = new byte[4 * 4 * 3];
        Arrays.fill(p, (byte) 200);
        BgrImage square = FaceMesh.cropSquare(new BgrImage(4, 4, p), -2, -2, 4);
        assertEquals(0, square.get(0, 0, 0));
        assertEquals(0, square.get(3, 1, 2));
        assertEquals(200, square.get(2, 2, 1));
        assertEquals(200, square.get(3, 3, 0));
        BgrImage outside = FaceMesh.cropSquare(new BgrImage(4, 4, p), 10, 10, 3);
        assertEquals(0, outside.get(1, 1, 1));
    }

    @Test
    void measuresFromLandmarks() {
        double[][] points = new double[478][];
        for (int i = 0; i < points.length; i++) {
            points[i] = new double[] {0, 0, 0};
        }
        // Right eye: width 10, openings 3 and 3.
        int[] eye = FaceMesh.RIGHT_EYE;
        points[eye[0]] = new double[] {0, 0, 0};
        points[eye[3]] = new double[] {10, 0, 0};
        points[eye[1]] = new double[] {3, -1.5, 0};
        points[eye[5]] = new double[] {3, 1.5, 0};
        points[eye[2]] = new double[] {7, -1.5, 0};
        points[eye[4]] = new double[] {7, 1.5, 0};
        int[] left = FaceMesh.LEFT_EYE;
        points[left[0]] = new double[] {40, 0, 0};
        points[left[3]] = new double[] {30, 0, 0};
        points[left[1]] = new double[] {37, -1, 0};
        points[left[5]] = new double[] {37, 1, 0};
        points[left[2]] = new double[] {33, -1, 0};
        points[left[4]] = new double[] {33, 1, 0};
        points[FaceMesh.MOUTH_CORNERS[0]] = new double[] {10, 30, 0};
        points[FaceMesh.MOUTH_CORNERS[1]] = new double[] {30, 30, 0};
        points[FaceMesh.FACE_SIDES[0]] = new double[] {-10, 10, 0};
        points[FaceMesh.FACE_SIDES[1]] = new double[] {50, 10, 0};
        points[FaceMesh.NOSE_TIP] = new double[] {26, 15, 0};

        FaceMeasures m = new FaceMesh.Landmarks(points, 1).measures();
        assertEquals((0.3 + 0.2) / 2, m.eye(), 1e-12);
        assertEquals(20.0 / 60, m.mouth(), 1e-12);
        assertEquals(36.0 / 60, m.yaw(), 1e-12);
    }
}
