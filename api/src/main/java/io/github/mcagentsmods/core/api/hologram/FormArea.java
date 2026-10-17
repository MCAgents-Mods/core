package io.github.mcagentsmods.core.api.hologram;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A box for several lines of text on a {@link Form}, such as instructions the player writes.
 * It takes all the height the other rows leave, wraps the text to its width and scrolls.
 *
 * <p>Clicking the box starts typing: Enter and Shift+Enter start a new line, and Escape, a click
 * or Tab stops. The mod holds the text: core shows {@code value} and hands every edit to
 * {@code onChange} while the player types.
 *
 * @param value     the text, read every frame while the player is not typing in it
 * @param onChange  called with the whole text after every edit, on the client thread
 * @param hint      what the empty box shows in grey: a translation key, or the text itself
 * @param maxLength the most characters the box takes, 1 to {@link #MAX_LENGTH}
 */
public record FormArea(Supplier<String> value, Consumer<String> onChange, String hint,
                       int maxLength) implements FormRow {

    /** The longest {@code maxLength} may be. */
    public static final int MAX_LENGTH = 64_000;

    public FormArea {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(onChange, "onChange");
        Objects.requireNonNull(hint, "hint");
        if (maxLength < 1 || maxLength > MAX_LENGTH) {
            throw new IllegalArgumentException("maxLength must be 1 to " + MAX_LENGTH
                    + ", not " + maxLength);
        }
    }
}
