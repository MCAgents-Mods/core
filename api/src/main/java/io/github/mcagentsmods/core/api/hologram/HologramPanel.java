package io.github.mcagentsmods.core.api.hologram;

/**
 * One panel of a {@link Hologram}. Each kind is a record; core draws it and handles the
 * player's clicks and scrolling on it.
 */
public sealed interface HologramPanel permits ButtonGrid, SelectList, Transcript, PromptInput {

    /**
     * The panel's title, shown at its top in upper case: a translation key, or the text itself
     * when there is no translation.
     *
     * @return the title
     */
    String title();
}
