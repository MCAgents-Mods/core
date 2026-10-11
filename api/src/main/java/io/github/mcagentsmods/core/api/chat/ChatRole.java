package io.github.mcagentsmods.core.api.chat;

/**
 * Who authored a {@link ChatMessage}.
 *
 * <p>{@link #SYSTEM}, {@link #USER} and {@link #ASSISTANT} are the roles every
 * supported vendor understands. {@link #TOOL} exists only for the tool exchange
 * of {@link io.github.mcagentsmods.core.api.model.PromptMessage}, and a
 * {@link ChatMessage} refuses it. The wire name each vendor expects is carried by {@link #code()} rather than being
 * derived from {@link #name()}, so renaming a constant here can never silently
 * change what goes over the network.</p>
 */
public enum ChatRole {

    /**
     * Instructions that frame the whole conversation, rather than a turn in it.
     *
     * <p>Not every vendor accepts this role inside the message list. The
     * Anthropic Messages API, for one, takes framing instructions as a separate
     * top level field, so a client may lift these messages out of the list when
     * it builds its request.</p>
     */
    SYSTEM("system"),

    /**
     * A turn authored by the caller — in practice, the mod driving
     * the agent.
     */
    USER("user"),

    /**
     * A turn authored by the model. Used when replaying an earlier exchange
     * back to the model, since the core keeps no conversation history itself.
     */
    ASSISTANT("assistant"),

    /**
     * The result of a tool the model asked for.
     *
     * <p>A wire role only, and only meaningful in a
     * {@link io.github.mcagentsmods.core.api.model.PromptMessage} that names
     * the call it answers. A {@link ChatMessage} cannot carry that id, so it
     * refuses this role rather than sending a turn every vendor rejects.</p>
     */
    TOOL("tool");

    /**
     * The identifier this role is sent as. Fixed per constant at construction
     * and never derived from the constant's name.
     */
    private final String code;

    /**
     * Binds a role to the identifier it travels as.
     *
     * @param code The wire identifier, never {@code null}.
     */
    ChatRole(String code) {
        this.code = code;
    }

    /**
     * Returns the identifier this role is sent as.
     *
     * @return The wire identifier, for example {@code "assistant"}.
     */
    public String code() {
        return code;
    }

    /**
     * Resolves a role from the identifier a vendor sent back.
     *
     * <p>Matching ignores case and surrounding whitespace, because vendors are
     * not consistent about either.</p>
     *
     * @param code The wire identifier to resolve.
     * @return The matching role.
     * @throws IllegalArgumentException When {@code code} is {@code null} or
     *                                  matches no role.
     */
    public static ChatRole fromCode(String code) {
        if (code != null) {
            String normalized = code.trim();
            for (ChatRole role : values()) {
                if (role.code.equalsIgnoreCase(normalized)) {
                    return role;
                }
            }
        }
        throw new IllegalArgumentException("Unknown chat role: " + code);
    }
}
