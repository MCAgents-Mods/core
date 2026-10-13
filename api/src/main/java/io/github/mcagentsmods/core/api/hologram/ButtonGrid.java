package io.github.mcagentsmods.core.api.hologram;

import java.util.List;
import java.util.Objects;

/**
 * A menu: a large title over a grid of buttons, {@link #COLUMNS} to a row in the given order,
 * with {@link #VISIBLE_ROWS} rows showing. More rows than that scroll, with the mouse wheel or
 * the scrollbar beside them. Under the grid, core tells the player how to move the hologram
 * and when it goes away.
 *
 * @param title   the large title at the top: a translation key, or the text itself
 * @param buttons the buttons, left to right and then row by row; may be empty
 */
public record ButtonGrid(String title, List<HologramButton> buttons) implements HologramPanel {

    /** Buttons in one row. */
    public static final int COLUMNS = 5;

    /** Rows showing at once; the rest scroll. */
    public static final int VISIBLE_ROWS = 3;

    public ButtonGrid {
        Objects.requireNonNull(title, "title");
        buttons = List.copyOf(buttons);
    }
}
