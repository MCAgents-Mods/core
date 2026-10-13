package io.github.mcagentsmods.core.common.hologram;

/**
 * Where one panel stands inside its hologram's frame, from {@link HologramSpace#arrange}.
 *
 * @param x    the panel's centre, to the right of the anchor, in blocks
 * @param z    the panel's centre, towards the player, in blocks
 * @param turn how far the panel is turned about its vertical centre line, in degrees; a panel
 *             to the right of the middle is turned towards the player with a negative turn
 */
public record PanelPlacement(double x, double z, double turn) {

    /** A lone panel, at the anchor and facing straight back at the player. */
    public static final PanelPlacement CENTRE = new PanelPlacement(0, 0, 0);
}
