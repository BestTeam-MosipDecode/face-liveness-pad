/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import io.mosip.registration.liveness.ModelResources;
import io.mosip.registration.liveness.api.ModelIntegrityException;

class ModelManifestTest {

    static byte[] resource(String file) {
        try (InputStream in = ModelManifestTest.class.getResourceAsStream("/models/" + file)) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The real files, with {@code file} replaced by {@code change} applied to its bytes. */
    static Function<String, byte[]> changed(String file, Function<byte[], byte[]> change) {
        return name -> name.equals(file) ? change.apply(resource(name)) : resource(name);
    }

    @Test
    void acceptsThePackagedModels() throws Exception {
        ModelManifest manifest = ModelManifest.load();
        assertEquals("1.1.0", manifest.modelSetVersion());
        assertEquals(Arrays.asList(ModelResources.YUNET, ModelResources.FACE_MESH, ModelResources.MINIFASNET_V2,
            ModelResources.MINIFASNET_V1SE), manifest.files());
    }

    @Test
    void refusesAModifiedModel() {
        ModelIntegrityException e = assertThrows(ModelIntegrityException.class,
            () -> ModelManifest.load(changed(ModelResources.MINIFASNET_V2, bytes -> {
                byte[] copy = bytes.clone();
                copy[copy.length / 2] ^= 1;
                return copy;
            })));
        assertTrue(e.getMessage().contains(ModelResources.MINIFASNET_V2), e.getMessage());
        assertTrue(e.getMessage().contains("SHA-256"), e.getMessage());
    }

    @Test
    void refusesATruncatedModel() {
        ModelIntegrityException e = assertThrows(ModelIntegrityException.class,
            () -> ModelManifest.load(changed(ModelResources.YUNET, bytes -> Arrays.copyOf(bytes, 100))));
        assertTrue(e.getMessage().contains("100 bytes"), e.getMessage());
    }

    @Test
    void refusesAMissingModel() {
        ModelIntegrityException e = assertThrows(ModelIntegrityException.class,
            () -> ModelManifest.load(changed(ModelResources.FACE_MESH, bytes -> null)));
        assertTrue(e.getMessage().contains("missing: " + ModelResources.FACE_MESH), e.getMessage());
    }

    @Test
    void refusesAMissingOrInvalidManifest() {
        assertThrows(ModelIntegrityException.class, () -> ModelManifest.load(name -> null));
        assertThrows(ModelIntegrityException.class,
            () -> ModelManifest.load(name -> "{ not json".getBytes(StandardCharsets.UTF_8)));
        assertThrows(ModelIntegrityException.class,
            () -> ModelManifest.load(name -> "{\"modelSetVersion\": \"1\", \"models\": []}"
                .getBytes(StandardCharsets.UTF_8)));
        assertThrows(ModelIntegrityException.class,
            () -> ModelManifest.load(name -> "{\"modelSetVersion\": \"1\", \"models\": [{\"file\": 3}]}"
                .getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void sha256IsHexadecimal() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            ModelManifest.sha256(new byte[0]));
    }

    @Test
    void refusesAManifestWithoutARequiredModel() {
        byte[] abc = "abc".getBytes(StandardCharsets.UTF_8);
        String manifest = "{\"modelSetVersion\": \"1\", \"models\": [{\"file\": \"a.onnx\", \"sha256\": \""
            + ModelManifest.sha256(abc) + "\", \"sizeBytes\": 3}]}";
        ModelIntegrityException e = assertThrows(ModelIntegrityException.class,
            () -> ModelManifest.load(name -> name.equals("a.onnx") ? abc : manifest.getBytes(StandardCharsets.UTF_8)));
        assertTrue(e.getMessage().contains(ModelResources.YUNET), e.getMessage());
    }

    @Test
    void keepsTheVerifiedBytes() throws Exception {
        ModelManifest manifest = ModelManifest.load();
        assertArrayEquals(resource(ModelResources.FACE_MESH), manifest.bytes(ModelResources.FACE_MESH));
    }

    @Test
    void aNullManifestOrModelIsAnIntegrityError() {
        assertThrows(ModelIntegrityException.class,
            () -> ModelManifest.load(name -> "null".getBytes(StandardCharsets.UTF_8)));
        assertThrows(ModelIntegrityException.class,
            () -> ModelManifest.load(name -> "{\"modelSetVersion\": \"1\", \"models\": [null]}"
                .getBytes(StandardCharsets.UTF_8)));
    }
}
