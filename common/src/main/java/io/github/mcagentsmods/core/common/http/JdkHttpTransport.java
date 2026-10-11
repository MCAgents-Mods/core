package io.github.mcagentsmods.core.common.http;

import io.github.mcagentsmods.core.api.http.HttpCall;
import io.github.mcagentsmods.core.api.http.HttpReply;
import io.github.mcagentsmods.core.api.http.HttpTransport;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * The real transport, over the JDK's HTTP client.
 *
 * <p>Both timeouts are set deliberately. A request without one can hang until the player
 * closes the game, and "the mod froze" is indistinguishable from "the mod is broken".
 */
public final class JdkHttpTransport implements HttpTransport {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(120);

    /** The whole exchange, body included; a little past the request timeout. */
    private static final Duration EXCHANGE_DEADLINE = REQUEST_TIMEOUT.plusSeconds(10);

    /**
     * The largest response read. A chat answer or an embedding is kilobytes; anything near
     * this is a misbehaving server, and reading it would be the game's memory.
     */
    static final int MAX_RESPONSE_BYTES = 8 * 1024 * 1024;

    private final HttpClient httpClient;

    public JdkHttpTransport() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                // Never followed: the client re-sends its headers to wherever a redirect
                // points, Authorization included, so a provider or a proxy answering 3xx
                // could hand the key to another host. A redirect reads as a failed request.
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public CompletableFuture<HttpReply> send(HttpCall call) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(call.url()))
                // Generous, because a long answer legitimately takes a while to generate.
                .timeout(REQUEST_TIMEOUT);

        builder.method(call.method(), HttpRequest.BodyPublishers.ofString(call.body()));

        call.headers().forEach(builder::header);

        return HttpLimits.withDeadline(
                        httpClient.sendAsync(builder.build(),
                                HttpLimits.ofString(MAX_RESPONSE_BYTES)),
                        EXCHANGE_DEADLINE)
                .thenApply(response -> new HttpReply(response.statusCode(), response.body()));
    }

    /**
     * Aborts every request in flight and returns at once.
     *
     * <p>Not {@code HttpClient.close()}, which waits for requests in flight to finish — up to
     * the request timeout, on whatever thread closed the client, which is often the game's.
     */
    @Override
    public void close() {
        httpClient.shutdownNow();
    }
}
