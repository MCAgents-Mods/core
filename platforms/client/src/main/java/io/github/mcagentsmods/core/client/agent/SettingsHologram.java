package io.github.mcagentsmods.core.client.agent;

import io.github.mcagentsmods.core.api.agent.AgentRequest;
import io.github.mcagentsmods.core.api.hologram.Form;
import io.github.mcagentsmods.core.api.hologram.FormButton;
import io.github.mcagentsmods.core.api.hologram.FormStepper;
import io.github.mcagentsmods.core.api.hologram.FormText;
import io.github.mcagentsmods.core.api.hologram.FormTone;
import io.github.mcagentsmods.core.api.hologram.Hologram;
import io.github.mcagentsmods.core.client.hologram.Holograms;
import io.github.mcagentsmods.core.common.agent.AgentSettings;
import io.github.mcagentsmods.core.common.agent.Agents;
import io.github.mcagentsmods.core.tool.WorkerPool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Settings hologram, which SETTINGS on the agents menu opens: how many tool calls run at
 * once and how many rounds of tool calls a question may take.
 *
 * <p>Each value has a minus and a plus button, so it can only ever be one the agent accepts.
 * SAVE applies both at once (a raised worker limit starts queued tool calls immediately),
 * writes them to disk off the game's thread and says so on the hologram, which stays open.
 * DEFAULTS only puts the defaults on the hologram; nothing changes until SAVE. Call every
 * method on the game's thread.
 */
public final class SettingsHologram {

    /** The hologram's id, as core's {@code Holograms.isOpen} and {@code close} take it. */
    public static final String ID = "mcagents_core.settings";

    private static final Logger LOGGER = Logger.getLogger("MCAgents Core");

    private static final String PREFIX = "gui.mcagents_core.agents.settings.";

    private int workerLimit;
    private int toolRounds;
    private String status = "";
    private FormTone tone = FormTone.SOFT;

    private SettingsHologram(AgentSettings current) {
        this.workerLimit = current.workerLimit();
        this.toolRounds = current.toolRounds();
    }

    /** Opens the hologram on the settings in force, or moves it in front of the player. */
    public static void open() {
        Holograms.open(new SettingsHologram(Agents.settings()).build());
    }

    private Hologram build() {
        return Hologram.of(ID, new Form(PREFIX + "title", List.of(
                new FormStepper(PREFIX + "workers", () -> workerLimit,
                        step -> workerLimit = WorkerPool.clampLimit(workerLimit + step)),
                new FormText(() -> I18n.get(PREFIX + "workers.help", WorkerPool.DEFAULT_LIMIT,
                        WorkerPool.MIN_LIMIT, WorkerPool.MAX_LIMIT), () -> FormTone.SOFT, 1),
                new FormStepper(PREFIX + "rounds", () -> toolRounds,
                        step -> toolRounds = AgentSettings.clampToolRounds(toolRounds + step)),
                new FormText(() -> I18n.get(PREFIX + "rounds.help", AgentSettings.DEFAULT_TOOL_ROUNDS,
                        AgentRequest.MAX_TOOL_ROUNDS), () -> FormTone.SOFT, 1),
                new FormText(() -> status, () -> tone, 2)),
                List.of(FormButton.of(PREFIX + "defaults", this::defaults),
                        FormButton.of(PREFIX + "save", this::save))).wide());
    }

    private void defaults() {
        workerLimit = AgentSettings.DEFAULTS.workerLimit();
        toolRounds = AgentSettings.DEFAULTS.toolRounds();
        say("", FormTone.SOFT);
    }

    private void save() {
        AgentSettings chosen = new AgentSettings(workerLimit, toolRounds);
        say(PREFIX + "saving", FormTone.SOFT);
        Agents.update(chosen).whenComplete((ignored, failure) -> {
            if (failure != null) {
                LOGGER.log(Level.WARNING, "Could not save the agents settings; they apply until the game closes", failure);
            }
            Minecraft.getInstance().execute(() -> say(failure == null ? PREFIX + "saved" : PREFIX + "save_failed",
                    failure == null ? FormTone.GOOD : FormTone.BAD));
        });
    }

    private void say(String key, FormTone tone) {
        this.status = key;
        this.tone = tone;
    }
}
