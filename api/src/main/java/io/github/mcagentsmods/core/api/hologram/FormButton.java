package io.github.mcagentsmods.core.api.hologram;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * A button on a {@link Form}, along its bottom or in a {@link FormButtons} row.
 *
 * @param label   what the button says, shown in upper case: a translation key, or the text
 *                itself. Not blank.
 * @param enabled whether it can be pressed now, read every frame; a button that cannot is
 *                greyed and ignores clicks
 * @param action  what pressing it does, on the client thread, inside the click; it must not
 *                block, and one that throws is logged
 */
public record FormButton(String label, BooleanSupplier enabled, Runnable action) {

    public FormButton {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(enabled, "enabled");
        Objects.requireNonNull(action, "action");
        if (label.isBlank()) {
            throw new IllegalArgumentException("A form button needs a label");
        }
    }

    /**
     * A button that can always be pressed.
     *
     * @param label  what it says
     * @param action what pressing it does
     * @return the button
     */
    public static FormButton of(String label, Runnable action) {
        return new FormButton(label, () -> true, action);
    }
}
