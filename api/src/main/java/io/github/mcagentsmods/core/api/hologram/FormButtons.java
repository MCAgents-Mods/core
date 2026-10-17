package io.github.mcagentsmods.core.api.hologram;

import java.util.List;

/**
 * A row of up to {@link #MAX_BUTTONS} buttons across a {@link Form}, sharing its width, such as
 * a button that acts on the field above it.
 *
 * @param buttons the buttons, left to right; at least one
 */
public record FormButtons(List<FormButton> buttons) implements FormRow {

    /** The most buttons that share a row. */
    public static final int MAX_BUTTONS = 3;

    public FormButtons {
        buttons = List.copyOf(buttons);
        if (buttons.isEmpty() || buttons.size() > MAX_BUTTONS) {
            throw new IllegalArgumentException("A button row has 1 to " + MAX_BUTTONS
                    + " buttons, not " + buttons.size());
        }
    }

    /**
     * A row of the given buttons.
     *
     * @param buttons the buttons, left to right
     * @return the row
     */
    public static FormButtons of(FormButton... buttons) {
        return new FormButtons(List.of(buttons));
    }
}
