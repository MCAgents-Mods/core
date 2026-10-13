package io.github.mcagentsmods.core.common.hologram;

/**
 * The rule that dismisses a hologram the player has walked away from.
 *
 * <p>Distance is counted in chunks, on each axis, from the chunk the hologram stands in now.
 * A hologram placed in chunk 1 stays while the player is in chunks 2 and 3, and goes once the
 * player reaches chunk 4.
 */
public final class HologramRange {

    /** How many chunks the player may be from the hologram's chunk, on either axis. */
    public static final int CHUNKS = 2;

    private HologramRange() {
    }

    /**
     * The chunk a block coordinate falls in.
     *
     * @param coordinate an x or z position, in blocks
     * @return the chunk coordinate
     */
    public static int chunk(double coordinate) {
        return Math.floorDiv((int) Math.floor(coordinate), 16);
    }

    /**
     * Whether a player in chunk {@code (playerX, playerZ)} keeps a hologram standing in chunk
     * {@code (hologramX, hologramZ)}.
     *
     * @param hologramX the hologram's chunk x
     * @param hologramZ the hologram's chunk z
     * @param playerX   the player's chunk x
     * @param playerZ   the player's chunk z
     * @return {@code true} while the player is at most {@link #CHUNKS} chunks away on both axes
     */
    public static boolean inRange(int hologramX, int hologramZ, int playerX, int playerZ) {
        return Math.abs((long) playerX - hologramX) <= CHUNKS
                && Math.abs((long) playerZ - hologramZ) <= CHUNKS;
    }

    /**
     * Whether a player at {@code player} keeps a hologram standing at {@code hologram}.
     *
     * @param hologram where the hologram stands
     * @param player   where the player stands
     * @return {@code true} while the player is in range
     */
    public static boolean inRange(Vector3 hologram, Vector3 player) {
        return inRange(chunk(hologram.x()), chunk(hologram.z()), chunk(player.x()),
                chunk(player.z()));
    }
}
