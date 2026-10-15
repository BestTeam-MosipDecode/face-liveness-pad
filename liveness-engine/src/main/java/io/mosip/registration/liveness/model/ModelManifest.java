/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.model;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.mosip.registration.liveness.api.ModelIntegrityException;

/**
 * The manifest {@code models.json} packaged with the models, checked when the engine starts (#31): every model file
 * must exist, have the declared size and the declared SHA-256. Only the fields of each model entry count; the
 * digests of its {@code source} block describe the original weights and are ignored here.
 */
public final class ModelManifest {

    static final String MANIFEST = "models.json";

    private final String modelSetVersion;
    private final List<String> files;

    private ModelManifest(String modelSetVersion, List<String> files) {
        this.modelSetVersion = modelSetVersion;
        this.files = Collections.unmodifiableList(files);
    }

    /** Reads and checks the manifest and the model files of the JAR. */
    public static ModelManifest load() throws ModelIntegrityException {
        return load(ModelManifest::resource);
    }

    /** Same check with another source of files: name to bytes, or null when the file does not exist. */
    static ModelManifest load(Function<String, byte[]> source) throws ModelIntegrityException {
        byte[] manifest = source.apply(MANIFEST);
        if (manifest == null) {
            throw new ModelIntegrityException(MANIFEST + " not found next to the models");
        }
        String version;
        List<?> models;
        try {
            Map<?, ?> root = (Map<?, ?>) JsonReader.parse(new String(manifest, StandardCharsets.UTF_8));
            version = (String) root.get("modelSetVersion");
            models = (List<?>) root.get("models");
        } catch (IllegalArgumentException | ClassCastException e) {
            throw new ModelIntegrityException(MANIFEST + " is not a valid model manifest", e);
        }
        if (version == null || models == null || models.isEmpty()) {
            throw new ModelIntegrityException(MANIFEST + " gives no model set version or no model");
        }
        List<String> files = new ArrayList<>();
        for (Object item : models) {
            String file;
            String sha256;
            Number size;
            try {
                Map<?, ?> model = (Map<?, ?>) item;
                file = (String) model.get("file");
                sha256 = (String) model.get("sha256");
                size = (Number) model.get("sizeBytes");
            } catch (ClassCastException e) {
                throw new ModelIntegrityException(MANIFEST + " is not a valid model manifest", e);
            }
            if (file == null || sha256 == null || size == null) {
                throw new ModelIntegrityException(MANIFEST + " has an incomplete model entry");
            }
            check(file, sha256, size.longValue(), source.apply(file));
            files.add(file);
        }
        return new ModelManifest(version, files);
    }

    private static void check(String file, String sha256, long size, byte[] data) throws ModelIntegrityException {
        if (data == null) {
            throw new ModelIntegrityException("model file missing: " + file);
        }
        if (data.length != size) {
            throw new ModelIntegrityException("model file " + file + " has " + data.length + " bytes, " + MANIFEST
                + " expects " + size);
        }
        if (!sha256(data).equalsIgnoreCase(sha256)) {
            throw new ModelIntegrityException("model file " + file + " does not match its SHA-256 in " + MANIFEST);
        }
    }

    /** Version of the model set, reported in results and audit events. */
    public String modelSetVersion() {
        return modelSetVersion;
    }

    /** Model files checked, in the order of the manifest. */
    public List<String> files() {
        return files;
    }

    static String sha256(byte[] data) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static byte[] resource(String file) {
        try (InputStream in = ModelManifest.class.getResourceAsStream("/models/" + file)) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }
}
