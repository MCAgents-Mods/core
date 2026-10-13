package io.github.mcagentsmods.core.common.hologram;

/**
 * A rectangle on a panel, in panel pixels from its top left corner: from {@code left} and
 * {@code top}, inclusive, to {@code right} and {@code bottom}, exclusive.
 *
 * @param left   the left edge
 * @param top    the top edge
 * @param right  the right edge
 * @param bottom the bottom edge
 */
public record PanelArea(double left, double top, double right, double bottom) {

    public PanelArea {
        if (right < left || bottom < top) {
            throw new IllegalArgumentException("An area runs left to right and top to bottom");
        }
    }

    /** @return how wide the area is */
    public double width() {
        return right - left;
    }

    /** @return how tall the area is */
    public double height() {
        return bottom - top;
    }

    /**
     * @param u a point's distance from the panel's left edge, or {@code NaN} for none
     * @param v a point's distance from the panel's top edge, or {@code NaN} for none
     * @return whether the point is inside
     */
    public boolean contains(double u, double v) {
        return u >= left && u < right && v >= top && v < bottom;
    }
}
