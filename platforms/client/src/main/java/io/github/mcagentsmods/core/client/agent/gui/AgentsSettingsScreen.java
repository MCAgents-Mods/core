package io.github.mcagentsmods.core.client.agent.gui;

import io.github.mcagentsmods.core.api.agent.AgentRequest;
import io.github.mcagentsmods.core.common.agent.AgentSettings;
import io.github.mcagentsmods.core.common.agent.Agents;
import io.github.mcagentsmods.core.tool.WorkerPool;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.function.IntConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Where the player sets how many tool calls run at once and how many rounds of tool calls a
 * question may take.
 *
 * <p>Each value has a minus and a plus button, so it can only ever be one the agent accepts.
 * Saving applies both at once — a raised worker limit starts queued tool calls immediately —
 * and writes them to disk off the game's thread.
 */
public final class AgentsSettingsScreen extends Screen {

    private static final Logger LOGGER = Logger.getLogger("MCAgents Core");

    private static final int WIDTH = 260;
    private static final int BUTTON_HEIGHT = 20;
    private static final int STEP = 20;
    private static final int ROW = 52;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GREY = 0xFFA0A0A0;

    private final Screen parent;
    private int workerLimit;
    private int toolRounds;
    private int top;

    /**
     * @param parent the screen to go back to when this one closes, or {@code null} to go back to
     *               the game, as from the agents menu
     */
    public AgentsSettingsScreen(Screen parent) {
        super(Component.translatable("gui.mcagents_core.agents.settings.title"));
        this.parent = parent;
        AgentSettings current = Agents.settings();
        this.workerLimit = current.workerLimit();
        this.toolRounds = current.toolRounds();
    }

    @Override
    protected void init() {
        int left = (width - WIDTH) / 2;
        top = Math.max(40, height / 2 - 70);

        stepper(left, top, value -> workerLimit = WorkerPool.clampLimit(workerLimit + value));
        stepper(left, top + ROW, value -> toolRounds = AgentSettings.clampToolRounds(toolRounds + value));

        int third = (WIDTH - 8) / 3;
        int bottom = top + 2 * ROW + 8;
        Component defaults = Component.translatable("gui.mcagents_core.agents.settings.defaults");
        addRenderableWidget(Button.builder(defaults, pressed -> {
                    workerLimit = AgentSettings.DEFAULTS.workerLimit();
                    toolRounds = AgentSettings.DEFAULTS.toolRounds();
                })
                .bounds(left, bottom, third, BUTTON_HEIGHT)
                .build());
        Component save = Component.translatable("gui.mcagents_core.agents.settings.save");
        addRenderableWidget(Button.builder(save, pressed -> save())
                .bounds(left + third + 4, bottom, third, BUTTON_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), pressed -> onClose())
                .bounds(left + 2 * (third + 4), bottom, third, BUTTON_HEIGHT)
                .build());
    }

    /** A minus and a plus button at the right of a row. */
    private void stepper(int left, int y, IntConsumer change) {
        int right = left + WIDTH;
        addRenderableWidget(Button.builder(Component.literal("-"), pressed -> change.accept(-1))
                .bounds(right - 2 * STEP - 40, y, STEP, BUTTON_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(Component.literal("+"), pressed -> change.accept(1))
                .bounds(right - STEP, y, STEP, BUTTON_HEIGHT)
                .build());
    }

    private void save() {
        AgentSettings chosen = new AgentSettings(workerLimit, toolRounds);
        Agents.update(chosen).whenComplete((ignored, failure) -> {
            if (failure != null) {
                LOGGER.log(Level.WARNING, "Could not save the agents settings; they apply until the game closes", failure);
            }
        });
        onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int left = (width - WIDTH) / 2;
        graphics.centeredText(font, title, width / 2, top - 24, WHITE);
        row(graphics, left, top, "gui.mcagents_core.agents.settings.workers", workerLimit,
                Component.translatable("gui.mcagents_core.agents.settings.workers.help",
                        WorkerPool.DEFAULT_LIMIT, WorkerPool.MIN_LIMIT, WorkerPool.MAX_LIMIT));
        row(graphics, left, top + ROW, "gui.mcagents_core.agents.settings.rounds", toolRounds,
                Component.translatable("gui.mcagents_core.agents.settings.rounds.help",
                        AgentSettings.DEFAULT_TOOL_ROUNDS, AgentRequest.MAX_TOOL_ROUNDS));
    }

    private void row(GuiGraphicsExtractor graphics, int left, int y, String label, int value,
                     Component help) {
        int right = left + WIDTH;
        graphics.text(font, Component.translatable(label), left, y + 6, WHITE);
        graphics.centeredText(font, Component.literal(Integer.toString(value)),
                right - STEP - 20, y + 6, WHITE);
        graphics.text(font, help, left, y + BUTTON_HEIGHT + 6, GREY);
    }

    @Override
    public void onClose() {
        if (minecraft != null && parent != null) {
            minecraft.setScreenAndShow(parent);
            return;
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
