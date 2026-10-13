package io.github.mcagentsmods.core.common.hologram;

import java.util.Objects;

/**
 * Where a hologram stands: the centre of its middle panel, and the yaw of the player it was
 * turned to face.
 *
 * <p>A hologram has its own frame: +x to the facing player's right, +y up and +z towards
 * that player. {@link #toWorld} and {@link #toLocal} convert between that frame and the world.
 *
 * @param anchor the centre of the middle panel, in world space
 * @param yaw    the yaw of the player the hologram faces, in Minecraft degrees
 */
public record HologramPose(Vector3 anchor, double yaw) {

    public HologramPose {
        Objects.requireNonNull(anchor, "anchor");
    }

    /**
     * The turn that carries the hologram's frame into the world's, as Minecraft's pose stack
     * applies it.
     *
     * @return degrees about the vertical axis
     */
    public double turn() {
        return 180 - yaw;
    }

    /**
     * Converts a direction from the hologram's frame into the world's.
     *
     * @param local a direction in the hologram's frame
     * @return the same direction in world space
     */
    public Vector3 toWorld(Vector3 local) {
        return local.rotateY(turn());
    }

    /**
     * Converts a direction from the world into the hologram's frame.
     *
     * @param world a direction in world space
     * @return the same direction in the hologram's frame
     */
    public Vector3 toLocal(Vector3 world) {
        return world.rotateY(-turn());
    }
}
