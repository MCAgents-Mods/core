package io.github.mcagentsmods.core.common.hologram;

import java.util.ArrayList;
import java.util.List;

/**
 * The layout of a form panel, in panel pixels from the panel's top left corner: the title band
 * of the tall panels, then the rows stacked top to bottom with a small gap between them, then,
 * when it has buttons, a row of them along the bottom.
 *
 * <p>Drawing and clicking both read this class, so a box, an arrow or a button is hit exactly
 * where it is drawn.
 */
public final class FormLayout {

    /** A normal form's width: the tall panels' width. */
    public static final int NORMAL_WIDTH = ColumnLayout.WIDTH;

    /** A wide form's width. */
    public static final int WIDE_WIDTH = 240;

    /** A form's height: the tall panels' height. */
    public static final int HEIGHT = ColumnLayout.HEIGHT;

    /** The left edge of the rows, and the space on their right. */
    public static final int EDGE = ColumnLayout.EDGE;

    /** The top of the first row, under the title band. */
    public static final int TOP = ColumnLayout.BOX_TOP + 2;

    /** The space between two rows. */
    public static final int GAP = 3;

    /** The height of one line of text. */
    public static final int LINE = ColumnLayout.LINE_HEIGHT;

    /** The height of a one-line box. */
    public static final int BOX = 14;

    /** The height of a button. */
    public static final int BUTTON = 16;

    /** The height of a field with a label: the label's line over the box. */
    public static final int LABELLED_BOX = LINE + BOX;

    /** The width of a field's arrow, inside the box's right end. */
    public static final int ARROW_WIDTH = 12;

    /** The width of a stepper's minus and plus buttons. */
    public static final int STEP_WIDTH = 14;

    /** The width of the number between them. */
    public static final int NUMBER_WIDTH = 24;

    /** The least height an area gets, whatever the other rows take: three lines and its edges. */
    public static final int MIN_AREA = 3 * LINE + 8;

    /** The height of one choice in a field's list. */
    public static final int CHOICE_HEIGHT = ColumnLayout.ROW_HEIGHT;

    /** The most choices a field's list shows at once; the rest scroll. */
    public static final int MAX_CHOICES = 8;

    private FormLayout() {
    }

    /**
     * @param wide whether the form is wide
     * @return the form's width
     */
    public static int width(boolean wide) {
        return wide ? WIDE_WIDTH : NORMAL_WIDTH;
    }

    /**
     * The lowest any row may reach.
     *
     * @param footer whether buttons run along the bottom
     * @return the bottom edge
     */
    public static double bottom(boolean footer) {
        return footer ? ColumnLayout.FOOTER.top() - 4 : HEIGHT - EDGE;
    }

    /**
     * Stacks the rows top to bottom.
     *
     * @param width   the form's width
     * @param heights each row's height, top to bottom; 0 for the one row that takes what the
     *                others leave, at least {@link #MIN_AREA}
     * @param footer  whether buttons run along the bottom
     * @return each row's area, in the same order; a row whose area reaches below
     *         {@link #bottom} does not fit
     */
    public static List<PanelArea> stack(int width, int[] heights, boolean footer) {
        double room = bottom(footer) - TOP;
        double fixed = 0;
        for (int height : heights) {
            fixed += height;
        }
        fixed += Math.max(0, heights.length - 1) * GAP;
        double fill = Math.max(MIN_AREA, room - fixed);
        List<PanelArea> areas = new ArrayList<>(heights.length);
        double top = TOP;
        for (int height : heights) {
            double tall = height > 0 ? height : fill;
            areas.add(new PanelArea(EDGE, top, width - EDGE, top + tall));
            top += tall + GAP;
        }
        return areas;
    }

    /**
     * Whether a row fits above the bottom.
     *
     * @param row    what {@link #stack} gave for it
     * @param footer whether buttons run along the bottom
     * @return whether it is drawn
     */
    public static boolean fits(PanelArea row, boolean footer) {
        return row.bottom() <= bottom(footer);
    }

    /**
     * One of the buttons along the bottom, sharing the row evenly.
     *
     * @param width the form's width
     * @param index the button, from the left
     * @param count how many share the row
     * @return the button
     */
    public static PanelArea footerButton(int width, int index, int count) {
        PanelArea footer = new PanelArea(EDGE, ColumnLayout.FOOTER.top(), width - EDGE,
                ColumnLayout.FOOTER.bottom());
        return share(footer, index, count);
    }

    /**
     * One of several buttons sharing an area evenly, left to right.
     *
     * @param area  the area they share
     * @param index the button, from the left
     * @param count how many share it
     * @return the button
     */
    public static PanelArea share(PanelArea area, int index, int count) {
        if (count < 1 || index < 0 || index >= count) {
            throw new IllegalArgumentException("Button " + index + " of " + count);
        }
        double width = (area.width() - (count - 1) * ColumnLayout.BUTTON_GAP) / count;
        double left = area.left() + index * (width + ColumnLayout.BUTTON_GAP);
        return new PanelArea(left, area.top(), left + width, area.bottom());
    }

    /**
     * A field's box: the bottom of its row, under the label when it has one.
     *
     * @param row what {@link #stack} gave for the field
     * @return the box, its one-pixel edge included
     */
    public static PanelArea box(PanelArea row) {
        return new PanelArea(row.left(), row.bottom() - BOX, row.right(), row.bottom());
    }

    /**
     * A field's arrow, inside its box's right end.
     *
     * @param box what {@link #box} gave
     * @return the arrow
     */
    public static PanelArea arrow(PanelArea box) {
        return new PanelArea(box.right() - 1 - ARROW_WIDTH, box.top() + 1, box.right() - 1,
                box.bottom() - 1);
    }

    /**
     * Where a field's text goes: inside its edge and padding, left of the arrow when it has one.
     *
     * @param box   what {@link #box} gave
     * @param arrow whether the field has an arrow
     * @return the area
     */
    public static PanelArea text(PanelArea box, boolean arrow) {
        double right = arrow ? arrow(box).left() - 2 : box.right() - ColumnLayout.PADDING;
        return new PanelArea(box.left() + ColumnLayout.PADDING, box.top() + 3, right, box.bottom() - 2);
    }

    /**
     * A stepper's minus button, left of its number.
     *
     * @param row what {@link #stack} gave for the stepper
     * @return the button
     */
    public static PanelArea minus(PanelArea row) {
        double right = number(row).left();
        return new PanelArea(right - STEP_WIDTH, row.top(), right, row.bottom());
    }

    /**
     * Where a stepper's number is drawn, between its two buttons.
     *
     * @param row what {@link #stack} gave for the stepper
     * @return the area
     */
    public static PanelArea number(PanelArea row) {
        double right = plus(row).left();
        return new PanelArea(right - NUMBER_WIDTH, row.top(), right, row.bottom());
    }

    /**
     * A stepper's plus button, at the row's right end.
     *
     * @param row what {@link #stack} gave for the stepper
     * @return the button
     */
    public static PanelArea plus(PanelArea row) {
        return new PanelArea(row.right() - STEP_WIDTH, row.top(), row.right(), row.bottom());
    }

    /**
     * Where a field's list of choices opens: under its box, as wide as the box and as tall as
     * the choices, up to {@link #MAX_CHOICES} of them. When fewer than three fit under the box
     * before {@code bottom}, it opens above the box instead.
     *
     * @param box    what {@link #box} gave
     * @param count  how many choices there are
     * @param bottom the lowest the list may reach
     * @return the list, its one-pixel edge included
     */
    public static PanelArea choices(PanelArea box, int count, double bottom) {
        int wanted = Math.max(1, Math.min(MAX_CHOICES, count));
        double below = bottom - box.bottom();
        double above = box.top() - TOP;
        if (below >= rowsHeight(Math.min(3, wanted)) || below >= above) {
            int rows = Math.max(1, Math.min(wanted, (int) ((below - 4) / CHOICE_HEIGHT)));
            return new PanelArea(box.left(), box.bottom(), box.right(), box.bottom() + rowsHeight(rows));
        }
        int rows = Math.max(1, Math.min(wanted, (int) ((above - 4) / CHOICE_HEIGHT)));
        return new PanelArea(box.left(), box.top() - rowsHeight(rows), box.right(), box.top());
    }

    /**
     * How many choices a list opened by {@link #choices} shows at once.
     *
     * @param list what {@link #choices} gave
     * @return the count, at least 1
     */
    public static int choiceCapacity(PanelArea list) {
        return Math.max(1, (int) ((list.height() - 4) / CHOICE_HEIGHT));
    }

    private static double rowsHeight(int rows) {
        return rows * CHOICE_HEIGHT + 4;
    }
}
