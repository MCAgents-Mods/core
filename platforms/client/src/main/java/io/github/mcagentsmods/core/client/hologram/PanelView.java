package io.github.mcagentsmods.core.client.hologram;

import net.minecraft.locale.Language;

import java.util.Locale;

/**
 * One open panel: what it draws, and what a click or the mouse wheel does on it. Points are in
 * panel pixels from the top left corner.
 */
abstract class PanelView {

    /** The close button's size, in the top right corner of a hologram's last panel. */
    static final int CLOSE_SIZE = 10;

    /** How far the close button sits in from the panel's top and right edges. */
    static final int CLOSE_INSET = 6;

    abstract int width();

    abstract int height();

    /**
     * Draws the panel.
     *
     * @param u where the player aims, or {@code NaN} when not at this panel
     * @param v where the player aims, or {@code NaN} when not at this panel
     */
    abstract void draw(HologramPainter painter, double u, double v);

    /** @return whether the click did something */
    abstract boolean click(double u, double v);

    /**
     * Scrolls whatever is under the point.
     *
     * @param rows how far, positive towards the later rows
     */
    abstract void scroll(double u, double v, int rows);

    /** Draws the frame, the face and the back every panel shares. */
    void drawFrame(HologramPainter painter) {
        int w = width();
        int h = height();
        painter.back(0, 0, w, h, HologramStyle.BACK);
        painter.rect(0, 0, w, h, 0, HologramStyle.FRAME);
        painter.rect(2, 2, w - 2, h - 2, 1, HologramStyle.FACE);
        painter.bevel(2, 2, w - 2, h - 2, 2, HologramStyle.FRAME_LIGHT, HologramStyle.FRAME_SHADE);
        // The studs on the corners, as in the picture the hologram was drawn from.
        for (int[] corner : new int[][] {{-2, -2}, {w - 3, -2}, {-2, h - 3}, {w - 3, h - 3}}) {
            painter.rect(corner[0], corner[1], corner[0] + 5, corner[1] + 5, 3, HologramStyle.FRAME);
        }
    }

    /** @return whether a point is on the close button */
    boolean onClose(double u, double v) {
        double left = width() - CLOSE_INSET - CLOSE_SIZE;
        return u >= left && u < left + CLOSE_SIZE && v >= CLOSE_INSET && v < CLOSE_INSET + CLOSE_SIZE;
    }

    /** Draws the close button, red while aimed at. */
    void drawClose(HologramPainter painter, double u, double v) {
        float left = width() - CLOSE_INSET - CLOSE_SIZE;
        float top = CLOSE_INSET;
        boolean hover = onClose(u, v);
        painter.rect(left, top, left + CLOSE_SIZE, top + CLOSE_SIZE, 3, HologramStyle.BUTTON_EDGE);
        painter.rect(left + 1, top + 1, left + CLOSE_SIZE - 1, top + CLOSE_SIZE - 1, 4,
                hover ? HologramStyle.CLOSE_HOVER : HologramStyle.BUTTON);
        painter.centred("x", left + CLOSE_SIZE / 2f, top + 1, 5, 1,
                hover ? HologramStyle.TITLE : HologramStyle.TEXT, 0);
    }

    /** A label as shown: its translation when there is one, in upper case. */
    static String label(String keyOrText) {
        return Language.getInstance().getOrDefault(keyOrText).toUpperCase(Locale.ROOT);
    }
}
