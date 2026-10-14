/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class JsonReaderTest {

    @Test
    void readsNestedValues() {
        Map<?, ?> root = (Map<?, ?>) JsonReader.parse(
            " {\"a\": [1, -2.5, 3e2, true, false, null], \"b\": {\"c\": \"d\"}, \"e\": []} ");
        assertEquals(Arrays.asList(1L, -2.5, 300.0, true, false, null), root.get("a"));
        assertEquals("d", ((Map<?, ?>) root.get("b")).get("c"));
        assertTrue(((List<?>) root.get("e")).isEmpty());
        assertEquals(Arrays.asList("a", "b", "e"), Arrays.asList(root.keySet().toArray()));
    }

    @Test
    void readsEscapes() {
        assertEquals("q\"b\\s/n\nt\tu\u00e9", JsonReader.parse("\"q\\\"b\\\\s\\/n\\nt\\tu\\u00e9\""));
        assertNull(JsonReader.parse("null"));
        assertEquals(Long.valueOf(232589), JsonReader.parse("232589"));
    }

    @Test
    void refusesInvalidText() {
        for (String bad : new String[] {"", "{", "{\"a\" 1}", "{\"a\": 1,}", "[1 2]", "\"open", "tru", "{} x",
            "\"\\x\"", "\"\\u12\"", "-", "{1: 2}"}) {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> JsonReader.parse(bad),
                bad);
            assertTrue(e.getMessage().startsWith("invalid JSON at offset"), e.getMessage());
        }
    }
}
