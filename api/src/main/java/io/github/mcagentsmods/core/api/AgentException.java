package io.github.mcagentsmods.core.api;

import io.github.mcagentsmods.core.api.llm.LlmVendor;

/**
 * What every failure reaching a language model arrives as.
 *
 * <p>One exception type covers the lot — a rejected key, a rate limit, a
 * malformed response, an unreachable host — because a caller on a Minecraft
 * server almost always handles them the same way: log it and move on. What
 * distinguishes them is {@link #statusCode()}, which carries the HTTP status
 * when the failure came back from the vendor.</p>
 *
 * <p>It is unchecked deliberately. These failures surface as a failed
 * {@link java.util.concurrent.CompletableFuture} rather than as something thrown
 * at a call site, and a checked type would force a {@code catch} on code that
 * never sees one.</p>
 */
public class AgentException extends RuntimeException {

    /**
     * Serialization identity. Fixed so a value serialized by one build
     * deserializes in the next.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The value {@link #statusCode()} takes when the failure never reached the
     * vendor — a DNS failure, a timeout, a malformed response.
     */
    public static final int NO_STATUS = -1;

    /**
     * The vendor the failing call was aimed at. Never {@code null}: every
     * failure the core raises happens while talking to one.
     */
    private final LlmVendor vendor;

    /**
     * The HTTP status the vendor answered with, or {@link #NO_STATUS} when the
     * call never got an answer.
     */
    private final int statusCode;

    /**
     * Creates an exception for a failure that never reached the vendor.
     *
     * @param vendor The vendor the call was aimed at.
     * @param message What went wrong.
     */
    public AgentException(LlmVendor vendor, String message) {
        this(vendor, message, NO_STATUS, null);
    }

    /**
     * Creates an exception wrapping a lower level failure.
     *
     * @param vendor The vendor the call was aimed at.
     * @param message What went wrong.
     * @param cause The underlying failure.
     */
    public AgentException(LlmVendor vendor, String message, Throwable cause) {
        this(vendor, message, NO_STATUS, cause);
    }

    /**
     * Creates an exception for a failure the vendor reported.
     *
     * @param vendor The vendor the call was aimed at.
     * @param message What went wrong, including the vendor's own error text
     *                where it supplied one.
     * @param statusCode The HTTP status the vendor answered with.
     */
    public AgentException(LlmVendor vendor, String message, int statusCode) {
        this(vendor, message, statusCode, null);
    }

    /**
     * Creates an exception with every detail supplied.
     *
     * @param vendor The vendor the call was aimed at.
     * @param message What went wrong.
     * @param statusCode The HTTP status the vendor answered with, or
     *                   {@link #NO_STATUS} when there was none.
     * @param cause The underlying failure, or {@code null} when there is none.
     */
    public AgentException(LlmVendor vendor, String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.vendor = vendor;
        this.statusCode = statusCode;
    }

    /**
     * Returns the vendor the failing call was aimed at.
     *
     * @return The vendor, never {@code null}.
     */
    public LlmVendor vendor() {
        return vendor;
    }

    /**
     * Returns the HTTP status the vendor answered with.
     *
     * @return The status, or {@link #NO_STATUS} when the call never reached the
     *         vendor.
     */
    public int statusCode() {
        return statusCode;
    }

    /**
     * Reports whether the vendor rejected the credentials.
     *
     * <p>HTTP 401 only. A 403 is a refusal of the request — a moderation flag, a
     * region, a permission — not of the key, and {@code MCAgentsProvider} deletes
     * a key it reads as rejected, so one flagged prompt answered with 403 would
     * otherwise delete every stored key in turn.</p>
     *
     * @return {@code true} for HTTP 401.
     */
    public boolean isAuthFailure() {
        return statusCode == 401;
    }

    /**
     * Reports whether the vendor rate limited the call.
     *
     * <p>The one failure worth treating differently: it says the request was
     * fine and simply came too fast, so backing off and trying later is
     * reasonable where retrying an auth failure never is.</p>
     *
     * @return {@code true} for HTTP 429.
     */
    public boolean isRateLimited() {
        return statusCode == 429;
    }
}
