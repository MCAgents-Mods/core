package io.github.mcagentsmods.core.api.hologram;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Text on a {@link Form}: a label, a note or a status line, wrapped to the panel's width.
 *
 * <p>The row is always {@code lines} lines tall, whatever the text, so the rows under it never
 * move; text that needs more lines ends in three dots, and a blank or null text leaves the room
 * empty.
 *
 * @param text  what it says, a translation key or the text itself; read every frame
 * @param tone  the colour it is drawn in; read every frame
 * @param lines how many lines it takes, 1 to {@link #MAX_LINES}
 */
public record FormText(Supplier<String> text, Supplier<FormTone> tone, int lines) implements FormRow {

    /** The most lines one text row takes. */
    public static final int MAX_LINES = 8;

    public FormText {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(tone, "tone");
        if (lines < 1 || lines > MAX_LINES) {
            throw new IllegalArgumentException("A text row takes 1 to " + MAX_LINES
                    + " lines, not " + lines);
        }
    }

    /**
     * A one-line label that never changes, such as a field's name.
     *
     * @param keyOrText a translation key, or the text itself
     * @return the row
     */
    public static FormText label(String keyOrText) {
        Objects.requireNonNull(keyOrText, "keyOrText");
        return new FormText(() -> keyOrText, () -> FormTone.TEXT, 1);
    }

    /**
     * Text that never changes.
     *
     * @param keyOrText a translation key, or the text itself
     * @param tone      its colour
     * @param lines     how many lines it takes
     * @return the row
     */
    public static FormText of(String keyOrText, FormTone tone, int lines) {
        Objects.requireNonNull(keyOrText, "keyOrText");
        Objects.requireNonNull(tone, "tone");
        return new FormText(() -> keyOrText, () -> tone, lines);
    }
}
