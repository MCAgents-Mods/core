package io.github.mcagentsmods.core.api.hologram;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A conversation to read: a title over the entries, each a {@code <Speaker>} line and then its
 * text wrapped to the panel, with a blank line between entries. The text scrolls, and stays at
 * the bottom as entries arrive while the player has not scrolled up.
 *
 * <p>Core keeps none of the conversation. It reads {@code entries} every frame on the client
 * thread, so the supplier must answer from memory at once.
 *
 * @param title   the title at the top: a translation key, or the text itself
 * @param entries the entries, oldest first; read every frame
 */
public record Transcript(String title, Supplier<List<TranscriptEntry>> entries) implements HologramPanel {

    public Transcript {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(entries, "entries");
    }
}
