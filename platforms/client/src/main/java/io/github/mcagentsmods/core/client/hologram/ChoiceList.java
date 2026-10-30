package io.github.mcagentsmods.core.client.hologram;

import io.github.mcagentsmods.core.common.hologram.ColumnLayout;
import io.github.mcagentsmods.core.common.hologram.FormLayout;
import io.github.mcagentsmods.core.common.hologram.PanelArea;
import io.github.mcagentsmods.core.common.hologram.ScrollWindow;

import java.util.List;

/**
 * The open list of a form field's choices: where it stands, which row is under a point, its
 * scrollbar and how it is drawn. The form decides when it opens and closes and what a pick
 * does; this class only knows the list.
 */
final class ChoiceList {

    /** Layers the list is drawn on, over every row of the form. */
    private static final int LAYER = 9;

    private final List<String> choices;
    private final PanelArea area;
    private final ScrollWindow window;

    private ChoiceList(List<String> choices, PanelArea area, ScrollWindow window) {
        this.choices = choices;
        this.area = area;
        this.window = window;
    }

    /**
     * Opens the list under a field's box, scrolled so the current value shows; null when there
     * is nothing to choose from.
     */
    static ChoiceList open(PanelArea box, List<String> choices, String current) {
        if (choices.isEmpty()) {
            return null;
        }
        List<String> copy = List.copyOf(choices);
        PanelArea area = FormLayout.choices(box, copy.size(), FormLayout.bottom(false));
        ScrollWindow window = new ScrollWindow(FormLayout.choiceCapacity(area), false);
        window.setTotal(copy.size());
        int at = copy.indexOf(current);
        if (at >= 0) {
            window.reveal(at);
        }
        return new ChoiceList(copy, area, window);
    }

    boolean contains(double u, double v) {
        return area.contains(u, v);
    }

    void scroll(int amount) {
        window.scrollBy(amount);
    }

    /**
     * Handles a click inside the list: moves the scrollbar, or returns the choice clicked; null
     * when the click picked nothing.
     */
    String click(double u, double v) {
        if (scrolls() && ColumnLayout.scrollbar(area).contains(u, v)) {
            PanelArea track = ColumnLayout.scrollbar(area);
            window.scrollToTrack(v - track.top() - 1, (int) track.height() - 2, ColumnLayout.MIN_THUMB);
            return null;
        }
        int index = choiceAt(u, v);
        return index < 0 ? null : choices.get(index);
    }

    /** Draws the list, marking the field's current value and the row under the pointer. */
    void draw(HologramPainter painter, double u, double v, String current) {
        float left = (float) area.left();
        float top = (float) area.top();
        float right = (float) area.right();
        float bottom = (float) area.bottom();
        painter.rect(left, top, right, bottom, LAYER, HologramStyle.FOCUS);
        painter.rect(left + 1, top + 1, right - 1, bottom - 1, LAYER + 1, HologramStyle.BOX);
        int hovered = choiceAt(u, v);
        float rowRight = (float) rowRight();
        float room = rowRight - left - 2 * ColumnLayout.PADDING;
        for (int slot = 0; slot < window.visible(); slot++) {
            int index = window.offset() + slot;
            if (index >= choices.size()) {
                break;
            }
            float rowTop = top + 2 + slot * FormLayout.CHOICE_HEIGHT;
            String choice = choices.get(index);
            if (index == hovered || choice.equals(current)) {
                painter.rect(left + 1, rowTop, rowRight, rowTop + FormLayout.CHOICE_HEIGHT, LAYER + 2,
                        index == hovered ? HologramStyle.ROW_HOVER : HologramStyle.ROW_SELECTED);
            }
            painter.plainText(painter.fit(choice, room), left + ColumnLayout.PADDING, rowTop + 2,
                    LAYER + 3, HologramStyle.TEXT);
        }
        if (scrolls()) {
            drawScrollbar(painter, u, v);
        }
    }

    private void drawScrollbar(HologramPainter painter, double u, double v) {
        PanelArea track = ColumnLayout.scrollbar(area);
        float trackLeft = (float) track.left();
        float trackTop = (float) track.top();
        painter.rect(trackLeft, trackTop, (float) track.right(), (float) track.bottom(),
                LAYER + 2, HologramStyle.TRACK);
        int length = (int) track.height() - 2;
        float thumbTop = trackTop + 1 + window.thumbStart(length, ColumnLayout.MIN_THUMB);
        float thumbBottom = thumbTop + window.thumbLength(length, ColumnLayout.MIN_THUMB);
        painter.rect(trackLeft + 1, thumbTop, (float) track.right() - 1, thumbBottom, LAYER + 3,
                track.contains(u, v) ? HologramStyle.THUMB_HOVER : HologramStyle.THUMB);
    }

    /** Whether there are more choices than rows, so the list has a scrollbar. */
    private boolean scrolls() {
        return window.total() > window.visible();
    }

    private int choiceAt(double u, double v) {
        if (!area.contains(u, v) || u >= rowRight()) {
            return -1;
        }
        int slot = (int) ((v - area.top() - 2) / FormLayout.CHOICE_HEIGHT);
        int index = window.offset() + slot;
        return slot >= 0 && slot < window.visible() && index < choices.size() ? index : -1;
    }

    /** The right end of the choices' rows: short of the scrollbar when there is one. */
    private double rowRight() {
        return scrolls() ? ColumnLayout.scrollbar(area).left() : area.right() - 1;
    }
}
