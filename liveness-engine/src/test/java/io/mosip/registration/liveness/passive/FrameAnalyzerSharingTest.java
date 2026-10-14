/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.passive;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import ai.onnxruntime.OrtEnvironment;
import io.mosip.registration.liveness.face.FaceMesh;
import io.mosip.registration.liveness.face.YuNetDetector;
import io.mosip.registration.liveness.image.BgrImage;

class FrameAnalyzerSharingTest {

    @Test
    void anAnalyserOnSharedModelsLeavesThemOpen() throws Exception {
        byte[] grey = new byte[320 * 240 * 3];
        Arrays.fill(grey, (byte) 128);
        BgrImage image = new BgrImage(320, 240, grey);
        OrtEnvironment environment = OrtEnvironment.getEnvironment();
        try (YuNetDetector detector = new YuNetDetector(environment);
                MiniFasClassifier classifier = new MiniFasClassifier(environment);
                FaceMesh mesh = new FaceMesh(environment)) {
            FrameAnalyzer analyzer = new FrameAnalyzer(detector, classifier, mesh, new QualityChecker());
            assertTrue(analyzer.analyze(image, 0, false).faces().isEmpty());
            analyzer.close();
            assertTrue(detector.detect(image).isEmpty(), "the detector must still work after close()");
        }
    }
}
