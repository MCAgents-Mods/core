package io.github.mcagentsmods.core.common.agent;

import io.github.mcagentsmods.core.api.agent.Agent;
import io.github.mcagentsmods.core.api.model.ModelClient;
import io.github.mcagentsmods.core.tool.ToolExecutor;
import io.github.mcagentsmods.core.tool.ToolRegistry;
import io.github.mcagentsmods.core.tool.WorkerPool;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Where a mod finds core's agent layer: the tool registry, the worker pool, the agents menu's
 * buttons, the player's settings, and an agent to ask.
 *
 * <pre>{@code
 * Agents.tools().register(spec, arguments -> ToolResult.ok("..."));
 * Agents.buttons().register(new AgentButton("mymod.open", "gui.mymod.open", 100, MyScreen::open));
 * Agents.agent(modelClient).answer(AgentRequest.of(model, prompt))
 *         .thenAccept(answer -> show(answer.text()));
 * }</pre>
 *
 * <p>There is one of each per game. A test builds its own {@link ToolAgent}, registry and pool
 * instead.
 */
public final class Agents {

    private static final AgentButtons BUTTONS = new AgentButtons();
    private static final ToolExecutor EXECUTOR = new ToolExecutor(WorkerPool.shared());
    private static final AtomicReference<AgentSettings> SETTINGS =
            new AtomicReference<>(AgentSettings.DEFAULTS);
    private static final AtomicReference<Path> SETTINGS_FILE = new AtomicReference<>();
    /** One save at a time, each writing the newest settings, so old ones never land last. */
    private static final Object SAVING = new Object();

    private Agents() {
        throw new AssertionError("No instances.");
    }

    /** The registry every mod adds its tools to. */
    public static ToolRegistry tools() {
        return ToolRegistry.shared();
    }

    /** The pool every tool runs on, whose limit the player sets. */
    public static WorkerPool workers() {
        return WorkerPool.shared();
    }

    /** The buttons on the agents menu. */
    public static AgentButtons buttons() {
        return BUTTONS;
    }

    /** What the player has set. */
    public static AgentSettings settings() {
        return SETTINGS.get();
    }

    /**
     * An agent that asks {@code client} and uses every registered tool.
     *
     * <p>Cheap to build: it holds nothing of its own, and reads the registry and the player's
     * tool rounds afresh for every question.
     */
    public static Agent agent(ModelClient client) {
        return new ToolAgent(client, tools(), EXECUTOR, () -> settings().toolRounds());
    }

    /**
     * Loads the player's settings from {@code settingsFile} and applies them. Called once at
     * start-up; settings changed before then are kept in memory only.
     */
    public static void start(Path settingsFile) {
        Objects.requireNonNull(settingsFile, "settingsFile");
        SETTINGS_FILE.set(settingsFile);
        apply(AgentSettings.load(settingsFile));
    }

    /**
     * Applies {@code settings} now and saves them off the calling thread.
     *
     * @return completes when they are on disk; fails if they could not be written, in which
     *         case they still apply until the game closes
     */
    public static CompletableFuture<Void> update(AgentSettings settings) {
        Objects.requireNonNull(settings, "settings");
        apply(settings);
        Path file = SETTINGS_FILE.get();
        if (file == null) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(() -> {
            synchronized (SAVING) {
                try {
                    SETTINGS.get().save(file);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        });
    }

    private static void apply(AgentSettings settings) {
        SETTINGS.set(settings);
        workers().setLimit(settings.workerLimit());
    }
}
