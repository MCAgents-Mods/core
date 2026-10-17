package io.github.mcagentsmods.core.common.hologram;

/**
 * The layout of the tall panels a hologram stands side by side — the lists, the transcript and
 * the prompt — in panel pixels from the panel's top left corner. Each has a title in a band
 * across the top, a sunken box under it with a scrollbar down its right side, and, when it has
 * buttons, a row of them along the bottom. A pick list's band is a nav bar instead: its title on
 * the left and its buttons on the right.
 *
 * <p>Drawing and clicking both read this class, so a row or a button is hit exactly where it is
 * drawn.
 */
public final class ColumnLayout {

    /** A tall panel's width. */
    public static final int WIDTH = 160;

    /** A tall panel's height. */
    public static final int HEIGHT = 230;

    /** The band the title sits in. */
    public static final PanelArea HEADER = new PanelArea(4, 4, WIDTH - 4, 20);

    /** The top of the title's text. */
    public static final int TITLE_TOP = 8;

    /** The left edge of the box and of the row of buttons. */
    public static final int EDGE = 6;

    /** The top of the box. */
    public static final int BOX_TOP = 24;

    /** The row of buttons along the bottom. */
    public static final PanelArea FOOTER = new PanelArea(EDGE, HEIGHT - 24, WIDTH - EDGE, HEIGHT - 6);

    /** The space between two buttons of the bottom row. */
    public static final int BUTTON_GAP = 4;

    /** The send button's width. */
    public static final int SEND_WIDTH = 44;

    /** The scrollbar's width, inside the box's right edge. */
    public static final int SCROLLBAR_WIDTH = 8;

    /** The shortest a scrollbar's thumb gets, so it stays easy to aim at. */
    public static final int MIN_THUMB = 8;

    /** The space between the box's edges and its text. */
    public static final int PADDING = 4;

    /** The height of one row of a list. */
    public static final int ROW_HEIGHT = 12;

    /** The height of one line of text. */
    public static final int LINE_HEIGHT = 10;

    /** A nav bar button's width. */
    public static final int NAV_BUTTON_WIDTH = 52;

    /**
     * The right end of a nav bar's buttons: clear of the close button, which sits in the
     * band's right end on a hologram's last panel.
     */
    public static final int NAV_RIGHT = WIDTH - 20;

    /** Where a nav bar's title starts. */
    public static final int NAV_TITLE_LEFT = 10;

    private ColumnLayout() {
    }

    /**
     * The sunken box under the title.
     *
     * @param footer whether a row of buttons runs along the bottom
     * @return the box, its one-pixel edge included
     */
    public static PanelArea box(boolean footer) {
        return new PanelArea(EDGE, BOX_TOP, WIDTH - EDGE, footer ? FOOTER.top() - 4 : HEIGHT - EDGE);
    }

    /**
     * The scrollbar's track, inside the box's right edge.
     *
     * @param box what {@link #box} gave
     * @return the track
     */
    public static PanelArea scrollbar(PanelArea box) {
        return new PanelArea(box.right() - 1 - SCROLLBAR_WIDTH, box.top() + 1, box.right() - 1,
                box.bottom() - 1);
    }

    /**
     * Where the box's rows or lines go: inside its edge and padding, left of the scrollbar.
     *
     * @param box what {@link #box} gave
     * @return the area
     */
    public static PanelArea content(PanelArea box) {
        return new PanelArea(box.left() + PADDING, box.top() + 3,
                scrollbar(box).left() - 3, box.bottom() - 2);
    }

    /**
     * How many rows or lines of a height fit in the box.
     *
     * @param box    what {@link #box} gave
     * @param height one row's or line's height
     * @return the count, at least 1
     */
    public static int capacity(PanelArea box, int height) {
        return Math.max(1, (int) (content(box).height() / height));
    }

    /**
     * The band a list's rows are highlighted across: the box's inside, left of the scrollbar,
     * from the first row's top to the last's bottom.
     *
     * @param box what {@link #box} gave
     * @return the band
     */
    public static PanelArea rows(PanelArea box) {
        PanelArea content = content(box);
        return new PanelArea(box.left() + 1, content.top(), scrollbar(box).left() - 1,
                content.bottom());
    }

    /**
     * Which row or line a point is on.
     *
     * @param box    what {@link #box} gave
     * @param height one row's or line's height
     * @param window the box's scroll window
     * @param u      the point's distance from the panel's left edge
     * @param v      the point's distance from the panel's top edge
     * @return the item's index in the whole content, or -1 when the point is on none
     */
    public static int itemAt(PanelArea box, int height, ScrollWindow window, double u, double v) {
        PanelArea content = content(box);
        if (!rows(box).contains(u, v)) {
            return -1;
        }
        int slot = (int) ((v - content.top()) / height);
        int item = window.offset() + slot;
        return slot < window.visible() && item < window.total() ? item : -1;
    }

    /**
     * One of the buttons along the bottom, sharing the row evenly.
     *
     * @param index the button, from the left
     * @param count how many share the row
     * @return the button
     */
    public static PanelArea footerButton(int index, int count) {
        if (count < 1 || index < 0 || index >= count) {
            throw new IllegalArgumentException("Button " + index + " of " + count);
        }
        double width = (FOOTER.width() - (count - 1) * BUTTON_GAP) / count;
        double left = FOOTER.left() + index * (width + BUTTON_GAP);
        return new PanelArea(left, FOOTER.top(), left + width, FOOTER.bottom());
    }

    /** @return the send button, at the right end of the bottom row */
    public static PanelArea sendButton() {
        return new PanelArea(FOOTER.right() - SEND_WIDTH, FOOTER.top(), FOOTER.right(), FOOTER.bottom());
    }

    /**
     * One of the buttons on a nav bar: the title band's right end, left of the close button,
     * the last button rightmost.
     *
     * @param index the button, from the left
     * @param count how many share the bar
     * @return the button
     */
    public static PanelArea navButton(int index, int count) {
        if (count < 1 || index < 0 || index >= count) {
            throw new IllegalArgumentException("Button " + index + " of " + count);
        }
        double right = NAV_RIGHT - (count - 1 - index) * (NAV_BUTTON_WIDTH + BUTTON_GAP);
        return new PanelArea(right - NAV_BUTTON_WIDTH, HEADER.top() + 2, right, HEADER.bottom() - 2);
    }

    /**
     * How much room a nav bar's title has, from {@link #NAV_TITLE_LEFT} to just short of the
     * first button.
     *
     * @param count how many buttons share the bar, 0 or more
     * @return the width, in panel pixels
     */
    public static double navTitleRoom(int count) {
        double end = count == 0 ? NAV_RIGHT : navButton(0, count).left() - BUTTON_GAP;
        return end - NAV_TITLE_LEFT;
    }
}
