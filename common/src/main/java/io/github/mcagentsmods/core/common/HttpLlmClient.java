package io.github.mcagentsmods.core.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.github.mcagentsmods.core.api.AgentException;
import io.github.mcagentsmods.core.api.llm.AbstractLlmClient;
import io.github.mcagentsmods.core.api.llm.LlmCredentials;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * The HTTP half of a vendor client: everything about sending JSON over the wire
 * that does not depend on which vendor is on the other end.
 *
 * <p>Package-private on purpose. Nothing outside {@code common} may hold a
 * client, so {@code MCAgentsProvider} stays the only way to reach a language
 * model and the concrete clients cannot be constructed around it.</p>
 *
 * <p>Subclasses supply two things: how to authenticate
 * ({@link #authorize(HttpRequest.Builder)}), and how to translate between the
 * core's exchange model and their vendor's wire format.</p>
 */
abstract class HttpLlmClient extends AbstractLlmClient {

    /**
     * The connection timeout applied when opening a socket, as opposed to the
     * per request timeout that comes from the credentials. Kept short: failing
     * to connect is a different problem from a model taking its time, and
     * should be reported as such rather than sitting for the full request
     * timeout.
     */
    private static final java.time.Duration CONNECT_TIMEOUT = java.time.Duration.ofSeconds(15);

    /**
     * How much of an error body is quoted back in an {@link AgentException}.
     *
     * <p>Vendors sometimes answer a failure with an HTML page or a stack trace.
     * The first few hundred characters identify the problem; the rest would
     * only flood a server log.</p>
     */
    private static final int MAX_ERROR_BODY = 512;

    /**
     * The HTTP client every request from this instance goes through.
     *
     * <p>One per vendor client rather than one shared globally: each carries
     * its own connect timeout, and closing a vendor client must not tear down
     * connections another vendor is still using.</p>
     */
    private final HttpClient http;

    /**
     * Builds a client against a vendor's endpoint.
     *
     * @param credentials The key, endpoint, timeout, and extra headers to use.
     */
    protected HttpLlmClient(LlmCredentials credentials) {
        super(credentials);
        this.http = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Adds this vendor's authentication to a request being built.
     *
     * <p>The one place the vendors genuinely disagree at the transport level:
     * most take an {@code Authorization: Bearer} header, Anthropic takes
     * {@code x-api-key}.</p>
     *
     * @param builder The request under construction.
     */
    protected abstract void authorize(HttpRequest.Builder builder);

    /**
     * Sends a JSON body to a path below the configured base URL.
     *
     * @param path The endpoint path, starting with a slash — for example
     *             {@code "/chat/completions"}.
     * @param body The request document.
     * @return A CompletableFuture containing the parsed response document,
     *         failing with an {@link AgentException} when the vendor rejects
     *         the call or cannot be reached.
     */
    protected CompletableFuture<JsonObject> post(String path, JsonObject body) {
        try {
            ensureOpen();

            HttpRequest.Builder builder = request(path)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));

            return send(builder.build());
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(asAgentException(e));
        }
    }

    /**
     * Reads a JSON document from a path below the configured base URL.
     *
     * @param path The endpoint path, starting with a slash — for example
     *             {@code "/models"}.
     * @return A CompletableFuture containing the parsed response document,
     *         failing with an {@link AgentException} when the vendor rejects
     *         the call or cannot be reached.
     */
    protected CompletableFuture<JsonObject> get(String path) {
        try {
            ensureOpen();
            return send(request(path).GET().build());
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(asAgentException(e));
        }
    }

    /**
     * Starts a request against a path, with the common headers, the per request
     * timeout, and this vendor's authentication already applied.
     *
     * @param path The endpoint path, starting with a slash.
     * @return The builder, ready for a method to be set.
     */
    private HttpRequest.Builder request(String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(credentials.baseUrl() + path))
                .timeout(credentials.timeout())
                .header("Accept", "application/json");

        // Caller supplied headers first, so a vendor's own required headers
        // cannot be accidentally overwritten by configuration.
        for (Map.Entry<String, String> header : credentials.headers().entrySet()) {
            builder.header(header.getKey(), header.getValue());
        }
        authorize(builder);
        return builder;
    }

    /**
     * Sends a prepared request and turns the answer into a JSON document.
     *
     * <p>Everything that can go wrong past this point lands as a failed future
     * carrying an {@link AgentException}, per the
     * {@link io.github.mcagentsmods.core.api.llm.LlmClient} contract.</p>
     *
     * @param request The request to send.
     * @return A CompletableFuture containing the parsed response document.
     */
    private CompletableFuture<JsonObject> send(HttpRequest request) {
        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .handle((response, failure) -> {
                    if (failure != null) {
                        throw new CompletionException(new AgentException(
                                vendor(), "Request to " + vendor().code() + " failed: " + rootMessage(failure), failure));
                    }
                    return readBody(response);
                });
    }

    /**
     * Turns a completed exchange into a JSON document, or into the exception
     * the status code calls for.
     *
     * @param response The vendor's answer.
     * @return The parsed response document.
     * @throws AgentException When the status is not 2xx, or the body is not a
     *                        JSON object.
     */
    private JsonObject readBody(HttpResponse<String> response) {
        int status = response.statusCode();
        String body = response.body() == null ? "" : response.body();

        if (status < 200 || status >= 300) {
            throw new AgentException(vendor(), vendor().code() + " returned HTTP " + status + ": " + describe(body), status);
        }

        try {
            JsonElement parsed = JsonParser.parseString(body);
            if (!parsed.isJsonObject()) {
                throw new AgentException(vendor(), vendor().code() + " returned a non object response: " + describe(body), status);
            }
            return parsed.getAsJsonObject();
        } catch (JsonSyntaxException | IllegalStateException e) {
            throw new AgentException(vendor(), vendor().code() + " returned malformed JSON: " + describe(body), status, e);
        }
    }

    /**
     * Extracts the message a vendor put in an error body.
     *
     * <p>All four wrap their error text differently, but every one of them
     * nests a {@code message} string somewhere. Pulling it out turns an opaque
     * "HTTP 400" into something a server owner can act on. Falls back to the
     * truncated raw body when no message is found.</p>
     *
     * @param body The response body, possibly empty and possibly not JSON.
     * @return A short description safe to put in an exception message.
     */
    private String describe(String body) {
        if (body.isBlank()) {
            return "(empty response)";
        }
        try {
            JsonElement parsed = JsonParser.parseString(body);
            if (parsed.isJsonObject()) {
                String message = findMessage(parsed.getAsJsonObject());
                if (message != null) {
                    return message;
                }
            }
        } catch (JsonSyntaxException ignored) {
            // Not JSON at all — a proxy's HTML error page, most likely. The
            // truncated body below is the best that can be said about it.
        }
        return body.length() > MAX_ERROR_BODY ? body.substring(0, MAX_ERROR_BODY) + "…" : body;
    }

    /**
     * Finds the nearest {@code message} string in an error document.
     *
     * <p>Checks the document itself, then a nested {@code error} object, which
     * covers the shapes all four vendors use.</p>
     *
     * @param document The parsed error body.
     * @return The message, or {@code null} when the document holds none.
     */
    private String findMessage(JsonObject document) {
        if (document.has("message") && document.get("message").isJsonPrimitive()) {
            return document.get("message").getAsString();
        }
        if (document.has("error")) {
            JsonElement error = document.get("error");
            if (error.isJsonObject()) {
                return findMessage(error.getAsJsonObject());
            }
            if (error.isJsonPrimitive()) {
                return error.getAsString();
            }
        }
        return null;
    }

    /**
     * Describes the underlying cause of a transport failure.
     *
     * <p>{@code HttpClient} wraps failures in a {@code CompletionException}
     * whose own message is just the wrapped class name, so the useful text is
     * one level down.</p>
     *
     * @param failure The failure the future completed with.
     * @return A description of the root cause.
     */
    private String rootMessage(Throwable failure) {
        Throwable cause = failure instanceof CompletionException && failure.getCause() != null
                ? failure.getCause()
                : failure;
        String message = cause.getMessage();
        return message == null || message.isBlank() ? cause.getClass().getSimpleName() : message;
    }

    /**
     * Normalizes anything thrown before the request left into an
     * {@link AgentException}.
     *
     * @param failure What was thrown while preparing the request.
     * @return The failure as an {@link AgentException}.
     */
    private AgentException asAgentException(RuntimeException failure) {
        return failure instanceof AgentException agentFailure
                ? agentFailure
                : new AgentException(vendor(), "Could not build the request: " + failure.getMessage(), failure);
    }

    /**
     * Reads a string field, tolerating its absence.
     *
     * <p>Vendors add and drop optional fields without warning, so every read of
     * one goes through here rather than assuming it is present.</p>
     *
     * @param document The object to read from.
     * @param field The field name.
     * @param fallback What to return when the field is absent or not a string.
     * @return The field's value, or {@code fallback}.
     */
    protected static String optString(JsonObject document, String field, String fallback) {
        JsonElement value = document.get(field);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : fallback;
    }

    /**
     * Reads an integer field, tolerating its absence.
     *
     * @param document The object to read from.
     * @param field The field name.
     * @param fallback What to return when the field is absent or not a number.
     * @return The field's value, or {@code fallback}.
     */
    protected static int optInt(JsonObject document, String field, int fallback) {
        JsonElement value = document.get(field);
        if (value == null || !value.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return value.getAsInt();
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /**
     * Reads a nested object, tolerating its absence.
     *
     * @param document The object to read from.
     * @param field The field name.
     * @return The nested object, or {@code null} when absent or not an object.
     */
    protected static JsonObject optObject(JsonObject document, String field) {
        JsonElement value = document.get(field);
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : null;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Shuts the underlying {@code HttpClient} down without waiting for
     * in-flight requests to finish. Deliberately not the blocking variant: this
     * is normally called while a mod is shutting down, on a thread
     * that must not stall, and any request still running will fail on its own
     * once the client goes down.</p>
     */
    @Override
    public void close() {
        if (markClosed()) {
            http.shutdown();
        }
    }
}
