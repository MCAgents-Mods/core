package io.github.mcagentsmods.core.common.hologram;

import java.util.Objects;

/**
 * The ground a standing hologram takes up, seen from above: a circle around its anchor wide
 * enough to hold every panel. Only the level position counts; holograms open near eye height,
 * so two that meet on the ground stand in each other.
 *
 * @param centre the hologram's anchor
 * @param radius half the hologram's width, in blocks
 */
public record HologramFootprint(Vector3 centre, double radius) {

    public HologramFootprint {
        Objects.requireNonNull(centre, "centre");
        if (!(radius >= 0)) {
            throw new IllegalArgumentException("A footprint's radius is zero or more, not " + radius);
        }
    }

    /**
     * Whether this footprint and {@code other} share ground.
     *
     * @param other another hologram's footprint
     * @return {@code true} when the circles overlap
     */
    public boolean overlaps(HologramFootprint other) {
        double dx = centre.x() - other.centre.x();
        double dz = centre.z() - other.centre.z();
        double reach = radius + other.radius;
        return dx * dx + dz * dz < reach * reach;
    }
}
