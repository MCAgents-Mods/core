package io.github.mcagentsmods.core.client.agent;

import io.github.mcagentsmods.core.api.button.AgentButton;
import io.github.mcagentsmods.core.api.hologram.ButtonGrid;
import io.github.mcagentsmods.core.api.hologram.Hologram;
import io.github.mcagentsmods.core.api.hologram.HologramButton;
import io.github.mcagentsmods.core.client.agent.gui.AgentsSettingsScreen;
import io.github.mcagentsmods.core.client.hologram.Holograms;
import io.github.mcagentsmods.core.common.agent.Agents;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/**
 * The menu G shows: a hologram in front of the player, titled MCAgents, with one button for
 * each agent feature a mod has added and then Settings.
 *
 * <p>Core draws it and takes the player's clicks; this class only builds it. It is built again
 * each time it opens, so a button registered late appears the next time. Call every method on
 * the game's thread.
 */
public final class AgentsMenu {

    /** The menu hologram's id, as core's {@code Holograms.isOpen} and {@code close} take it. */
    public static final String ID = "mcagents_core.menu";

    private static final String TITLE = "gui.mcagents_core.agents.title";
    private static final String SETTINGS = "gui.mcagents_core.agents.settings";

    private AgentsMenu() {
        throw new AssertionError("No instances.");
    }

    /** Opens the menu, closing any other hologram, as a mod's own button might want. */
    public static void open() {
        Holograms.open(build());
    }

    /** Closes the menu when it is open and opens it otherwise, as G does. */
    public static void toggle() {
        Holograms.toggle(build());
    }

    /** Closes the menu, if it is the hologram open. */
    public static void close() {
        Holograms.close(ID);
    }

    /** Whether the menu is the hologram open. */
    public static boolean isOpen() {
        return Holograms.isOpen(ID);
    }

    private static Hologram build() {
        List<HologramButton> buttons = new ArrayList<>();
        for (AgentButton button : Agents.buttons().list()) {
            buttons.add(new HologramButton(button.label(), button.action()));
        }
        buttons.add(new HologramButton(SETTINGS, AgentsMenu::openSettings));
        return Hologram.of(ID, new ButtonGrid(TITLE, buttons));
    }

    private static void openSettings() {
        Minecraft.getInstance().setScreenAndShow(new AgentsSettingsScreen(null));
    }
}
