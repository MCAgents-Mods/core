package io.github.mcagentsmods.core.api.web;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A request to a module's API, after the console has checked the token and the host.
 *
 * @param method the HTTP method, upper case
 * @param path   everything after {@code /api/{id}/}, URL-decoded per segment, without a
 *               leading slash; empty for the module's API root
 * @param query  the query string's parameters, decoded, without the console's own token
 * @param body   the request body as UTF-8 text; empty when there was none
 */
public record WebRequest(String method, String path, Map<String, String> query, String body) {

    public WebRequest {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(path, "path");
        query = query == null ? Map.of() : Map.copyOf(query);
        body = body == null ? "" : body;
    }

    /** Whether this is a {@code method} request for exactly {@code path}. */
    public boolean is(String method, String path) {
        return this.method.equals(method) && this.path.equals(path);
    }

    /** One query parameter, if it was sent. */
    public Optional<String> parameter(String name) {
        return Optional.ofNullable(query.get(name));
    }
}
