package io.github.mcagentsmods.core.client.hologram;

import io.github.mcagentsmods.core.api.hologram.HologramButton;
import io.github.mcagentsmods.core.api.hologram.PickList;
import io.github.mcagentsmods.core.api.hologram.PickRow;
import io.github.mcagentsmods.core.common.hologram.ColumnLayout;
import io.github.mcagentsmods.core.common.hologram.PanelArea;
import io.github.mcagentsmods.core.common.hologram.ScrollWindow;

import java.util.List;

/**
 * A {@link PickList}: a nav bar with the title on the left and the buttons on the right, then
 * the rows, those the player cannot pick faded and deaf to clicks.
 */
final class PickListPanelView extends ColumnPanelView {

    private final PickList spec;
    private final Actions actions;
    private final PanelArea box = ColumnLayout.box(false);
    private final ScrollWindow window;

    /** The rows last read, so a click lands on the row that was drawn. */
    private List<PickRow> rows = List.of();

    PickListPanelView(PickList spec, Actions actions) {
        super(spec.title());
        this.spec = spec;
        this.actions = actions;
        this.window = new ScrollWindow(ColumnLayout.capacity(box, ColumnLayout.ROW_HEIGHT), false);
    }

    @Override
    void draw(HologramPainter painter, double u, double v) {
        rows = read(spec.rows(), List.of());
        window.setTotal(rows.size());

        drawBand(painter);
        List<HologramButton> buttons = spec.navButtons();
        String title = label(title());
        float room = (float) ColumnLayout.navTitleRoom(buttons.size());
        float scale = Math.max(0.5f, Math.min(1, room / painter.width(title)));
        painter.text(painter.fit(title, room / scale), ColumnLayout.NAV_TITLE_LEFT,
                ColumnLayout.TITLE_TOP + (8 - 8 * scale) / 2, 4, scale, HologramStyle.TEXT, 0);
        for (int i = 0; i < buttons.size(); i++) {
            PanelArea area = ColumnLayout.navButton(i, buttons.size());
            drawButton(painter, area, label(buttons.get(i).label()), area.contains(u, v), true);
        }

        drawBox(painter, box, false);
        PanelArea band = ColumnLayout.rows(box);
        PanelArea content = ColumnLayout.content(box);
        int hovered = ColumnLayout.itemAt(box, ColumnLayout.ROW_HEIGHT, window, u, v);
        for (int slot = 0; slot < window.visible(); slot++) {
            int index = window.offset() + slot;
            if (index >= rows.size()) {
                break;
            }
            PickRow row = rows.get(index);
            if (row == null) {
                continue;
            }
            float top = (float) content.top() + slot * ColumnLayout.ROW_HEIGHT;
            float bottom = top + ColumnLayout.ROW_HEIGHT;
            if (!row.enabled()) {
                painter.rect((float) band.left(), top, (float) band.right(), bottom, 4,
                        HologramStyle.ROW_FADED);
            } else if (index == hovered) {
                painter.rect((float) band.left(), top, (float) band.right(), bottom, 4,
                        HologramStyle.ROW_HOVER);
            }
            painter.plainText(painter.fit(row.label(), (float) content.width()),
                    (float) content.left(), top + 2, 6,
                    row.enabled() ? HologramStyle.TEXT : HologramStyle.TEXT_FADED);
        }
        drawScrollbar(painter, box, window, u, v);
    }

    @Override
    boolean click(double u, double v) {
        List<HologramButton> buttons = spec.navButtons();
        for (int i = 0; i < buttons.size(); i++) {
            if (ColumnLayout.navButton(i, buttons.size()).contains(u, v)) {
                HologramButton button = buttons.get(i);
                actions.run("The hologram button " + button.label(), button.action());
                return true;
            }
        }
        if (clickScrollbar(box, window, u, v)) {
            return true;
        }
        int index = ColumnLayout.itemAt(box, ColumnLayout.ROW_HEIGHT, window, u, v);
        if (index < 0 || index >= rows.size()) {
            return false;
        }
        PickRow row = rows.get(index);
        if (row == null || !row.enabled()) {
            // Faded: it stays in the list, but there is nothing to pick.
            return true;
        }
        actions.run("Picking row " + index + " of " + title(), () -> spec.onPick().accept(index));
        return true;
    }

    @Override
    void scroll(double u, double v, int rows) {
        window.scrollBy(rows);
    }
}
