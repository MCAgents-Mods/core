package io.github.mcagentsmods.core.common.hologram;

import io.github.mcagentsmods.core.api.hologram.ButtonGrid;

/**
 * The layout and scrolling of a {@link ButtonGrid} panel, in panel pixels from its top left
 * corner: {@link ButtonGrid#COLUMNS} buttons to a row in their given order, a new row for every
 * five more, and {@link ButtonGrid#VISIBLE_ROWS} rows showing at once. The rows that do not fit
 * scroll, a whole row at a time.
 *
 * <p>Drawing and clicking both read this class, so a button is pressed exactly where it is
 * drawn. One instance belongs to one open panel and is used on the client thread only.
 */
public final class HologramGrid {

    /** The panel's width. */
    public static final int WIDTH = 250;

    /** The panel's height. */
    public static final int HEIGHT = 130;

    /** The left edge of the first column. */
    public static final int GRID_LEFT = 10;

    /** The top edge of the first visible row. */
    public static final int GRID_TOP = 36;

    /** One button's width. */
    public static final int BUTTON_WIDTH = 40;

    /** One button's height. */
    public static final int BUTTON_HEIGHT = 20;

    /** The space between two buttons of a row. */
    public static final int COLUMN_GAP = 4;

    /** The space between two rows. */
    public static final int ROW_GAP = 5;

    /** The height of the visible rows together. */
    public static final int GRID_HEIGHT =
            ButtonGrid.VISIBLE_ROWS * BUTTON_HEIGHT + (ButtonGrid.VISIBLE_ROWS - 1) * ROW_GAP;

    /** The scrollbar's left edge. */
    public static final int SCROLLBAR_LEFT = GRID_LEFT
            + ButtonGrid.COLUMNS * BUTTON_WIDTH + (ButtonGrid.COLUMNS - 1) * COLUMN_GAP + 6;

    /** The scrollbar's width. */
    public static final int SCROLLBAR_WIDTH = 8;

    /** The shortest the scrollbar's thumb gets, so it stays easy to aim at. */
    public static final int MIN_THUMB = 8;

    private final int buttons;
    private int scroll;

    /**
     * A grid of {@code buttons} buttons, scrolled to the top.
     *
     * @param buttons how many buttons the panel has
     */
    public HologramGrid(int buttons) {
        if (buttons < 0) {
            throw new IllegalArgumentException("buttons must not be negative");
        }
        this.buttons = buttons;
    }

    /** @return how many buttons the panel has */
    public int buttons() {
        return buttons;
    }

    /** @return how many rows the buttons fill */
    public int rows() {
        return (buttons + ButtonGrid.COLUMNS - 1) / ButtonGrid.COLUMNS;
    }

    /** @return the furthest the grid scrolls: the rows that do not fit */
    public int maxScroll() {
        return Math.max(0, rows() - ButtonGrid.VISIBLE_ROWS);
    }

    /** @return the first visible row */
    public int scroll() {
        return scroll;
    }

    /**
     * Scrolls by whole rows, stopping at either end.
     *
     * @param rows how far, positive towards the later rows
     * @return whether the grid moved
     */
    public boolean scrollBy(int rows) {
        int before = scroll;
        scrollTo((long) scroll + rows);
        return scroll != before;
    }

    /**
     * Scrolls so {@code row} is the first visible row, as near as the ends allow.
     *
     * @param row the row wanted first
     */
    public void scrollTo(long row) {
        scroll = (int) Math.max(0, Math.min(maxScroll(), row));
    }

    /**
     * The left edge of a column.
     *
     * @param column 0 to {@link ButtonGrid#COLUMNS} - 1
     * @return panel pixels
     */
    public static int buttonLeft(int column) {
        return GRID_LEFT + column * (BUTTON_WIDTH + COLUMN_GAP);
    }

    /**
     * The top edge of a visible row.
     *
     * @param visibleRow 0 to {@link ButtonGrid#VISIBLE_ROWS} - 1
     * @return panel pixels
     */
    public static int buttonTop(int visibleRow) {
        return GRID_TOP + visibleRow * (BUTTON_HEIGHT + ROW_GAP);
    }

    /**
     * The button shown in a visible slot, at the current scroll.
     *
     * @param column     the column
     * @param visibleRow the row on screen, counted from the top visible one
     * @return the button's index, or -1 when the slot is empty
     */
    public int indexAt(int column, int visibleRow) {
        if (column < 0 || column >= ButtonGrid.COLUMNS
                || visibleRow < 0 || visibleRow >= ButtonGrid.VISIBLE_ROWS) {
            return -1;
        }
        long index = (long) (scroll + visibleRow) * ButtonGrid.COLUMNS + column;
        return index < buttons ? (int) index : -1;
    }

    /**
     * The button under a point, at the current scroll. The gaps between buttons belong to no
     * button.
     *
     * @param u panel pixels from the left
     * @param v panel pixels from the top
     * @return the button's index, or -1
     */
    public int buttonAt(double u, double v) {
        for (int row = 0; row < ButtonGrid.VISIBLE_ROWS; row++) {
            int top = buttonTop(row);
            if (v < top || v >= top + BUTTON_HEIGHT) {
                continue;
            }
            for (int column = 0; column < ButtonGrid.COLUMNS; column++) {
                int left = buttonLeft(column);
                if (u >= left && u < left + BUTTON_WIDTH) {
                    return indexAt(column, row);
                }
            }
        }
        return -1;
    }

    /**
     * Whether a point is on the scrollbar's track.
     *
     * @param u panel pixels from the left
     * @param v panel pixels from the top
     * @return {@code true} on the track
     */
    public static boolean onScrollbar(double u, double v) {
        return u >= SCROLLBAR_LEFT && u < SCROLLBAR_LEFT + SCROLLBAR_WIDTH
                && v >= GRID_TOP && v < GRID_TOP + GRID_HEIGHT;
    }

    /** @return the thumb's height: the visible share of the rows, never under {@link #MIN_THUMB} */
    public int thumbHeight() {
        int rows = Math.max(rows(), ButtonGrid.VISIBLE_ROWS);
        return Math.max(MIN_THUMB, GRID_HEIGHT * ButtonGrid.VISIBLE_ROWS / rows);
    }

    /** @return the thumb's top edge, in panel pixels */
    public int thumbTop() {
        int travel = GRID_HEIGHT - thumbHeight();
        return maxScroll() == 0 ? GRID_TOP : GRID_TOP + travel * scroll / maxScroll();
    }

    /**
     * Scrolls so the thumb's middle sits as near {@code v} as whole rows allow, which is what a
     * click on the track does.
     *
     * @param v panel pixels from the top
     */
    public void scrollToTrack(double v) {
        int travel = GRID_HEIGHT - thumbHeight();
        if (travel <= 0 || maxScroll() == 0) {
            return;
        }
        double share = (v - GRID_TOP - thumbHeight() / 2.0) / travel;
        scrollTo(Math.round(Math.max(0, Math.min(1, share)) * maxScroll()));
    }
}
