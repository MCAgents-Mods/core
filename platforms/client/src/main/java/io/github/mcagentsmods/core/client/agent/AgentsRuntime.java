package io.github.mcagentsmods.core.client.agent;

import io.github.mcagentsmods.core.common.agent.Agents;
import io.github.mcagentsmods.core.common.agent.AgentsPaths;
import io.github.mcagentsmods.core.common.model.Models;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The agent layer's start-up on the client, shared by every loader.
 *
 * <p>Each loader's client entry point finds the game directory and calls {@link #start(Path)},
 * after it registers the G key. It loads the player's settings and the global model; the tool registry, the worker
 * pool and the menu's buttons need no start-up and are ready before it runs.
 */
public final class AgentsRuntime {

    private static final Logger LOGGER = Logger.getLogger("MCAgents Core");

    private static final AtomicReference<AgentsPaths> PATHS = new AtomicReference<>();

    private AgentsRuntime() {
        throw new AssertionError("No instances.");
    }

    /**
     * Starts the agent layer. Safe to call more than once; only the first call does anything.
     *
     * <p>Never throws: a failure is logged and the game carries on with what could be set up,
     * the default settings at worst.
     *
     * @param gameDirectory the Minecraft game directory, as the loader reports it
     */
    public static void start(Path gameDirectory) {
        AgentsPaths paths = AgentsPaths.under(gameDirectory);
        if (!PATHS.compareAndSet(null, paths)) {
            return;
        }
        try {
            paths.createDataDirectory();
        } catch (IOException | RuntimeException e) {
            LOGGER.log(Level.WARNING, "Could not create " + paths.dataDirectory(), e);
        }
        try {
            // Before 0.14.0 the files were in MCAgents/agents/; a player who updates keeps them.
            for (Path moved : paths.moveLegacyFiles()) {
                LOGGER.info("Moved " + moved.getFileName() + " from " + paths.legacyDirectory()
                        + " to " + paths.dataDirectory());
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.log(Level.WARNING, "Could not move the files in " + paths.legacyDirectory()
                    + " to " + paths.dataDirectory(), e);
        }
        try {
            // The player's worker limit and tool rounds; a missing or broken file gives the
            // defaults, 2 workers and 1 round.
            Agents.start(paths.settingsFile());
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Could not load the agents settings; using the defaults", e);
        }
        try {
            // The global model every task with no model of its own uses. A mod may have read
            // the same file first; then this does nothing.
            Models.start(paths.modelsFile());
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Could not load the global model; using the defaults", e);
        }
        LOGGER.info("Agents started, settings in " + paths.dataDirectory());
    }

    /** The agent layer's layout on disk, once {@link #start(Path)} has run. */
    public static Optional<AgentsPaths> paths() {
        return Optional.ofNullable(PATHS.get());
    }
}
