package io.github.mcagentsmods.core.api.chat;

/**
 * What one exchange cost, counted in tokens.
 *
 * <p>Vendors do not all report the same fields, and some omit usage entirely
 * for streamed or cached responses. {@link #UNKNOWN} is the value used when
 * nothing was reported, so a caller never has to null check a response's
 * usage.</p>
 *
 * @param promptTokens Tokens consumed by the request, or {@code -1} when the
 *                     vendor did not report it.
 * @param completionTokens Tokens produced in the reply, or {@code -1} when the
 *                         vendor did not report it.
 * @param totalTokens Tokens billed for the exchange, or {@code -1} when the
 *                    vendor did not report it. Not always the sum of the other
 *                    two — cached or reasoning tokens can be billed
 *                    differently.
 */
public record TokenUsage(int promptTokens, int completionTokens, int totalTokens) {

    /**
     * The value used when a vendor reported no usage at all. Every count is
     * {@code -1}, which {@link #isKnown()} reads as unreported.
     */
    public static final TokenUsage UNKNOWN = new TokenUsage(-1, -1, -1);

    /**
     * Reports whether the vendor gave a usable total.
     *
     * @return {@code true} when {@link #totalTokens()} was actually reported.
     */
    public boolean isKnown() {
        return totalTokens >= 0;
    }
}
