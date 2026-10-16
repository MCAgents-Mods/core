package io.github.mcagentsmods.core.client.agent;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.logging.Logger;

/**
 * The key that shows and hides the agents menu: G unless the player rebinds it.
 *
 * <p>Built here and registered by each loader, as early as its client start-up runs and before
 * anything that can fail, because every loader refuses a key mapping once the game's options
 * exist. It sits in core's Controls category, beside the key that moves a hologram. The loader
 * also calls {@link #onEndTick} at the end of every client tick.
 */
public final class AgentsKeys {

    private static final Logger LOGGER = Logger.getLogger("MCAgents Core");

    private static KeyMapping open;

    private AgentsKeys() {
        throw new AssertionError("No instances.");
    }

    /**
     * Builds the key in {@code category}, core's own, for the loader to register.
     *
     * @return the key, which the loader hands to its own registration
     */
    public static KeyMapping create(KeyMapping.Category category) {
        // A keyboard key: the type is left to KeyMapping, whose name for it changed in 26.3.
        open = new KeyMapping("key.mcagents_core.agents", InputConstants.KEY_G, category);
        LOGGER.info("Registered the agents menu key binding, default G");
        return open;
    }

    /** Acts on the key; each loader calls it at the end of every client tick. */
    public static void onEndTick(Minecraft client) {
        if (open == null) {
            return;
        }
        // consumeClick drains the queued presses, so a held key or a double press within one
        // tick toggles the menu once rather than opening and closing it again.
        boolean pressed = false;
        while (open.consumeClick()) {
            pressed = true;
        }
        if (pressed) {
            AgentsMenu.toggle();
        }
    }
}
