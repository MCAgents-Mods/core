package io.github.mcagentsmods.core.api.button;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A button on the agents menu, the hologram the player shows with G.
 *
 * <p>Each mod built on core adds its own — chat adds the one that opens chat — so the
 * menu is where the player finds every agent feature they have installed.
 *
 * @param id     unique among buttons: lowercase letters, digits, dots, dashes or underscores,
 *               conventionally {@code {modid}.{name}}
 * @param label  a translation key, or the text itself when there is no translation; the menu
 *               shows it in upper case
 * @param order  where the button sits; lower comes first, and equal orders sort by id
 * @param action what pressing it does, run on the game's thread
 */
public record AgentButton(String id, String label, int order, Runnable action) {

    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]{1,64}");

    public AgentButton {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(action, "action");
        if (!ID.matcher(id).matches()) {
            throw new IllegalArgumentException("The button id '" + id + "' must be 1 to 64 "
                    + "lowercase letters, digits, dots, dashes or underscores");
        }
        if (label.isBlank()) {
            throw new IllegalArgumentException("The button '" + id + "' needs a label");
        }
    }
}
