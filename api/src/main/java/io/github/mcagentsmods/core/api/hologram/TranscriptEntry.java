package io.github.mcagentsmods.core.api.hologram;

import java.util.Objects;

/**
 * One entry of a {@link Transcript}.
 *
 * @param speaker who said it, shown as {@code <speaker>} on a line of its own, as given
 * @param text    what they said, shown as given; line breaks are kept
 */
public record TranscriptEntry(String speaker, String text) {

    public TranscriptEntry {
        Objects.requireNonNull(speaker, "speaker");
        Objects.requireNonNull(text, "text");
    }
}
