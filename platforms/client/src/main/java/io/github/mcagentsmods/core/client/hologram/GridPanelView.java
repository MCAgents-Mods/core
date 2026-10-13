package io.github.mcagentsmods.core.client.hologram;

import io.github.mcagentsmods.core.api.hologram.ButtonGrid;
import io.github.mcagentsmods.core.api.hologram.HologramButton;
import io.github.mcagentsmods.core.common.hologram.HologramGrid;
import io.github.mcagentsmods.core.common.hologram.PanelArea;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** A {@link ButtonGrid}: the large title, the buttons, the scrollbar and the hint under them. */
final class GridPanelView extends PanelView {

    private static final int TITLE_TOP = 8;
    private static final float TITLE_SCALE = 2;
    private static final int RULE_TOP = 29;
    private static final int HINT_TOP = 114;
    private static final float HINT_SCALE = 0.75f;

    private final ButtonGrid spec;
    private final HologramGrid grid;
    private final Supplier<String> hint;
    private final Consumer<HologramButton> press;

    /**
     * @param spec  the panel
     * @param hint  the line under the grid, read each frame so it follows the key binding
     * @param press runs a pressed button's action
     */
    GridPanelView(ButtonGrid spec, Supplier<String> hint, Consumer<HologramButton> press) {
        this.spec = spec;
        this.grid = new HologramGrid(spec.buttons().size());
        this.hint = hint;
        this.press = press;
    }

    @Override
    int width() {
        return HologramGrid.WIDTH;
    }

    @Override
    int height() {
        return HologramGrid.HEIGHT;
    }

    @Override
    void draw(HologramPainter painter, double u, double v) {
        drawFrame(painter);
        float middle = width() / 2f;
        painter.centred(label(spec.title()), middle, TITLE_TOP, 4, TITLE_SCALE, HologramStyle.TITLE,
                HologramStyle.TITLE_OUTLINE);
        painter.rect(40, RULE_TOP, width() - 40, RULE_TOP + 1, 3, HologramStyle.RULE);
        painter.rect(40, RULE_TOP + 1, width() - 40, RULE_TOP + 2, 3, HologramStyle.FRAME_LIGHT);

        List<HologramButton> buttons = spec.buttons();
        int hovered = grid.buttonAt(u, v);
        for (int row = 0; row < ButtonGrid.VISIBLE_ROWS; row++) {
            for (int column = 0; column < ButtonGrid.COLUMNS; column++) {
                int index = grid.indexAt(column, row);
                if (index >= 0) {
                    int left = HologramGrid.buttonLeft(column);
                    int top = HologramGrid.buttonTop(row);
                    PanelArea area = new PanelArea(left, top, left + HologramGrid.BUTTON_WIDTH,
                            top + HologramGrid.BUTTON_HEIGHT);
                    drawButton(painter, area, label(buttons.get(index).label()), index == hovered, true);
                }
            }
        }
        drawScrollbar(painter, u, v);
        painter.centred(hint.get(), middle, HINT_TOP, 4, HINT_SCALE, HologramStyle.TEXT_SOFT, 0);
    }

    private void drawScrollbar(HologramPainter painter, double u, double v) {
        int left = HologramGrid.SCROLLBAR_LEFT;
        int right = left + HologramGrid.SCROLLBAR_WIDTH;
        int top = HologramGrid.GRID_TOP;
        int bottom = top + HologramGrid.GRID_HEIGHT;
        painter.rect(left, top, right, bottom, 3, HologramStyle.BUTTON_EDGE);
        painter.rect(left + 1, top + 1, right - 1, bottom - 1, 4, HologramStyle.TRACK);
        int thumbTop = grid.thumbTop();
        int thumbBottom = thumbTop + grid.thumbHeight();
        boolean hover = HologramGrid.onScrollbar(u, v);
        painter.rect(left + 1, thumbTop + 1, right - 1, thumbBottom - 1, 5,
                hover ? HologramStyle.THUMB_HOVER : HologramStyle.THUMB);
        int grip = (thumbTop + thumbBottom) / 2;
        for (int line = -2; line <= 2; line += 2) {
            painter.rect(left + 2, grip + line, right - 2, grip + line + 1, 6,
                    HologramStyle.FRAME_LIGHT);
        }
    }

    @Override
    boolean click(double u, double v) {
        int index = grid.buttonAt(u, v);
        if (index >= 0) {
            press.accept(spec.buttons().get(index));
            return true;
        }
        if (HologramGrid.onScrollbar(u, v)) {
            grid.scrollToTrack(v);
            return true;
        }
        return false;
    }

    @Override
    void scroll(double u, double v, int rows) {
        grid.scrollBy(rows);
    }
}
