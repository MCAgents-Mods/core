package io.github.mcagentsmods.core.client.hologram;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.mcagentsmods.core.api.hologram.ButtonGrid;
import io.github.mcagentsmods.core.api.hologram.Hologram;
import io.github.mcagentsmods.core.api.hologram.HologramButton;
import io.github.mcagentsmods.core.api.hologram.HologramPanel;
import io.github.mcagentsmods.core.api.hologram.PromptInput;
import io.github.mcagentsmods.core.api.hologram.SelectList;
import io.github.mcagentsmods.core.api.hologram.Transcript;
import io.github.mcagentsmods.core.common.hologram.HologramFootprint;
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

    /**
     * The holograms standing, oldest first. Replaced, never changed in place, so a reader on
     * another thread always sees a whole list.
     */
    private static volatile List<OpenHologram> open = List.of();

    /** The hologram being carried, if any; one at a time. */
    private static OpenHologram carried;

    /** The hologram under the crosshair this frame, if any: the nearest one aimed at. */
    private static OpenHologram aimed;

    /** While a press runs: the id of a hologram the first one it opens takes the place of. */
    private static String replacing;

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
        // A keyboard key: the type is left to KeyMapping, whose name for it changed in 26.3.
        moveKey = new KeyMapping("key.mcagents_core.move", InputConstants.KEY_Z, category);
        return moveKey;
    }

    /**
     * Carries, lets go of, or dismisses the holograms. Called at the end of every client tick.
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
        List<OpenHologram> holograms = open;
        if (holograms.isEmpty()) {
            return;
        }
        LocalPlayer player = minecraft.player;
        if (player == null) {
            closeAll();
            return;
        }
        boolean holding = moveKey != null && moveKey.isDown() && minecraft.gui.screen() == null;
        Vector3 eye = vector(player.getEyePosition());
        Vector3 look = vector(player.getViewVector(1));
        if (carried != null) {
            if (holding) {
                carried.follow(eye, look, player.getYRot());
            } else {
                carried.release();
                carried = null;
            }
        } else if (holding && aimed != null) {
            carried = aimed;
            carried.take(eye, look);
        }
        Vector3 feet = new Vector3(player.getX(), player.getY(), player.getZ());
        for (OpenHologram hologram : holograms) {
            if (minecraft.level != hologram.level
                    || !HologramRange.inRange(hologram.pose().anchor(), feet)) {
                close(hologram);
            }
        }
    }

    /**
     * Draws the holograms. Called once a frame while the level's features are gathered.
     *
     * @param state     the frame's level state, for the camera
     * @param collector where the frame's geometry and text go
     */
    public static void submit(LevelRenderState state, SubmitNodeCollector collector) {
        List<OpenHologram> holograms = open;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (holograms.isEmpty() || player == null) {
            aimed = null;
            return;
        }
        float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vector3 eye = vector(player.getEyePosition(partialTick));
        Vector3 look = vector(player.getViewVector(partialTick));
        if (carried != null) {
            carried.follow(eye, look, player.getViewYRot(partialTick));
        }
        boolean free = minecraft.gui.screen() == null;
        double blocked = blockedAt(minecraft.hitResult, eye);
        // Every hologram finds its own point, then only the nearest keeps it: one in front
        // hides the parts of another behind it.
        OpenHologram nearest = null;
        for (OpenHologram hologram : holograms) {
            hologram.aim(free ? eye : null, look, blocked);
            if (hologram.aimDistance() < (nearest == null ? Double.POSITIVE_INFINITY
                    : nearest.aimDistance())) {
                nearest = hologram;
            }
        }
        for (OpenHologram hologram : holograms) {
            if (hologram != nearest) {
                hologram.clearAim();
            }
            hologram.submit(collector, state.cameraRenderState.pos, minecraft.font);
        }
        aimed = nearest;
    }

    /**
     * Presses what the player aims at. Called when the attack key fires, which happens once
     * when it is pressed and then every tick while it is held on a block.
     *
     * @return {@code true} when a hologram took the click and the attack must be cancelled
     */
    public static boolean onAttack() {
        OpenHologram hologram = aimed;
        if (hologram == null || !hologram.hovered()
                || Minecraft.getInstance().gui.screen() != null) {
            // This press belongs to the world, even if the crosshair reaches a hologram later.
            attackArmed = false;
            return false;
        }
        if (attackArmed) {
            attackArmed = false;
            if (hologram.click()) {
                close(hologram);
            }
        }
        return true;
    }

    /**
     * Scrolls the panel the player aims at.
     *
     * @param amount the wheel's vertical movement, positive away from the player
     * @return {@code true} when a hologram took the wheel and the hotbar must not change
     */
    public static boolean onScroll(double amount) {
        OpenHologram hologram = aimed;
        if (hologram == null || !hologram.hovered()
                || Minecraft.getInstance().gui.screen() != null) {
            return false;
        }
        if (amount != 0) {
            hologram.scroll(amount > 0 ? -1 : 1);
        }
        return true;
    }

    /**
     * Stands {@code spec} in front of the player. One with the same id is closed first, so it
     * moves rather than doubles; past {@link Holograms#MAX_OPEN}, the oldest closes.
     */
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
        close(spec.id());
        if (replacing != null) {
            // The hologram pressed gives its place to this one, once.
            close(replacing);
            replacing = null;
        }
        while (open.size() >= Holograms.MAX_OPEN) {
            close(open.getFirst());
        }
        List<HologramFootprint> others = open.stream().map(OpenHologram::footprint).toList();
        OpenHologram hologram = new OpenHologram(spec, minecraft.level, views,
                vector(player.getEyePosition()), player.getYRot(), others);
        List<OpenHologram> now = new ArrayList<>(open);
        now.add(hologram);
        open = List.copyOf(now);
    }

    /** Closes the hologram with this id, if it is open. */
    static void close(String id) {
        for (OpenHologram hologram : open) {
            if (hologram.spec.id().equals(id)) {
                close(hologram);
            }
        }
    }

    static void closeAll() {
        for (OpenHologram hologram : open) {
            close(hologram);
        }
    }

    private static void close(OpenHologram hologram) {
        List<OpenHologram> now = new ArrayList<>(open);
        if (!now.remove(hologram)) {
            return;
        }
        open = List.copyOf(now);
        if (carried == hologram) {
            carried = null;
        }
        if (aimed == hologram) {
            aimed = null;
        }
        hologram.dispose();
    }

    /** @return the ids of the holograms standing, oldest first */
    static List<String> openIds() {
        return open.stream().map(hologram -> hologram.spec.id()).toList();
    }

    /**
     * Runs {@code action}; the first hologram it opens takes the place of the hologram with
     * id {@code id}, which closes. A hologram it does not open leaves that one standing.
     */
    static void replacing(String id, Runnable action) {
        String before = replacing;
        replacing = id;
        try {
            action.run();
        } finally {
            replacing = before;
        }
    }

    private static PanelView view(HologramPanel panel) {
        return switch (panel) {
            case ButtonGrid grid -> new GridPanelView(grid, HologramClient::hint, HologramClient::press);
            case SelectList list -> new ListPanelView(list, HologramClient::run);
            case Transcript transcript -> new TranscriptPanelView(transcript);
            case PromptInput prompt -> new PromptPanelView(prompt, HologramClient::run,
                    HologramClient::startTyping);
        };
    }

    private static String hint() {
        String key = moveKey == null ? "Z" : moveKey.getTranslatedKeyMessage().getString();
        return I18n.get("hologram.mcagents_core.hint", key);
    }

    private static void press(HologramButton button) {
        run("The hologram button " + button.label(), button.action());
    }

    private static boolean run(String what, Runnable action) {
        try {
            action.run();
            return true;
        } catch (RuntimeException failure) {
            LOGGER.log(Level.WARNING, what + " failed", failure);
            return false;
        }
    }

    private static void startTyping(PromptPanelView prompt) {
        Minecraft.getInstance().setScreenAndShow(new TypingScreen(prompt));
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
