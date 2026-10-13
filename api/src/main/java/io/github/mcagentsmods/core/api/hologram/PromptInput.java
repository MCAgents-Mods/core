package io.github.mcagentsmods.core.api.hologram;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * A box the player types into, with a send button under it.
 *
 * <p>Clicking the box starts typing: the keys go to the box until the player presses Escape,
 * clicks, or sends. Enter sends and Shift+Enter starts a new line. Sending hands the text to
 * {@code onSend} and empties the box; it does nothing while the box holds only blanks or
 * {@code ready} answers {@code false}. The text being typed is kept in memory while the
 * hologram is open and is gone when it closes.
 *
 * @param title     the title at the top: a translation key, or the text itself
 * @param send      the send button's label: a translation key, or the text itself
 * @param maxLength the most characters the box takes, 1 to {@link #MAX_LENGTH}
 * @param ready     whether sending is allowed now, read every frame; the button is greyed
 *                  while it is not
 * @param onSend    called with the text when the player sends it, on the client thread. It
 *                  must not block; an {@code onSend} that throws is logged and the text stays
 */
public record PromptInput(String title, String send, int maxLength, BooleanSupplier ready,
                          Consumer<String> onSend) implements HologramPanel {

    /** The longest {@code maxLength} may be. */
    public static final int MAX_LENGTH = 32_000;

    /** What the shorter constructor uses for {@code maxLength}. */
    public static final int DEFAULT_MAX_LENGTH = 4_000;

    /** What the shorter constructor uses for {@code send}: "Send". */
    public static final String SEND = "hologram.mcagents_core.send";

    public PromptInput {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(send, "send");
        Objects.requireNonNull(ready, "ready");
        Objects.requireNonNull(onSend, "onSend");
        if (send.isBlank()) {
            throw new IllegalArgumentException("A prompt's send button needs a label");
        }
        if (maxLength < 1 || maxLength > MAX_LENGTH) {
            throw new IllegalArgumentException("maxLength must be 1 to " + MAX_LENGTH
                    + ", not " + maxLength);
        }
    }

    /**
     * A box of up to {@link #DEFAULT_MAX_LENGTH} characters under a {@link #SEND} button that
     * is always ready.
     *
     * @param title  the title at the top
     * @param onSend called with the text when the player sends it
     */
    public PromptInput(String title, Consumer<String> onSend) {
        this(title, SEND, DEFAULT_MAX_LENGTH, () -> true, onSend);
    }
}
