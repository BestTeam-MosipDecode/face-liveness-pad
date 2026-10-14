/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.passive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.mosip.registration.liveness.active.FaceBox;
import io.mosip.registration.liveness.face.FaceDetection;
import io.mosip.registration.liveness.image.BgrImage;
import io.mosip.registration.liveness.passive.PassiveWindow.Decision;
import io.mosip.registration.liveness.passive.QualityChecker.Tip;

class QualityAndWindowTest {

    static final double[][] FRONTAL = {{90, 90}, {150, 92}, {120, 130}, {95, 170}, {150, 170}};

    static FaceDetection face(double x, double y, double w, double h, double[][] keypoints) {
        return new FaceDetection(new FaceBox(x, y, w, h), keypoints, 0.95);
    }

    static BgrImage uniform(int width, int height, int value) {
        byte[] p = new byte[width * height * 3];
        java.util.Arrays.fill(p, (byte) value);
        return new BgrImage(width, height, p);
    }

    /** Values computed by {@code training/liveness/quality.py} with OpenCV 4.10 on the same synthetic image. */
    @Test
    void measuresMatchThePythonReference() {
        BgrImage image = GoldenParityTest.synthetic(320, 240);
        double[][] keypoints = {{90, 90}, {150, 92}, {125, 130}, {95, 170}, {150, 170}};
        QualityResult result = new QualityChecker().check(image, face(40.4, 30.6, 150.2, 170.5, keypoints), true);
        assertEquals(127.4791195948578, result.brightness(), 1e-9);
        assertEquals(16562.660034179688, result.sharpness(), 1e-6);
        assertEquals(15.524110996754263, result.yawDeg(), 1e-9);
        assertEquals(150.2, result.facePx(), 1e-12);
        assertTrue(result.inside());
        assertTrue(result.passed());
    }

    @Test
    void greyConversionMatchesOpenCv() {
        BgrImage image = GoldenParityTest.synthetic(320, 240);
        int[] grey = QualityChecker.grey(image);
        assertEquals(227, grey[17 * 320 + 23]);
        assertEquals(127.76248697916667, QualityChecker.brightness(image), 1e-9);
    }

    @Test
    void smallFaceAsksToStayInTheFrame() {
        BgrImage image = GoldenParityTest.synthetic(320, 240);
        QualityResult result = new QualityChecker().check(image, face(40, 30, 100, 100, FRONTAL), true);
        assertEquals(Tip.STAY_IN_FRAME, result.tip().get());
        assertFalse(result.passed());
    }

    @Test
    void faceOutsideTheImageAsksToStayInTheFrame() {
        BgrImage image = GoldenParityTest.synthetic(320, 240);
        assertEquals(Tip.STAY_IN_FRAME, new QualityChecker().check(image, face(200, 50, 150, 150, FRONTAL), true)
                .tip().get());
    }

    @Test
    void darkOrFlatFacesFail() {
        assertEquals(Tip.LIGHTING, new QualityChecker().check(uniform(320, 240, 20), face(40, 30, 150, 170, FRONTAL),
                true).tip().get());
        assertEquals(Tip.HOLD_STILL, new QualityChecker().check(uniform(320, 240, 128),
                face(40, 30, 150, 170, FRONTAL), true).tip().get());
    }

    @Test
    void yawLimitOnlyInThePassiveCheck() {
        BgrImage image = GoldenParityTest.synthetic(320, 240);
        double[][] turned = {{90, 90}, {150, 92}, {148, 130}, {95, 170}, {150, 170}};
        assertEquals(Tip.LOOK_CAMERA, new QualityChecker().check(image, face(40, 30, 150, 170, turned), true)
                .tip().get());
        assertTrue(new QualityChecker().check(image, face(40, 30, 150, 170, turned), false).passed());
    }

    @Test
    void yawOfCoincidentEyesIsZero() {
        assertEquals(0.0, QualityChecker.yawDegrees(new double[][] {{1, 1}, {1, 1}, {5, 5}}), 1e-12);
    }

    @Test
    void classifierBoxIsTheSquareOfTheSameArea() {
        FaceBox square = face(10, 20, 100, 144, FRONTAL).classifierBox();
        assertEquals(120, square.width(), 1e-9);
        assertEquals(120, square.height(), 1e-9);
        assertEquals(0, square.x(), 1e-9);
        assertEquals(32, square.y(), 1e-9);
    }

    @Test
    void windowDecisions() {
        PassiveWindow window = new PassiveWindow();
        for (int i = 0; i < 4; i++) {
            window.add(i * 100, 0.9);
            assertEquals(Decision.PENDING, window.decision());
        }
        assertEquals(0.8, window.progress(), 1e-12);
        window.add(400, 0.1);
        assertEquals(Decision.PASSED, window.decision());
        assertEquals(1.0, window.progress(), 1e-12);
        window.clear();
        for (int i = 0; i < 5; i++) {
            window.add(1000 + i * 100, 0.2);
        }
        assertEquals(Decision.ATTACK, window.decision());
        window.clear();
        for (int i = 0; i < 6; i++) {
            window.add(2000 + i * 100, i % 2 == 0 ? 0.5 : 0.7);
        }
        assertEquals(0.6, window.aggregate().getAsDouble(), 1e-12);
        assertEquals(Decision.CHALLENGE, window.decision());
    }

    @Test
    void oldFramesLeaveTheWindow() {
        PassiveWindow window = new PassiveWindow();
        for (int i = 0; i < 5; i++) {
            window.add(i * 100, 0.1);
        }
        window.add(10_000, 0.9);
        assertEquals(1, window.size());
        assertEquals(Decision.PENDING, window.decision());
    }

    @Test
    void thresholdsMustBeOrdered() {
        assertThrows(IllegalArgumentException.class, () -> new PassiveWindow(0.3, 0.5, 5, 3000));
    }

    @Test
    void frameWithoutFaceGivesANoFaceObservation() throws Exception {
        try (FrameAnalyzer analyzer = new FrameAnalyzer(ai.onnxruntime.OrtEnvironment.getEnvironment(),
                new QualityChecker())) {
            FrameAnalyzer.FrameAnalysis analysis = analyzer.analyze(uniform(320, 240, 128), 1234, true);
            assertTrue(analysis.faces().isEmpty());
            assertNull(analysis.quality());
            assertNull(analysis.score());
            assertNull(analysis.measures());
            assertEquals(1234, analysis.timestampMs());
            assertEquals(0, analysis.toObservation().faceCount());
        }
    }

    @Test
    void emptyFaceRegion() {
        assertNull(QualityChecker.faceRegion(uniform(50, 50, 100), new FaceBox(60, 60, 10, 10)));
    }
}
