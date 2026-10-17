package io.github.mcagentsmods.core.api.hologram;

import java.util.List;
import java.util.Objects;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/**
 * A list to pick from, with a nav bar: the title on the left of the bar at the top and up to
 * {@link #MAX_NAV_BUTTONS} buttons on its right, then a scrolling list of rows. A row that is
 * not enabled is drawn faded and ignores clicks; it stays in its place in the list.
 *
 * <p>Unlike a {@link SelectList}, nothing stays chosen: a click on a row only tells the mod
 * which one. Core keeps none of the list. It reads {@code rows} every frame on the client
 * thread, so the supplier must answer from memory at once; the calling mod changes what it
 * returns, a row's {@code enabled} included, and the panel follows.
 *
 * @param title      the title on the left of the nav bar: a translation key, or the text itself
 * @param rows       the rows, top to bottom; read every frame
 * @param onPick     called with a row's index when the player clicks an enabled row, on the
 *                   client thread
 * @param navButtons the buttons on the right of the nav bar, left to right; at most
 *                   {@link #MAX_NAV_BUTTONS}
 */
public record PickList(String title, Supplier<List<PickRow>> rows, IntConsumer onPick,
                       List<HologramButton> navButtons) implements HologramPanel {

    /** The most buttons that fit on the nav bar beside the title. */
    public static final int MAX_NAV_BUTTONS = 2;

    public PickList {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(rows, "rows");
        Objects.requireNonNull(onPick, "onPick");
        navButtons = List.copyOf(navButtons);
        if (navButtons.size() > MAX_NAV_BUTTONS) {
            throw new IllegalArgumentException("A pick list has at most " + MAX_NAV_BUTTONS
                    + " buttons on its nav bar, not " + navButtons.size());
        }
    }

    /**
     * A pick list with nothing on its nav bar but the title.
     *
     * @param title  the title
     * @param rows   the rows, top to bottom
     * @param onPick called with a clicked row's index
     */
    public PickList(String title, Supplier<List<PickRow>> rows, IntConsumer onPick) {
        this(title, rows, onPick, List.of());
    }
}
