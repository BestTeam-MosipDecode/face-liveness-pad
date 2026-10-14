/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * The engine core must run on Android, where {@code java.awt} and {@code javax.imageio} do not exist: only the
 * desktop adapter may use them.
 */
class NoDesktopImportsTest {

    @Test
    void onlyTheDesktopAdapterUsesDesktopImaging() throws IOException {
        List<String> offenders;
        try (Stream<Path> files = Files.walk(Paths.get("src/main/java"))) {
            offenders = files
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> !p.toString().replace('\\', '/').contains("/platform/desktop/"))
                .filter(NoDesktopImportsTest::importsDesktopImaging)
                .map(Path::toString)
                .collect(Collectors.toList());
        }
        assertEquals(Collections.emptyList(), offenders);
    }

    private static boolean importsDesktopImaging(Path file) {
        try {
            String source = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            return source.contains("import java.awt") || source.contains("import javax.imageio");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
