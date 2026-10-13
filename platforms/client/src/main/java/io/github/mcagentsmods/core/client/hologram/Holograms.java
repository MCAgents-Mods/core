package io.github.mcagentsmods.core.client.hologram;

import io.github.mcagentsmods.core.api.hologram.Hologram;
import net.minecraft.client.Minecraft;

import java.util.Objects;
import java.util.Optional;

/**
 * Opens and closes holograms. This is what a mod calls.
 *
 * <p>A hologram opens in front of the player, facing them, and stays at that place in the
 * world. The player aims at it and left-clicks to press, scrolls with the mouse wheel, holds
 * the move key (Z) to carry it and lets go to leave it, and clicks its X to close it. It also
 * closes when the player is more than two chunks from it, changes dimension or leaves the
 * world. Only one hologram is open at a time: opening one closes the other.
 *
 * <h2>Threading</h2>
 *
 * <p>Call from the client thread. A call from any other thread is handed to the client thread
 * and takes effect on its next turn; {@link #isOpen} and {@link #openId} may be read from any
 * thread.
 */
public final class Holograms {

    private Holograms() {
    }

    /**
     * Opens {@code hologram} in front of the player, closing whichever is open. Does nothing,
     * apart from logging, when no player is in a world.
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
            if (hologram.id().equals(HologramClient.openId())) {
                HologramClient.close();
            } else {
                HologramClient.open(hologram);
            }
        });
    }

    /**
     * Closes the hologram with this id, if it is the one open.
     *
     * @param id the hologram's id
     */
    public static void close(String id) {
        Objects.requireNonNull(id, "id");
        onClientThread(() -> {
            if (id.equals(HologramClient.openId())) {
                HologramClient.close();
            }
        });
    }

    /** Closes whichever hologram is open. */
    public static void closeAll() {
        onClientThread(HologramClient::close);
    }

    /**
     * @param id a hologram's id
     * @return whether the hologram with this id is the one open
     */
    public static boolean isOpen(String id) {
        return id != null && id.equals(HologramClient.openId());
    }

    /** @return the id of the hologram open, if any */
    public static Optional<String> openId() {
        return Optional.ofNullable(HologramClient.openId());
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
