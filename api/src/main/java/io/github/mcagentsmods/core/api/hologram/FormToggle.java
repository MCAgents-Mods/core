package io.github.mcagentsmods.core.api.hologram;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * An on and off switch on a {@link Form}: a button across the panel that says its label and
 * which state it is in, ON in green or OFF in red. Pressing it calls {@code flip}; the mod
 * changes what {@code on} answers.
 *
 * @param label what it switches: a translation key, or the text itself. Not blank.
 * @param on    whether it is on now, read every frame
 * @param flip  called when the player presses it, on the client thread
 */
public record FormToggle(String label, BooleanSupplier on, Runnable flip) implements FormRow {

    public FormToggle {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(on, "on");
        Objects.requireNonNull(flip, "flip");
        if (label.isBlank()) {
            throw new IllegalArgumentException("A toggle needs a label");
        }
    }
}
