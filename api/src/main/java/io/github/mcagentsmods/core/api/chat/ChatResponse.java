package io.github.mcagentsmods.core.api.chat;

import java.util.Objects;

/**
 * What a language model returned for one {@link ChatRequest}.
 *
 * <p>Only the first choice is carried. Every supported vendor can return
 * several candidate completions, but the core does not ask for more than one,
 * so a response maps to exactly one reply.</p>
 *
 * @param id The vendor's identifier for this exchange, or an empty string when
 *           the vendor returned none. Useful for correlating with a vendor
 *           dashboard, and for nothing else.
 * @param model The model that actually answered. This can differ from the model
 *              that was requested — OpenRouter in particular routes to whatever
 *              provider is serving, and reports what it picked here.
 * @param content The reply text, never {@code null}. Empty when the model
 *                produced no text, which happens when generation was cut off
 *                immediately.
 * @param finishReason Why generation stopped, as the vendor worded it — for
 *                     example {@code "stop"}, {@code "length"}, or
 *                     {@code "content_filter"}. Empty when none was reported.
 *                     Deliberately not an enum: the vocabulary differs per
 *                     vendor and grows without notice.
 * @param usage What the exchange cost, or {@link TokenUsage#UNKNOWN} when the
 *              vendor reported nothing.
 */
public record ChatResponse(String id, String model, String content, String finishReason, TokenUsage usage) {

    /**
     * Validates the components.
     *
     * @throws NullPointerException When any component is {@code null}.
     */
    public ChatResponse {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(model, "model cannot be null");
        Objects.requireNonNull(content, "content cannot be null");
        Objects.requireNonNull(finishReason, "finishReason cannot be null");
        Objects.requireNonNull(usage, "usage cannot be null");
    }

    /**
     * Reports whether generation stopped because the token limit was reached
     * rather than because the model finished.
     *
     * <p>A truncated reply is usually still worth showing, but it is not a
     * complete answer — a caller that parses the content should check this
     * first.</p>
     *
     * @return {@code true} when the vendor reported a length limit as the
     *         reason generation stopped.
     */
    public boolean isTruncated() {
        return finishReason.equalsIgnoreCase("length")
                || finishReason.equalsIgnoreCase("max_tokens");
    }

    /**
     * Returns the reply as an assistant turn, ready to be replayed as context
     * in a later request.
     *
     * <p>This is how a caller builds multi turn behavior on a core that stores
     * no history: keep the returned message, and include it in the next
     * request's message list.</p>
     *
     * @return A {@link ChatRole#ASSISTANT} message holding {@link #content()}.
     */
    public ChatMessage asMessage() {
        return ChatMessage.assistant(content);
    }
}
