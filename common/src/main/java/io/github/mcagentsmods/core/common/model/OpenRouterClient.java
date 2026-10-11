package io.github.mcagentsmods.core.common.model;

import io.github.mcagentsmods.core.api.http.HttpCall;
import io.github.mcagentsmods.core.api.http.HttpReply;
import io.github.mcagentsmods.core.api.http.HttpTransport;
import io.github.mcagentsmods.core.api.model.ApiKeySource;
import io.github.mcagentsmods.core.api.model.ChatCompletion;
import io.github.mcagentsmods.core.api.model.ChatRequest;
import io.github.mcagentsmods.core.api.model.ModelClient;
import io.github.mcagentsmods.core.api.model.ModelException;
import io.github.mcagentsmods.core.api.model.ProviderRoute;
import io.github.mcagentsmods.core.api.model.ProviderRoutes;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * The {@link ModelClient} that speaks the wire format OpenRouter defines.
 *
 * <p>That format is OpenAI's — {@code POST {base}/chat/completions} and
 * {@code POST {base}/embeddings}, a bearer token, the same request and response shapes — which
 * is why one client covers both routes a request can take. A {@link ProviderRoutes} decides
 * which: through the gateway, where the whole {@code {platform}/{model}} name travels and the
 * gateway's own extensions are welcome, or straight to a platform's own API, where the bare
 * model id travels and those extensions would be a 400. The default is the gateway, for every
 * request, which is what this client did before it could route at all.
 *
 * <p>Three behaviours live here so that no caller has to think about them:
 *
 * <ul>
 *   <li><strong>Key rotation.</strong> A rejected key is deactivated and the next one is
 *       tried, so a player who topped up a second key does not see a failed conversation.</li>
 *   <li><strong>Retries.</strong> Rate limits, provider failures, and transport failures
 *       back off and retry. A bad request does not — repeating it cannot help.</li>
 *   <li><strong>Timeouts.</strong> Enforced by the transport, on every call.</li>
 * </ul>
 *
 * <p>This class knows nothing about Minecraft and nothing about where keys are stored. It
 * is given an {@link ApiKeySource} and an {@link HttpTransport}, which is what lets every
 * behaviour above be tested without a network or an API key.
 *
 * <p>Every method may be called from any thread, and none of them blocks. A returned future
 * completes on a transport or retry thread, never on the caller's, so a callback that
 * touches game state must hop back onto the game thread first.
 */
public final class OpenRouterClient implements ModelClient {

    private static final Logger LOGGER = System.getLogger(OpenRouterClient.class.getName());

    /** Attempts per request, including the first. Three is enough to ride out a blip. */
    private static final int MAX_ATTEMPTS = 3;

    private static final Duration DEFAULT_INITIAL_BACKOFF = Duration.ofMillis(500);

    /** How many keys to try before giving up, so a wallet of dead keys still terminates. */
    private static final int MAX_KEYS_PER_REQUEST = 5;

    private final HttpTransport transport;
    private final ApiKeySource keys;
    private final ProviderRoutes routes;
    private final ScheduledExecutorService scheduler;
    private final boolean ownsScheduler;
    private final Duration initialBackoff;
    private final Attribution attribution;
    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * Backoffs not yet over. Held so {@link #close()} can fail them: a scheduler shut down
     * with {@code shutdownNow} drops its queue, and a request waiting on a dropped backoff
     * would otherwise never complete.
     */
    private final Set<CompletableFuture<Void>> pendingDelays = ConcurrentHashMap.newKeySet();

    /** A client that sends everything through the gateway, attributed to core. */
    public OpenRouterClient(HttpTransport transport, ApiKeySource keys) {
        this(transport, keys, ProviderRoutes.throughOpenRouter());
    }

    /**
     * A client that asks {@code routes} where each request goes, attributed to core.
     *
     * @param routes resolves a stored model id to the address and the key that serve it
     */
    public OpenRouterClient(HttpTransport transport, ApiKeySource keys, ProviderRoutes routes) {
        this(transport, keys, routes, Attribution.CORE);
    }

    /**
     * A client that asks {@code routes} where each request goes, and names the calling mod to
     * OpenRouter.
     *
     * @param attribution what OpenRouter is told about the caller; {@link Attribution#NONE}
     *                    sends nothing
     */
    public OpenRouterClient(HttpTransport transport, ApiKeySource keys, ProviderRoutes routes,
                            Attribution attribution) {
        this(transport, keys, routes, attribution, defaultScheduler(), true,
                DEFAULT_INITIAL_BACKOFF);
    }

    /** Test seam: the backoff is injectable so retry tests do not sleep for real. */
    OpenRouterClient(HttpTransport transport, ApiKeySource keys, ProviderRoutes routes,
                     Attribution attribution, ScheduledExecutorService scheduler,
                     boolean ownsScheduler, Duration initialBackoff) {
        this.transport = Objects.requireNonNull(transport, "transport");
        this.keys = Objects.requireNonNull(keys, "keys");
        this.routes = Objects.requireNonNull(routes, "routes");
        this.attribution = Objects.requireNonNull(attribution, "attribution");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.ownsScheduler = ownsScheduler;
        this.initialBackoff = Objects.requireNonNull(initialBackoff, "initialBackoff");
    }

    private static ScheduledExecutorService defaultScheduler() {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, runnable -> {
            Thread thread = new Thread(runnable, "MCAgents-core-model-retry");
            // The game must be able to exit while a backoff is pending.
            thread.setDaemon(true);
            return thread;
        });
        executor.setRemoveOnCancelPolicy(true);
        return executor;
    }

    @Override
    public CompletableFuture<ChatCompletion> complete(ChatRequest request) {
        Objects.requireNonNull(request, "request");
        if (closed.get()) {
            return CompletableFuture.failedFuture(closedException());
        }
        return routes.route(request.model()).thenCompose(route -> {
            // The route decides what the model is called on the wire: the whole stored name
            // through the gateway, the platform's own id when the platform is called directly.
            ChatRequest routed = route.isThroughOpenRouter()
                    ? request
                    : new ChatRequest(route.modelId(), request.messages(), request.tools());
            String body = OpenRouterPayloads.chatRequestBody(routed, route.isThroughOpenRouter());
            return withKeyRotation(route, route.chatUrl(), body,
                    OpenRouterPayloads::readChatCompletion, 0);
        });
    }

    @Override
    public CompletableFuture<float[]> embed(String model, String text) {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(text, "text");
        if (closed.get()) {
            return CompletableFuture.failedFuture(closedException());
        }
        if (model.isBlank()) {
            return CompletableFuture.failedFuture(new ModelException(
                    ModelException.Kind.REQUEST, "An embedding request needs a model"));
        }
        return routes.route(model).thenCompose(route -> {
            String body = OpenRouterPayloads.embeddingRequestBody(route.modelId(), text);
            return withKeyRotation(route, route.embeddingsUrl(), body,
                    OpenRouterPayloads::readEmbedding, 0);
        });
    }

    /**
     * Sends with the next active key, moving to the one after it if the provider rejects it.
     *
     * @param keysTried how many keys have already been rejected for this request
     */
    private <T> CompletableFuture<T> withKeyRotation(
            ProviderRoute route, String url, String body, Function<String, T> reader,
            int keysTried) {

        if (keysTried >= MAX_KEYS_PER_REQUEST) {
            return CompletableFuture.failedFuture(new ModelException(ModelException.Kind.NO_KEY,
                    "Every stored API key was rejected"));
        }

        return keys.next(route.platform()).thenCompose(maybeKey -> {
            Optional<String> key = maybeKey;
            if (key.isEmpty()) {
                return CompletableFuture.<T>failedFuture(new ModelException(
                        ModelException.Kind.NO_KEY,
                        keysTried == 0
                                ? "No API key is configured for " + route.platform()
                                : "Every stored API key was rejected"));
            }
            String apiKey = key.get();

            return attempt(route, url, body, apiKey, reader, 1).handle((result, failure) -> {
                if (failure == null) {
                    return CompletableFuture.completedFuture(result);
                }
                ModelException error = asModelException(failure);
                if (error.kind() != ModelException.Kind.AUTHENTICATION) {
                    return CompletableFuture.<T>failedFuture(error);
                }
                // The provider rejected this key. Retire it and carry on with the next,
                // without the player seeing a failure that another key would have served.
                LOGGER.log(Level.WARNING, "An API key was rejected; rotating to the next");
                return keys.reject(apiKey)
                        .thenCompose(ignored ->
                                withKeyRotation(route, url, body, reader, keysTried + 1));
            }).thenCompose(Function.identity());
        });
    }

    /** One send, retrying the same key while the failure is worth retrying. */
    private <T> CompletableFuture<T> attempt(
            ProviderRoute route, String url, String body, String apiKey,
            Function<String, T> reader, int attempt) {

        CompletableFuture<HttpReply> sent;
        try {
            sent = transport.send(HttpCall.post(url, headers(apiKey, route), body));
        } catch (RuntimeException e) {
            // Thrown before anything left the machine, most often by a header the HTTP client
            // refuses. Its message can quote that header, which is the key, so neither the
            // message nor the exception travels on.
            return CompletableFuture.failedFuture(new ModelException(
                    ModelException.Kind.REQUEST, "The request to the provider could not be built"));
        }

        return sent
                .handle((reply, failure) -> {
                    if (failure != null) {
                        throw new ModelException(ModelException.Kind.TRANSPORT,
                                "The request to the provider could not be sent", failure);
                    }
                    return reply;
                })
                .thenApply(reply -> {
                    if (!reply.isSuccess()) {
                        throw errorFor(reply);
                    }
                    return read(reader, reply.body());
                })
                .handle((result, failure) -> {
                    if (failure == null) {
                        return CompletableFuture.completedFuture(result);
                    }
                    ModelException error = asModelException(failure);
                    if (!error.isRetryable() || attempt >= MAX_ATTEMPTS) {
                        return CompletableFuture.<T>failedFuture(error);
                    }
                    return delay(backoffFor(attempt))
                            .thenCompose(ignored ->
                                    attempt(route, url, body, apiKey, reader, attempt + 1));
                })
                .thenCompose(Function.identity());
    }

    private Map<String, String> headers(String apiKey, ProviderRoute route) {
        Map<String, String> headers = new LinkedHashMap<>();
        // The one place a key value leaves this mod — and it goes to the platform the key was
        // stored for, never to another one.
        headers.put("Authorization", "Bearer " + apiKey);
        headers.put("Content-Type", "application/json");
        if (route.isThroughOpenRouter()) {
            // OpenRouter uses these for attribution; neither identifies the player. They mean
            // nothing to a platform's own API, so they are not sent there.
            attribution.referer().ifPresent(referer -> headers.put("HTTP-Referer", referer));
            attribution.title().ifPresent(title -> headers.put("X-Title", title));
        }
        return headers;
    }

    private static ModelException errorFor(HttpReply reply) {
        String message = OpenRouterPayloads.readErrorMessage(reply.body(), reply.status());
        return new ModelException(ModelException.Kind.forStatus(reply.status()), message);
    }

    /** Exponential, with jitter so a wave of clients does not retry in lockstep. */
    private Duration backoffFor(int attempt) {
        long base = initialBackoff.toMillis() * (1L << (attempt - 1));
        long jitter = ThreadLocalRandom.current().nextLong(base / 2 + 1);
        return Duration.ofMillis(base + jitter);
    }

    /**
     * Reads a successful body, so that any failure to read it is malformed rather than a
     * transport failure.
     *
     * <p>The distinction is money. A body of the wrong shape makes Gson throw an
     * {@link IllegalStateException} or a {@link ClassCastException}; classed as a transport
     * failure, that would be retried, and every retry of a request that already succeeded is
     * billed again.
     */
    private static <T> T read(Function<String, T> reader, String body) {
        try {
            return reader.apply(body);
        } catch (ModelException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ModelException(ModelException.Kind.MALFORMED_RESPONSE,
                    "The provider returned a response of an unexpected shape", e);
        }
    }

    private CompletableFuture<Void> delay(Duration duration) {
        CompletableFuture<Void> waited = new CompletableFuture<>();
        pendingDelays.add(waited);
        waited.whenComplete((ignored, failure) -> pendingDelays.remove(waited));
        if (closed.get()) {
            waited.completeExceptionally(closedException());
            return waited;
        }
        try {
            scheduler.schedule(() -> waited.complete(null), duration.toMillis(),
                    TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            waited.completeExceptionally(closedException());
        }
        return waited;
    }

    private static ModelException closedException() {
        return new ModelException(ModelException.Kind.CLOSED, "The model client is closed");
    }

    private static ModelException asModelException(Throwable failure) {
        Throwable cause = failure instanceof java.util.concurrent.CompletionException
                ? failure.getCause()
                : failure;
        if (cause instanceof ModelException modelException) {
            return modelException;
        }
        return new ModelException(ModelException.Kind.TRANSPORT,
                "The request to the provider failed", cause);
    }

    /** The executor retries are scheduled on, exposed for tests. */
    Executor scheduler() {
        return scheduler;
    }

    /**
     * What OpenRouter is told about the mod making a request.
     *
     * <p>OpenRouter reads {@code HTTP-Referer} and {@code X-Title} to attribute traffic to an
     * app. Each mod passes its own, so its traffic is not counted as another mod's. Neither
     * value identifies the player.
     *
     * @param refererValue the {@code HTTP-Referer} value, or empty to send none
     * @param titleValue   the {@code X-Title} value, or empty to send none
     */
    public record Attribution(String refererValue, String titleValue) {

        /** Attribution to core itself, for a caller that names nothing. */
        public static final Attribution CORE =
                new Attribution("https://github.com/MCAgents-Mods/core", "MCAgents");

        /** Sends no attribution headers at all. */
        public static final Attribution NONE = new Attribution("", "");

        /**
         * Validates both values.
         *
         * @throws IllegalArgumentException if either holds a control character, which would
         *                                  let a value end the header and start another
         */
        public Attribution {
            refererValue = refererValue == null ? "" : refererValue.strip();
            titleValue = titleValue == null ? "" : titleValue.strip();
            requireHeaderSafe(refererValue, "referer");
            requireHeaderSafe(titleValue, "title");
        }

        Optional<String> referer() {
            return refererValue.isEmpty() ? Optional.empty() : Optional.of(refererValue);
        }

        Optional<String> title() {
            return titleValue.isEmpty() ? Optional.empty() : Optional.of(titleValue);
        }

        private static void requireHeaderSafe(String value, String name) {
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (c < 0x20 || c == 0x7f) {
                    throw new IllegalArgumentException(
                            "The attribution " + name + " contains a control character");
                }
            }
        }
    }

    /**
     * Closes the transport and the retry scheduler, and fails every request still waiting on
     * a backoff with {@link ModelException.Kind#CLOSED}. Safe to call twice.
     */
    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        // Waiters first: a request failed here never reaches the transport, so closing the
        // transport afterwards cannot race a retry being sent through it.
        if (ownsScheduler) {
            scheduler.shutdownNow();
        }
        for (CompletableFuture<Void> waiting : List.copyOf(pendingDelays)) {
            waiting.completeExceptionally(closedException());
        }
        transport.close();
    }
}
