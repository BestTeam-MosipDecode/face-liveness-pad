/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Small JSON reader for models.json. The engine adds no JSON library to the Registration Client, whose classpath
 * already carries its own versions. Objects become {@code LinkedHashMap}, arrays {@code ArrayList}, integers
 * {@code Long}, other numbers {@code Double}.
 */
final class JsonReader {

    private final String text;
    private int pos;

    private JsonReader(String text) {
        this.text = text;
    }

    /** Parses a whole document. Throws {@link IllegalArgumentException} on invalid text. */
    static Object parse(String text) {
        JsonReader reader = new JsonReader(text);
        reader.skipSpace();
        Object value = reader.value();
        reader.skipSpace();
        if (reader.pos != text.length()) {
            throw reader.error("unexpected content after the value");
        }
        return value;
    }

    private Object value() {
        if (pos >= text.length()) {
            throw error("unexpected end");
        }
        char c = text.charAt(pos);
        switch (c) {
            case '{':
                return object();
            case '[':
                return array();
            case '"':
                return string();
            case 't':
                return literal("true", Boolean.TRUE);
            case 'f':
                return literal("false", Boolean.FALSE);
            case 'n':
                return literal("null", null);
            default:
                if (c == '-' || (c >= '0' && c <= '9')) {
                    return number();
                }
                throw error("unexpected character '" + c + "'");
        }
    }

    private Map<String, Object> object() {
        Map<String, Object> map = new LinkedHashMap<>();
        pos++;
        skipSpace();
        if (peek('}')) {
            pos++;
            return map;
        }
        while (true) {
            skipSpace();
            if (!peek('"')) {
                throw error("expected a key");
            }
            String key = string();
            skipSpace();
            expect(':');
            skipSpace();
            map.put(key, value());
            skipSpace();
            if (peek(',')) {
                pos++;
                continue;
            }
            expect('}');
            return map;
        }
    }

    private List<Object> array() {
        List<Object> list = new ArrayList<>();
        pos++;
        skipSpace();
        if (peek(']')) {
            pos++;
            return list;
        }
        while (true) {
            skipSpace();
            list.add(value());
            skipSpace();
            if (peek(',')) {
                pos++;
                continue;
            }
            expect(']');
            return list;
        }
    }

    private String string() {
        pos++;
        StringBuilder out = new StringBuilder();
        while (pos < text.length()) {
            char c = text.charAt(pos++);
            if (c == '"') {
                return out.toString();
            }
            if (c < 0x20) {
                throw error("control character in a string");
            }
            if (c != '\\') {
                out.append(c);
                continue;
            }
            if (pos >= text.length()) {
                break;
            }
            char e = text.charAt(pos++);
            switch (e) {
                case '"':
                case '\\':
                case '/':
                    out.append(e);
                    break;
                case 'b':
                    out.append('\b');
                    break;
                case 'f':
                    out.append('\f');
                    break;
                case 'n':
                    out.append('\n');
                    break;
                case 'r':
                    out.append('\r');
                    break;
                case 't':
                    out.append('\t');
                    break;
                case 'u':
                    out.append(unicode());
                    break;
                default:
                    throw error("bad escape '\\" + e + "'");
            }
        }
        throw error("unterminated string");
    }

    private char unicode() {
        if (pos + 4 > text.length()) {
            throw error("bad unicode escape");
        }
        try {
            char c = (char) Integer.parseInt(text.substring(pos, pos + 4), 16);
            pos += 4;
            return c;
        } catch (NumberFormatException e) {
            throw error("bad unicode escape");
        }
    }

    private Number number() {
        int start = pos;
        while (pos < text.length() && "0123456789+-.eE".indexOf(text.charAt(pos)) >= 0) {
            pos++;
        }
        String s = text.substring(start, pos);
        try {
            if (s.indexOf('.') < 0 && s.indexOf('e') < 0 && s.indexOf('E') < 0) {
                return Long.parseLong(s);
            }
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            pos = start;
            throw error("bad number '" + s + "'");
        }
    }

    private Object literal(String word, Object value) {
        if (!text.startsWith(word, pos)) {
            throw error("unexpected literal");
        }
        pos += word.length();
        return value;
    }

    private void skipSpace() {
        while (pos < text.length() && " \t\r\n".indexOf(text.charAt(pos)) >= 0) {
            pos++;
        }
    }

    private boolean peek(char c) {
        return pos < text.length() && text.charAt(pos) == c;
    }

    private void expect(char c) {
        if (!peek(c)) {
            throw error("expected '" + c + "'");
        }
        pos++;
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException("invalid JSON at offset " + pos + ": " + message);
    }
}
