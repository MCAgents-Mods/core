package io.github.mcagentsmods.core.api.hologram;

import java.util.Objects;

/**
 * One row of a {@link PickList}.
 *
 * @param label   what the row says, shown as given
 * @param enabled whether the player can pick it; a row that cannot is drawn faded and a click
 *                on it does nothing
 */
public record PickRow(String label, boolean enabled) {

    public PickRow {
        Objects.requireNonNull(label, "label");
    }

    /**
     * A row the player can pick.
     *
     * @param label what the row says
     * @return the row
     */
    public static PickRow of(String label) {
        return new PickRow(label, true);
    }
}
