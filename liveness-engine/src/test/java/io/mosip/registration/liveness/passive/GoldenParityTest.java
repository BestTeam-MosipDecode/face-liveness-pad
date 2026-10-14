/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.passive;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.onnxruntime.OrtEnvironment;
import io.mosip.registration.liveness.active.FaceBox;
import io.mosip.registration.liveness.image.BgrImage;

/**
 * Parity with the Python reference, from the golden vectors of {@code src/test/resources/golden/}: crop box and
 * 80 x 80 patch byte for byte, logits within the tolerance of {@code golden.json}.
 */
class GoldenParityTest {

    private static JsonNode golden;
    private static MiniFasClassifier classifier;

    @BeforeAll
    static void open() throws Exception {
        load();
        classifier = new MiniFasClassifier(OrtEnvironment.getEnvironment());
    }

    static void load() throws IOException {
        if (golden == null) {
            golden = new ObjectMapper().readTree(resource("golden.json"));
        }
    }

    @AfterAll
    static void close() throws Exception {
        classifier.close();
    }

    static byte[] resource(String name) throws IOException {
        try (InputStream in = GoldenParityTest.class.getResourceAsStream("/golden/" + name)) {
            return in.readAllBytes();
        }
    }

    static Stream<Arguments> networkCases() throws Exception {
        load();
        List<Arguments> cases = new ArrayList<>();
        for (JsonNode c : golden.path("network").path("cases")) {
            cases.add(Arguments.of(c.path("case").asText(), c));
        }
        return cases.stream();
    }

    static Stream<Arguments> preprocessCases() throws Exception {
        load();
        List<Arguments> cases = new ArrayList<>();
        for (JsonNode c : golden.path("preprocess").path("cases")) {
            cases.add(Arguments.of(c.path("file").asText(), c));
        }
        return cases.stream();
    }

    /** The synthetic image of {@code golden/README.md}. */
    static BgrImage synthetic(int width, int height) {
        byte[] p = new byte[width * height * 3];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (int c = 0; c < 3; c++) {
                    int v = (7 * x + 13 * y + 31 * c + 5 * ((x * y) % 23) + 64 * ((x / 9 + y / 11) % 2)) % 256;
                    p[(y * width + x) * 3 + c] = (byte) v;
                }
            }
        }
        return new BgrImage(width, height, p);
    }

    static void assertLogits(JsonNode expected, float[] actual) {
        double tolerance = golden.path("tolerances").path("logits").asDouble();
        for (int i = 0; i < 3; i++) {
            assertEquals(expected.get(i).asDouble(), actual[i], tolerance, "logit " + i);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("networkCases")
    void networkCase(String name, JsonNode c) throws Exception {
        byte[] bytes = resource(c.path("file").asText());
        float[] chw = new float[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            chw[i] = bytes[i] & 0xFF;
        }
        assertLogits(c.path("outputs").path("minifasnet_v2").path("logits"), classifier.logits(true, chw));
        assertLogits(c.path("outputs").path("minifasnet_v1se").path("logits"), classifier.logits(false, chw));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("preprocessCases")
    void preprocessCase(String file, JsonNode c) throws Exception {
        int width = c.path("width").asInt();
        int height = c.path("height").asInt();
        JsonNode b = c.path("faceBox");
        FaceBox face = new FaceBox(b.get(0).asDouble(), b.get(1).asDouble(), b.get(2).asDouble(), b.get(3).asDouble());
        double scale = c.path("scale").asDouble();

        int[] expectedBox = new int[4];
        for (int i = 0; i < 4; i++) {
            expectedBox[i] = c.path("cropBox").get(i).asInt();
        }
        BgrImage image = synthetic(width, height);
        assertArrayEquals(expectedBox, ClassifierCrop.box(width, height, face, scale), "crop box");

        BgrImage patch = ClassifierCrop.patch(image, face, scale);
        assertArrayEquals(resource(file), patch.pixels(), "patch");

        boolean v2 = "minifasnet_v2".equals(c.path("model").asText());
        assertLogits(c.path("logits"), classifier.logits(v2, patch));
    }
}
