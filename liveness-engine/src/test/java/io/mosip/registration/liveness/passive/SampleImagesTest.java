/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.passive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ai.onnxruntime.OrtEnvironment;
import io.mosip.registration.liveness.active.FaceMeasures;
import io.mosip.registration.liveness.face.FaceDetection;
import io.mosip.registration.liveness.face.FaceMesh;
import io.mosip.registration.liveness.face.YuNetDetector;
import io.mosip.registration.liveness.image.BgrImage;

/**
 * End-to-end check on the sample images of Silent-Face-Anti-Spoofing, against the Python results recorded in
 * {@code training/README.md}. The images contain faces, so they are not in the repository: the test runs only with
 * {@code -Dliveness.samples=<clone>/images/sample} and is skipped otherwise.
 *
 * <p>Decoding (ImageIO instead of libjpeg) and the letterbox resize of YuNet are not bit-exact with OpenCV, hence
 * the tolerances.
 */
class SampleImagesTest {

    @ParameterizedTest(name = "{0}")
    @CsvSource({
        "image_F1.jpg, 0.932, 0.028, 0.395, 0.284, -0.022",
        "image_F2.jpg, 0.932, 0.022, 0.376, 0.325, 0.414",
        "image_T1.jpg, 0.930, 0.996, 0.2135, 0.310, 0.751"})
    void matchesThePythonPipeline(String name, double yunetScore, double realScore, double eye, double mouth,
            double yaw) throws Exception {
        String dir = System.getProperty("liveness.samples");
        assumeTrue(dir != null, "liveness.samples not set");
        Path path = Paths.get(dir, name);
        assumeTrue(Files.exists(path), "missing " + path);

        BgrImage image = BgrImage.decode(Files.readAllBytes(path)).orElseThrow(AssertionError::new);
        OrtEnvironment environment = OrtEnvironment.getEnvironment();
        try (YuNetDetector detector = new YuNetDetector(environment);
                MiniFasClassifier classifier = new MiniFasClassifier(environment);
                FaceMesh mesh = new FaceMesh(environment)) {
            java.util.List<FaceDetection> faces = detector.detect(image);
            assertEquals(1, faces.size());
            FaceDetection face = faces.get(0);
            assertEquals(yunetScore, face.score(), 0.01, "YuNet score");

            double score = classifier.score(image, face.classifierBox());
            assertEquals(realScore, score, 0.05, "real-class score");
            assertTrue(realScore > 0.5 == score > 0.5, "decision");

            FaceMesh.Landmarks landmarks = mesh.landmarks(image, face.box());
            assertTrue(landmarks.presence() > 0.99, "presence");
            FaceMeasures m = landmarks.measures();
            assertEquals(eye, m.eye(), 0.02, "eye aspect ratio");
            assertEquals(mouth, m.mouth(), 0.02, "mouth width ratio");
            assertEquals(yaw, m.yaw(), 0.02, "yaw ratio");
        }
    }
}
