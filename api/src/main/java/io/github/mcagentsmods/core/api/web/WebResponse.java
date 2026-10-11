package io.github.mcagentsmods.core.api.web;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * A module's answer to an API request.
 *
 * <p>The console adds the headers every answer carries — no caching, no sniffing, the content
 * security policy — so a module supplies only the status and the body.
 *
 * @param status      the HTTP status
 * @param contentType the body's media type
 * @param body        the body; may be empty
 */
public record WebResponse(int status, String contentType, byte[] body) {

    private static final String JSON = "application/json; charset=utf-8";

    public WebResponse {
        Objects.requireNonNull(contentType, "contentType");
        Objects.requireNonNull(body, "body");
        if (status < 100 || status > 599) {
            throw new IllegalArgumentException("Not an HTTP status: " + status);
        }
        body = body.clone();
    }

    @Override
    public byte[] body() {
        return body.clone();
    }

    /** A JSON answer. {@code json} is sent as it stands, so it must already be JSON. */
    public static WebResponse json(int status, String json) {
        Objects.requireNonNull(json, "json");
        return new WebResponse(status, JSON, json.getBytes(StandardCharsets.UTF_8));
    }

    /** {@code 200} with a JSON body. */
    public static WebResponse ok(String json) {
        return json(200, json);
    }

    /**
     * An error a page can show: {@code {"error": message}}.
     *
     * @param message written for the player; never a key, a path or an exception's text
     */
    public static WebResponse error(int status, String message) {
        String text = message == null || message.isBlank() ? "Request refused." : message;
        return json(status, "{\"error\":" + quote(text) + "}");
    }

    /**
     * {@code value} as a JSON string literal, quotes included.
     *
     * <p>Here so a module that builds a small answer by hand does not need a JSON library
     * to do it safely.
     */
    public static String quote(String value) {
        StringBuilder out = new StringBuilder(value.length() + 2).append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                // < and > as escapes, so a string can never close a <script> it lands in.
                case '<' -> out.append("\\u003c");
                case '>' -> out.append("\\u003e");
                default -> {
                    if (c < 0x20 || c == 0x2028 || c == 0x2029) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }
}
