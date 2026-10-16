package io.github.mcagentsmods.core.tool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The little JSON this module needs: reading a model's arguments and writing a schema.
 *
 * <p>Written here rather than borrowed, because this module depends on the JDK alone. It reads
 * standard JSON into plain Java values — {@link Map}, {@link List}, {@link String},
 * {@link Long} for whole numbers, {@link Double} for the rest, {@link Boolean}, and
 * {@code null} — and writes strings with the escapes JSON requires.
 */
final class Json {

    /** How deep a value may nest before it is refused, so a hostile reply cannot overflow the stack. */
    private static final int MAX_DEPTH = 64;

    private final String text;
    private int position;

    private Json(String text) {
        this.text = text;
    }

    /**
     * Reads one JSON value that must fill {@code text}.
     *
     * @throws IllegalArgumentException if it is not valid JSON, with where it went wrong
     */
    static Object parse(String text) {
        if (text == null) {
            throw new IllegalArgumentException("There is no JSON to read");
        }
        Json reader = new Json(text);
        reader.skipWhitespace();
        Object value = reader.value(0);
        reader.skipWhitespace();
        if (reader.position != text.length()) {
            throw reader.error("unexpected text after the value");
        }
        return value;
    }

    /** {@code value} as a quoted JSON string. */
    static String quote(String value) {
        StringBuilder out = new StringBuilder(value.length() + 2);
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }

    private Object value(int depth) {
        if (depth > MAX_DEPTH) {
            throw error("nested deeper than " + MAX_DEPTH + " levels");
        }
        if (position >= text.length()) {
            throw error("the text ended where a value was expected");
        }
        char c = text.charAt(position);
        return switch (c) {
            case '{' -> object(depth);
            case '[' -> array(depth);
            case '"' -> string();
            case 't' -> literal("true", Boolean.TRUE);
            case 'f' -> literal("false", Boolean.FALSE);
            case 'n' -> literal("null", null);
            default -> {
                if (c == '-' || (c >= '0' && c <= '9')) {
                    yield number();
                }
                throw error("unexpected '" + c + "'");
            }
        };
    }

    private Map<String, Object> object(int depth) {
        position++; // {
        Map<String, Object> members = new LinkedHashMap<>();
        skipWhitespace();
        if (peek('}')) {
            position++;
            return Collections.unmodifiableMap(members);
        }
        while (true) {
            skipWhitespace();
            if (!peek('"')) {
                throw error("expected a member name in quotes");
            }
            String name = string();
            skipWhitespace();
            expect(':');
            skipWhitespace();
            members.put(name, value(depth + 1));
            skipWhitespace();
            if (peek(',')) {
                position++;
                continue;
            }
            expect('}');
            return Collections.unmodifiableMap(members);
        }
    }

    private List<Object> array(int depth) {
        position++; // [
        List<Object> items = new ArrayList<>();
        skipWhitespace();
        if (peek(']')) {
            position++;
            return Collections.unmodifiableList(items);
        }
        while (true) {
            skipWhitespace();
            items.add(value(depth + 1));
            skipWhitespace();
            if (peek(',')) {
                position++;
                continue;
            }
            expect(']');
            return Collections.unmodifiableList(items);
        }
    }

    private String string() {
        position++; // opening quote
        StringBuilder out = new StringBuilder();
        while (position < text.length()) {
            char c = text.charAt(position++);
            if (c == '"') {
                return out.toString();
            }
            if (c != '\\') {
                if (c < 0x20) {
                    throw error("a control character inside a string");
                }
                out.append(c);
                continue;
            }
            if (position >= text.length()) {
                break;
            }
            char escape = text.charAt(position++);
            switch (escape) {
                case '"' -> out.append('"');
                case '\\' -> out.append('\\');
                case '/' -> out.append('/');
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case 'u' -> {
                    if (position + 4 > text.length()) {
                        throw error("a \\u escape cut short");
                    }
                    try {
                        out.append((char) Integer.parseInt(text.substring(position, position + 4), 16));
                    } catch (NumberFormatException e) {
                        throw error("a \\u escape that is not four hex digits");
                    }
                    position += 4;
                }
                default -> throw error("an unknown escape \\" + escape);
            }
        }
        throw error("a string that never closes");
    }

    private Object number() {
        int start = position;
        if (peek('-')) {
            position++;
        }
        boolean whole = true;
        while (position < text.length()) {
            char c = text.charAt(position);
            if (c >= '0' && c <= '9') {
                position++;
            } else if (c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                whole = false;
                position++;
            } else {
                break;
            }
        }
        String digits = text.substring(start, position);
        try {
            if (whole) {
                return Long.parseLong(digits);
            }
            return Double.parseDouble(digits);
        } catch (NumberFormatException e) {
            if (whole) {
                // Too long for a long: still a number, just not a whole one this reader keeps exactly.
                try {
                    return Double.parseDouble(digits);
                } catch (NumberFormatException ignored) {
                    // falls through to the error below
                }
            }
            position = start;
            throw error("'" + digits + "' is not a number");
        }
    }

    private Object literal(String word, Object value) {
        if (!text.startsWith(word, position)) {
            throw error("unexpected '" + text.charAt(position) + "'");
        }
        position += word.length();
        return value;
    }

    private void expect(char c) {
        if (!peek(c)) {
            throw error("expected '" + c + "'");
        }
        position++;
    }

    private boolean peek(char c) {
        return position < text.length() && text.charAt(position) == c;
    }

    private void skipWhitespace() {
        while (position < text.length()) {
            char c = text.charAt(position);
            if (c != ' ' && c != '\n' && c != '\r' && c != '\t') {
                return;
            }
            position++;
        }
    }

    private IllegalArgumentException error(String what) {
        return new IllegalArgumentException("Not valid JSON at character " + position + ": " + what);
    }
}
