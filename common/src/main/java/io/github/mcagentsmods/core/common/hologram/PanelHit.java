package io.github.mcagentsmods.core.common.hologram;

/**
 * Where a ray from the player's eye meets a panel.
 *
 * @param u        panel pixels from the panel's left edge
 * @param v        panel pixels from the panel's top edge
 * @param distance blocks from the eye to the point
 */
public record PanelHit(double u, double v, double distance) {
}
