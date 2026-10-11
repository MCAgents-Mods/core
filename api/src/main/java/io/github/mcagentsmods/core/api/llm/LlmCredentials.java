package io.github.mcagentsmods.core.api.llm;

import io.github.mcagentsmods.core.api.model.ModelPlatform;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * What is needed to reach one vendor: a key, an endpoint, a timeout, and any
 * extra headers that vendor wants.
 *
 * <p>Credentials are immutable and hold a secret, so treat them as one. In
 * particular {@link #toString()} is overridden to redact
 * {@link #apiKey()} — a record's generated {@code toString} would otherwise
 * print the key into any log line that happened to include the credentials, and
 * on a Minecraft server that log is frequently pasted in public.</p>
 *
 * @param vendor The service these credentials reach, never {@code null}.
 * @param apiKey The secret to authenticate with, never blank. Where it is
 *               placed on the request is the vendor's business — Anthropic
 *               takes it in {@code x-api-key}, the rest in an
 *               {@code Authorization: Bearer} header.
 * @param baseUrl The endpoint to call, never blank and never ending in a
 *                slash. Defaults to {@link LlmVendor#defaultBaseUrl()}; override
 *                it to route through a proxy or a self hosted gateway.
 * @param timeout How long a single request may take before it is abandoned.
 *                Always positive. This bounds one HTTP exchange, not a retry
 *                sequence — the core does not retry.
 * @param headers Extra headers sent with every request, never {@code null} and
 *                always an unmodifiable copy. OpenRouter, for one, uses
 *                {@code HTTP-Referer} and {@code X-Title} for attribution.
 */
public record LlmCredentials(
        LlmVendor vendor,
        String apiKey,
        String baseUrl,
        Duration timeout,
        Map<String, String> headers) {

    /**
     * The timeout applied when a caller does not choose one. Sixty seconds is
     * long enough for a large completion and short enough that a wedged request
     * does not hold a connection open indefinitely.
     */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

    /**
     * Validates the components, strips any trailing slash from the base URL,
     * and defensively copies the headers.
     *
     * @throws NullPointerException When any component is {@code null}.
     * @throws IllegalArgumentException When the key or base URL is blank, the
     *                                  base URL is neither https nor a loopback
     *                                  http address, or the timeout is zero or
     *                                  negative.
     */
    public LlmCredentials {
        Objects.requireNonNull(vendor, "vendor cannot be null");
        Objects.requireNonNull(apiKey, "apiKey cannot be null");
        Objects.requireNonNull(baseUrl, "baseUrl cannot be null");
        Objects.requireNonNull(timeout, "timeout cannot be null");
        Objects.requireNonNull(headers, "headers cannot be null");

        if (apiKey.isBlank()) {
            throw new IllegalArgumentException("apiKey cannot be blank");
        }
        // Checked here rather than left to the HTTP client, whose refusal quotes the header
        // value, which is this key. The message names no character and no position.
        if (!isWellFormedKey(apiKey)) {
            throw new IllegalArgumentException(
                    "apiKey may hold only printable ASCII without spaces");
        }
        if (baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl cannot be blank");
        }
        // The key travels in a header on every request, so a plain-text URL to another
        // machine would put it on the wire in the clear. Loopback is allowed for a local
        // gateway.
        if (!ModelPlatform.isUsableBaseUrl(baseUrl)) {
            throw new IllegalArgumentException(
                    "baseUrl must be https, or http on a loopback address");
        }
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }

        // Endpoint paths are appended with a leading slash, so a trailing one
        // here would produce a double slash the vendor may or may not accept.
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        headers = Map.copyOf(headers);
    }

    /**
     * Whether a string can be an API key: non-empty, and printable ASCII with no space.
     *
     * <p>Every provider's keys fit that, and anything outside it is a paste gone wrong — a
     * newline, a trailing space, a smart quote. Such a value cannot be sent as a header, and
     * a newline in one could start a second header. Callers check with this before storing a
     * key, so the refusal never has to repeat the value.
     *
     * @param apiKey the candidate, may be {@code null}
     * @return {@code true} if the value can be stored and sent as a key
     */
    public static boolean isWellFormedKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return false;
        }
        for (int i = 0; i < apiKey.length(); i++) {
            char c = apiKey.charAt(i);
            if (c < 0x21 || c > 0x7e) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates credentials for a vendor using its default endpoint, the default
     * timeout, and no extra headers.
     *
     * @param vendor The service to reach.
     * @param apiKey The secret to authenticate with.
     * @return The new credentials.
     */
    public static LlmCredentials of(LlmVendor vendor, String apiKey) {
        Objects.requireNonNull(vendor, "vendor cannot be null");
        return new LlmCredentials(vendor, apiKey, vendor.defaultBaseUrl(), DEFAULT_TIMEOUT, Map.of());
    }

    /**
     * Creates credentials for a vendor reached through a different endpoint.
     *
     * @param vendor The service to reach.
     * @param apiKey The secret to authenticate with.
     * @param baseUrl The endpoint to call instead of the vendor's default.
     * @return The new credentials.
     */
    public static LlmCredentials of(LlmVendor vendor, String apiKey, String baseUrl) {
        return new LlmCredentials(vendor, apiKey, baseUrl, DEFAULT_TIMEOUT, Map.of());
    }

    /**
     * Returns a copy of these credentials with a different timeout.
     *
     * @param newTimeout The timeout to apply to a single request.
     * @return A new credentials instance; this one is unchanged.
     */
    public LlmCredentials withTimeout(Duration newTimeout) {
        return new LlmCredentials(vendor, apiKey, baseUrl, newTimeout, headers);
    }

    /**
     * Returns a copy of these credentials with an extra header added.
     *
     * @param name The header name.
     * @param value The header value.
     * @return A new credentials instance; this one is unchanged.
     */
    public LlmCredentials withHeader(String name, String value) {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(value, "value cannot be null");

        Map<String, String> merged = new java.util.LinkedHashMap<>(headers);
        merged.put(name, value);
        return new LlmCredentials(vendor, apiKey, baseUrl, timeout, merged);
    }

    /**
     * Renders these credentials with the API key redacted.
     *
     * <p>Overrides the record's generated {@code toString}, which would print
     * the key verbatim.</p>
     *
     * @return A description safe to write to a log.
     */
    @Override
    public String toString() {
        return "LlmCredentials[vendor=" + vendor
                + ", apiKey=***"
                + ", baseUrl=" + baseUrl
                + ", timeout=" + timeout
                + ", headers=" + headers.keySet()
                + ']';
    }
}
