package io.github.mcagentsmods.core.api.hologram;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A conversation to read: a heading over the entries, each a {@code <Speaker>} line and then
 * its text wrapped to the panel, with a blank line between entries, and up to
 * {@link #MAX_BUTTONS} buttons under them. The text scrolls, and stays at the bottom as entries
 * arrive while the player has not scrolled up.
 *
 * <p>Core keeps none of the conversation. It reads {@code entries} and {@code heading} every
 * frame on the client thread, so both suppliers must answer from memory at once.
 *
 * @param title   the title: a translation key, or the text itself. Shown at the top whenever
 *                {@code heading} gives nothing, and named in the log
 * @param entries the entries, oldest first; read every frame
 * @param heading what the top shows instead of {@code title}, such as the name of the
 *                conversation, which may change while the panel stands; read every frame, and
 *                {@code title} is shown while it gives null or a blank string
 * @param buttons the buttons under the entries, left to right; at most {@link #MAX_BUTTONS}
 */
public record Transcript(String title, Supplier<List<TranscriptEntry>> entries,
                         Supplier<String> heading, List<HologramButton> buttons) implements HologramPanel {

    /** The most buttons that fit under the entries. */
    public static final int MAX_BUTTONS = 3;

    public Transcript {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(entries, "entries");
        Objects.requireNonNull(heading, "heading");
        buttons = List.copyOf(buttons);
        if (buttons.size() > MAX_BUTTONS) {
            throw new IllegalArgumentException("A transcript has at most " + MAX_BUTTONS
                    + " buttons under it, not " + buttons.size());
        }
    }

    /**
     * A transcript under its title, with no buttons.
     *
     * @param title   the title at the top
     * @param entries the entries, oldest first
     */
    public Transcript(String title, Supplier<List<TranscriptEntry>> entries) {
        this(title, entries, () -> null, List.of());
    }
}
