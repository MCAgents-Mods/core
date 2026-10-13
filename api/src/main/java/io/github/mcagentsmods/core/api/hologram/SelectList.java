package io.github.mcagentsmods.core.api.hologram;

import java.util.List;
import java.util.Objects;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * A list the player picks one row from: a title over a scrolling list of rows, the chosen row
 * highlighted, and up to {@link #MAX_BUTTONS} buttons under it.
 *
 * <p>Core keeps none of the list. It reads {@code rows} and {@code selected} every frame on the
 * client thread, so both must answer from memory at once; the calling mod changes what they
 * return and the panel follows.
 *
 * @param title    the title at the top: a translation key, or the text itself
 * @param rows     the rows, top to bottom, shown as given; read every frame
 * @param selected the index of the chosen row, or -1 for none; read every frame
 * @param onSelect called with a row's index when the player clicks it, on the client thread
 * @param buttons  the buttons under the list, left to right; at most {@link #MAX_BUTTONS}
 */
public record SelectList(String title, Supplier<List<String>> rows, IntSupplier selected,
                         IntConsumer onSelect, List<HologramButton> buttons) implements HologramPanel {

    /** The most buttons that fit under the list. */
    public static final int MAX_BUTTONS = 3;

    public SelectList {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(rows, "rows");
        Objects.requireNonNull(selected, "selected");
        Objects.requireNonNull(onSelect, "onSelect");
        buttons = List.copyOf(buttons);
        if (buttons.size() > MAX_BUTTONS) {
            throw new IllegalArgumentException("A list has at most " + MAX_BUTTONS
                    + " buttons under it, not " + buttons.size());
        }
    }

    /**
     * A list with no buttons under it.
     *
     * @param title    the title at the top
     * @param rows     the rows, top to bottom
     * @param selected the index of the chosen row, or -1
     * @param onSelect called with a clicked row's index
     */
    public SelectList(String title, Supplier<List<String>> rows, IntSupplier selected,
                      IntConsumer onSelect) {
        this(title, rows, selected, onSelect, List.of());
    }
}
