package io.github.mcagentsmods.core.client.hologram;

import io.github.mcagentsmods.core.api.hologram.HologramButton;
import io.github.mcagentsmods.core.common.hologram.ColumnLayout;
import io.github.mcagentsmods.core.common.hologram.PanelArea;
import io.github.mcagentsmods.core.common.hologram.ScrollWindow;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * What the tall panels share: the title band, the sunken box and its scrollbar, and reading the
 * mod's suppliers without letting one that throws break the frame.
 */
abstract class ColumnPanelView extends PanelView {

    private static final Logger LOGGER = Logger.getLogger("MCAgents Core");

    private final String title;
    private boolean readFailed;

    ColumnPanelView(String title) {
        this.title = title;
    }

    @Override
    int width() {
        return ColumnLayout.WIDTH;
    }

    @Override
    final int height() {
        return ColumnLayout.HEIGHT;
    }

    /** @return the panel's title, as the mod gave it */
    final String title() {
        return title;
    }

    /**
     * What the title band shows. The title, unless the panel shows something that changes
     * while it stands.
     *
     * @return a translation key, or the text itself
     */
    String heading() {
        return title;
    }

    /** Draws the frame and the title band with the heading centred in it. */
    final void drawColumn(HologramPainter painter) {
        drawBand(painter);
        String text = label(heading());
        // Clear of the close button, which sits in the band's right end.
        float room = width() - 2 * (CLOSE_INSET + CLOSE_SIZE + 4);
        float scale = Math.max(0.5f, Math.min(1, room / painter.width(text)));
        painter.centred(text, width() / 2f, ColumnLayout.TITLE_TOP + (8 - 8 * scale) / 2, 4, scale,
                HologramStyle.TEXT, 0);
    }

    /** Draws the frame and the empty title band. */
    final void drawBand(HologramPainter painter) {
        drawFrame(painter);
        PanelArea header = new PanelArea(ColumnLayout.HEADER.left(), ColumnLayout.HEADER.top(),
                width() - (ColumnLayout.WIDTH - ColumnLayout.HEADER.right()), ColumnLayout.HEADER.bottom());
        painter.rect((float) header.left(), (float) header.top(), (float) header.right(),
                (float) header.bottom(), 2, HologramStyle.HEADER);
        painter.rect((float) header.left(), (float) header.bottom() - 1, (float) header.right(),
                (float) header.bottom(), 3, HologramStyle.FRAME_SHADE);
    }

    /** Draws the sunken box, ringed in the focus colour while it takes the keys. */
    static void drawBox(HologramPainter painter, PanelArea box, boolean focused) {
        float left = (float) box.left();
        float top = (float) box.top();
        float right = (float) box.right();
        float bottom = (float) box.bottom();
        painter.rect(left, top, right, bottom, 2, focused ? HologramStyle.FOCUS : HologramStyle.BUTTON_EDGE);
        painter.rect(left + 1, top + 1, right - 1, bottom - 1, 3, HologramStyle.BOX);
        if (focused) {
            painter.outline(left + 1, top + 1, right - 1, bottom - 1, 4, HologramStyle.FOCUS);
        }
    }

    /**
     * Runs the button along the bottom that a point is on, if any.
     *
     * @return whether the point was on a button
     */
    static boolean clickFooter(List<HologramButton> buttons, Actions actions, double u, double v) {
        for (int i = 0; i < buttons.size(); i++) {
            if (ColumnLayout.footerButton(i, buttons.size()).contains(u, v)) {
                HologramButton button = buttons.get(i);
                actions.run("The hologram button " + button.label(), button.action());
                return true;
            }
        }
        return false;
    }

    /** Draws the buttons along the bottom, the one aimed at lightened. */
    static void drawFooter(HologramPainter painter, List<HologramButton> buttons, double u, double v) {
        for (int i = 0; i < buttons.size(); i++) {
            PanelArea area = ColumnLayout.footerButton(i, buttons.size());
            drawButton(painter, area, label(buttons.get(i).label()), area.contains(u, v), true);
        }
    }

    /** Draws the box's scrollbar, its thumb darker while aimed at. */
    static void drawScrollbar(HologramPainter painter, PanelArea box, ScrollWindow window,
                              double u, double v) {
        PanelArea track = ColumnLayout.scrollbar(box);
        float left = (float) track.left();
        float top = (float) track.top();
        float right = (float) track.right();
        float bottom = (float) track.bottom();
        painter.rect(left, top, right, bottom, 4, HologramStyle.BUTTON_EDGE);
        painter.rect(left + 1, top + 1, right - 1, bottom - 1, 5, HologramStyle.TRACK);
        int length = (int) track.height() - 2;
        float thumbTop = top + 1 + window.thumbStart(length, ColumnLayout.MIN_THUMB);
        float thumbBottom = thumbTop + window.thumbLength(length, ColumnLayout.MIN_THUMB);
        painter.rect(left + 1, thumbTop, right - 1, thumbBottom, 6,
                track.contains(u, v) ? HologramStyle.THUMB_HOVER : HologramStyle.THUMB);
        float grip = Math.round((thumbTop + thumbBottom) / 2);
        for (int line = -2; line <= 2; line += 2) {
            painter.rect(left + 2, grip + line, right - 2, grip + line + 1, 7, HologramStyle.FRAME_LIGHT);
        }
    }

    /**
     * Jumps the box's window to a click on its scrollbar.
     *
     * @return whether the click was on the scrollbar
     */
    static boolean clickScrollbar(PanelArea box, ScrollWindow window, double u, double v) {
        PanelArea track = ColumnLayout.scrollbar(box);
        if (!track.contains(u, v)) {
            return false;
        }
        window.scrollToTrack(v - track.top() - 1, (int) track.height() - 2, ColumnLayout.MIN_THUMB);
        return true;
    }

    /** Reads one of the mod's suppliers, falling back when it throws or gives null. */
    final <T> T read(Supplier<T> supplier, T fallback) {
        try {
            T value = supplier.get();
            return value == null ? fallback : value;
        } catch (RuntimeException failure) {
            readFailed(failure);
            return fallback;
        }
    }

    /** Reads one of the mod's int suppliers, falling back when it throws. */
    final int readInt(IntSupplier supplier, int fallback) {
        try {
            return supplier.getAsInt();
        } catch (RuntimeException failure) {
            readFailed(failure);
            return fallback;
        }
    }

    /** Reads one of the mod's boolean suppliers, falling back when it throws. */
    final boolean readBoolean(BooleanSupplier supplier, boolean fallback) {
        try {
            return supplier.getAsBoolean();
        } catch (RuntimeException failure) {
            readFailed(failure);
            return fallback;
        }
    }

    private void readFailed(RuntimeException failure) {
        // Once per panel: a supplier that throws does so every frame.
        if (!readFailed) {
            readFailed = true;
            LOGGER.log(Level.WARNING, "The hologram panel " + title + " could not read what it shows",
                    failure);
        }
    }
}
