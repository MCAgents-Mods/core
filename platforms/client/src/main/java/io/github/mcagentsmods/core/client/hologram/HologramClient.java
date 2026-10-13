package io.github.mcagentsmods.core.client.hologram;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.mcagentsmods.core.api.hologram.ButtonGrid;
import io.github.mcagentsmods.core.api.hologram.Hologram;
import io.github.mcagentsmods.core.api.hologram.HologramButton;
import io.github.mcagentsmods.core.api.hologram.HologramPanel;
import io.github.mcagentsmods.core.common.hologram.HologramRange;
import io.github.mcagentsmods.core.common.hologram.Vector3;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * What core's loader modules connect their events to. A mod never calls this class; it opens
 * holograms through {@link Holograms}.
 *
 * <p>A loader calls {@link #createMoveKey} while its client starts, registers the key the way
 * it registers keys, and then calls {@link #onEndTick} at the end of every client tick,
 * {@link #submit} while the level is drawn, {@link #onAttack} when the attack key fires and
 * {@link #onScroll} when the mouse wheel would change the hotbar slot. Every method runs on the
 * client thread.
 */
public final class HologramClient {

    /** The Controls category the move key is listed under. */
    public static final Identifier KEY_CATEGORY = Identifier.fromNamespaceAndPath("mcagents_core", "main");

    private static final Logger LOGGER = Logger.getLogger("MCAgents Core");

    private static KeyMapping moveKey;
    private static volatile OpenHologram open;
    private static boolean attackArmed = true;

    private HologramClient() {
    }

    /**
     * Builds the key that moves a hologram while it is held, Z unless the player rebinds it.
     * Every loader refuses a key once the game's options exist, so a loader calls this first
     * thing in its client start-up.
     *
     * @param category the category, registered by the loader its own way
     * @return the key, for the loader to register
     */
    public static KeyMapping createMoveKey(KeyMapping.Category category) {
        moveKey = new KeyMapping("key.mcagents_core.move", InputConstants.Type.KEYSYM,
                InputConstants.KEY_Z, category);
        return moveKey;
    }

    /**
     * Carries, lets go of, or dismisses the open hologram. Called at the end of every client
     * tick.
     *
     * @param minecraft the game
     */
    public static void onEndTick(Minecraft minecraft) {
        if (moveKey != null) {
            // Holding is read through isDown; the queued presses only need draining.
            while (moveKey.consumeClick()) {
                // drained
            }
        }
        if (!minecraft.options.keyAttack.isDown()) {
            attackArmed = true;
        }
        OpenHologram hologram = open;
        if (hologram == null) {
            return;
        }
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level != hologram.level) {
            close();
            return;
        }
        boolean holding = moveKey != null && moveKey.isDown() && minecraft.gui.screen() == null;
        Vector3 eye = vector(player.getEyePosition());
        Vector3 look = vector(player.getViewVector(1));
        if (hologram.carried()) {
            if (holding) {
                hologram.follow(eye, look, player.getYRot());
            } else {
                hologram.release();
            }
        } else if (holding && hologram.hovered()) {
            hologram.take(eye, look);
        }
        if (!HologramRange.inRange(hologram.pose().anchor(),
                new Vector3(player.getX(), player.getY(), player.getZ()))) {
            close();
        }
    }

    /**
     * Draws the open hologram. Called once a frame while the level's features are gathered.
     *
     * @param state     the frame's level state, for the camera
     * @param collector where the frame's geometry and text go
     */
    public static void submit(LevelRenderState state, SubmitNodeCollector collector) {
        OpenHologram hologram = open;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (hologram == null || player == null) {
            return;
        }
        float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vector3 eye = vector(player.getEyePosition(partialTick));
        Vector3 look = vector(player.getViewVector(partialTick));
        if (hologram.carried()) {
            hologram.follow(eye, look, player.getViewYRot(partialTick));
        }
        boolean free = minecraft.gui.screen() == null;
        hologram.aim(free ? eye : null, look, blockedAt(minecraft.hitResult, eye));
        hologram.submit(collector, state.cameraRenderState.pos, minecraft.font);
    }

    /**
     * Presses what the player aims at on the open hologram. Called when the attack key fires,
     * which happens once when it is pressed and then every tick while it is held on a block.
     *
     * @return {@code true} when the hologram took the click and the attack must be cancelled
     */
    public static boolean onAttack() {
        OpenHologram hologram = open;
        if (hologram == null || !hologram.hovered()
                || Minecraft.getInstance().gui.screen() != null) {
            // This press belongs to the world, even if the crosshair reaches the hologram later.
            attackArmed = false;
            return false;
        }
        if (attackArmed) {
            attackArmed = false;
            if (hologram.click()) {
                close();
            }
        }
        return true;
    }

    /**
     * Scrolls the panel the player aims at.
     *
     * @param amount the wheel's vertical movement, positive away from the player
     * @return {@code true} when the hologram took the wheel and the hotbar must not change
     */
    public static boolean onScroll(double amount) {
        OpenHologram hologram = open;
        if (hologram == null || !hologram.hovered()
                || Minecraft.getInstance().gui.screen() != null) {
            return false;
        }
        if (amount != 0) {
            hologram.scroll(amount > 0 ? -1 : 1);
        }
        return true;
    }

    static void open(Hologram spec) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            LOGGER.warning("Hologram " + spec.id() + " was not opened: no player is in a world");
            return;
        }
        List<PanelView> views = new ArrayList<>();
        for (HologramPanel panel : spec.panels()) {
            views.add(view(panel));
        }
        open = new OpenHologram(spec, minecraft.level, views, vector(player.getEyePosition()),
                player.getYRot());
    }

    static void close() {
        open = null;
    }

    static String openId() {
        OpenHologram hologram = open;
        return hologram == null ? null : hologram.spec.id();
    }

    private static PanelView view(HologramPanel panel) {
        return switch (panel) {
            case ButtonGrid grid -> new GridPanelView(grid, HologramClient::hint, HologramClient::press);
        };
    }

    private static String hint() {
        String key = moveKey == null ? "Z" : moveKey.getTranslatedKeyMessage().getString();
        return I18n.get("hologram.mcagents_core.hint", key);
    }

    private static void press(HologramButton button) {
        try {
            button.action().run();
        } catch (RuntimeException failure) {
            LOGGER.log(Level.WARNING, "The hologram button " + button.label() + " failed", failure);
        }
    }

    private static double blockedAt(HitResult target, Vector3 eye) {
        if (target == null || target.getType() == HitResult.Type.MISS) {
            return Double.POSITIVE_INFINITY;
        }
        return vector(target.getLocation()).subtract(eye).length();
    }

    private static Vector3 vector(Vec3 vec) {
        return new Vector3(vec.x, vec.y, vec.z);
    }
}
