package io.github.mcagentsmods.core.api.chat;

import java.util.Objects;

/**
 * One turn in a conversation: who said it, and what they said.
 *
 * <p>A message is immutable and carries no identity of its own — no id, no
 * timestamp, no conversation handle. The core stores no history, so a message
 * is meaningful only inside the {@link ChatRequest} it is sent with. A caller
 * that wants multi turn behavior keeps its own list and replays it.</p>
 *
 * @param role The author of this turn, never {@code null}.
 * @param content The text of this turn, never {@code null}. May be empty, which
 *                some vendors accept as a deliberate blank turn.
 */
public record ChatMessage(ChatRole role, String content) {

    /**
     * Validates the components.
     *
     * @throws NullPointerException When {@code role} or {@code content} is
     *                              {@code null}.
     */
    public ChatMessage {
        Objects.requireNonNull(role, "role cannot be null");
        Objects.requireNonNull(content, "content cannot be null");
    }

    /**
     * Creates a {@link ChatRole#SYSTEM} message.
     *
     * @param content The framing instructions.
     * @return The new message.
     */
    public static ChatMessage system(String content) {
        return new ChatMessage(ChatRole.SYSTEM, content);
    }

    /**
     * Creates a {@link ChatRole#USER} message.
     *
     * @param content The text to send.
     * @return The new message.
     */
    public static ChatMessage user(String content) {
        return new ChatMessage(ChatRole.USER, content);
    }

    /**
     * Creates a {@link ChatRole#ASSISTANT} message.
     *
     * <p>Used to replay a model's earlier reply as context, since the core
     * keeps no conversation state.</p>
     *
     * @param content The text the model produced.
     * @return The new message.
     */
    public static ChatMessage assistant(String content) {
        return new ChatMessage(ChatRole.ASSISTANT, content);
    }
}
