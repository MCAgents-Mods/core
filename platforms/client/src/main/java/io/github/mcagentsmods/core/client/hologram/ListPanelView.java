package io.github.mcagentsmods.core.client.hologram;

import io.github.mcagentsmods.core.api.hologram.SelectList;
import io.github.mcagentsmods.core.common.hologram.ColumnLayout;
import io.github.mcagentsmods.core.common.hologram.PanelArea;
import io.github.mcagentsmods.core.common.hologram.ScrollWindow;

import java.util.List;

/** A {@link SelectList}: the rows, the chosen one highlighted, and the buttons under them. */
final class ListPanelView extends ColumnPanelView {

    private final SelectList spec;
    private final Actions actions;
    private final PanelArea box;
    private final ScrollWindow window;
    private boolean revealed;

    ListPanelView(SelectList spec, Actions actions) {
        super(spec.title());
        this.spec = spec;
        this.actions = actions;
        this.box = ColumnLayout.box(!spec.buttons().isEmpty());
        this.window = new ScrollWindow(ColumnLayout.capacity(box, ColumnLayout.ROW_HEIGHT), false);
    }

    @Override
    void draw(HologramPainter painter, double u, double v) {
        List<String> rows = read(spec.rows(), List.of());
        int selected = readInt(spec.selected(), -1);
        window.setTotal(rows.size());
        if (!revealed) {
            // Opened on the chosen row, wherever it is.
            window.reveal(selected);
            revealed = true;
        }

        drawColumn(painter);
        drawBox(painter, box, false);
        PanelArea band = ColumnLayout.rows(box);
        PanelArea content = ColumnLayout.content(box);
        int hovered = ColumnLayout.itemAt(box, ColumnLayout.ROW_HEIGHT, window, u, v);
        for (int slot = 0; slot < window.visible(); slot++) {
            int index = window.offset() + slot;
            if (index >= rows.size()) {
                break;
            }
            float top = (float) content.top() + slot * ColumnLayout.ROW_HEIGHT;
            float bottom = top + ColumnLayout.ROW_HEIGHT;
            if (index == selected) {
                painter.rect((float) band.left(), top, (float) band.right(), bottom, 4,
                        HologramStyle.ROW_SELECTED);
                painter.outline((float) band.left(), top, (float) band.right(), bottom, 5,
                        HologramStyle.BUTTON_EDGE);
            } else if (index == hovered) {
                painter.rect((float) band.left(), top, (float) band.right(), bottom, 4,
                        HologramStyle.ROW_HOVER);
            }
            String row = rows.get(index);
            painter.plainText(painter.fit(row == null ? "" : row, (float) content.width()),
                    (float) content.left(), top + 2, 6, HologramStyle.TEXT);
        }
        drawScrollbar(painter, box, window, u, v);

        drawFooter(painter, spec.buttons(), u, v);
    }

    @Override
    boolean click(double u, double v) {
        if (clickFooter(spec.buttons(), actions, u, v)) {
            return true;
        }
        if (clickScrollbar(box, window, u, v)) {
            return true;
        }
        int index = ColumnLayout.itemAt(box, ColumnLayout.ROW_HEIGHT, window, u, v);
        if (index >= 0) {
            actions.run("Choosing row " + index + " of " + title(), () -> spec.onSelect().accept(index));
            return true;
        }
        return false;
    }

    @Override
    void scroll(double u, double v, int rows) {
        window.scrollBy(rows);
    }
}
