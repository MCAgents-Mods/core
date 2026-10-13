package io.github.mcagentsmods.core.api.hologram;

import java.util.Objects;

/**
 * A button on a hologram.
 *
 * @param label  what the button says, shown in upper case: a translation key, or the text
 *               itself when there is no translation. Not blank.
 * @param action what pressing it does. It runs on the client thread, inside the click, so it
 *               must not block; an action that throws is logged and the hologram stays open.
 */
public record HologramButton(String label, Runnable action) {

    public HologramButton {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(action, "action");
        if (label.isBlank()) {
            throw new IllegalArgumentException("A hologram button needs a label");
        }
    }
}
