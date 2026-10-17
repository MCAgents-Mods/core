package io.github.mcagentsmods.core.client.hologram;

import io.github.mcagentsmods.core.api.hologram.Hologram;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Objects;

/**
 * Opens and closes holograms. This is what a mod calls.
 *
 * <p>A hologram opens in front of the player, facing them, and stays at that place in the
 * world. The player aims at it and left-clicks to press, scrolls with the mouse wheel, holds
 * the move key (Z) to carry it and lets go to leave it, and clicks its X to close it. It also
 * closes when the player is more than two chunks from it, changes dimension or leaves the
 * world.
 *
 * <h2>Several at once</h2>
 *
 * <p>Up to {@link #MAX_OPEN} holograms stand at once, each aimed at, carried and closed on its
 * own; the nearest one under the crosshair is the one aimed at. A new one opens in front of
 * the player or, when another stands there, turned to the player's right or left until it is
 * clear. Opening one more than {@link #MAX_OPEN} closes the one opened longest ago.
 *
 * <p>An id names one hologram: opening an id that is already open closes that one and opens
 * the new one in front of the player, so it moves rather than doubles. A mod that shows
 * several of a kind gives each its own id, such as {@code {modid}.{name}.{key}}.
 *
 * <h2>Threading</h2>
 *
 * <p>Call from the client thread. A call from any other thread is handed to the client thread
 * and takes effect on its next turn; {@link #isOpen} and {@link #openIds} may be read from any
 * thread.
 */
public final class Holograms {

    /** The most holograms that stand at once. */
    public static final int MAX_OPEN = 8;

    private Holograms() {
    }

    /**
     * Opens {@code hologram} in front of the player. One with the same id closes first. Does
     * nothing, apart from logging, when no player is in a world.
     *
     * @param hologram what to show
     */
    public static void open(Hologram hologram) {
        Objects.requireNonNull(hologram, "hologram");
        onClientThread(() -> HologramClient.open(hologram));
    }

    /**
     * Opens {@code hologram} unless one with its id is open, in which case that one closes.
     * This is what a key that shows and hides a hologram calls.
     *
     * @param hologram what to show
     */
    public static void toggle(Hologram hologram) {
        Objects.requireNonNull(hologram, "hologram");
        onClientThread(() -> {
            if (HologramClient.openIds().contains(hologram.id())) {
                HologramClient.close(hologram.id());
            } else {
                HologramClient.open(hologram);
            }
        });
    }

    /**
     * Closes the hologram with this id, if it is open.
     *
     * @param id the hologram's id
     */
    public static void close(String id) {
        Objects.requireNonNull(id, "id");
        onClientThread(() -> HologramClient.close(id));
    }

    /** Closes every hologram. */
    public static void closeAll() {
        onClientThread(HologramClient::closeAll);
    }

    /**
     * Runs {@code action}, and lets the first hologram it opens take the place of the
     * hologram with id {@code id}: that one closes, and the new one opens where the player
     * looks rather than beside it. When {@code action} opens no hologram, the one with id
     * {@code id} stays. A menu runs its buttons this way, so a button that opens a hologram
     * replaces the menu and one that opens a screen leaves it standing.
     *
     * @param id     the id of the hologram to give way
     * @param action what to run, at once, on the client thread
     */
    public static void replacing(String id, Runnable action) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(action, "action");
        onClientThread(() -> HologramClient.replacing(id, action));
    }

    /**
     * @param id a hologram's id
     * @return whether the hologram with this id is open
     */
    public static boolean isOpen(String id) {
        return id != null && HologramClient.openIds().contains(id);
    }

    /** @return the ids of the holograms open, the one opened longest ago first */
    public static List<String> openIds() {
        return HologramClient.openIds();
    }

    private static void onClientThread(Runnable action) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.isSameThread()) {
            action.run();
        } else {
            minecraft.execute(action);
        }
    }
}
