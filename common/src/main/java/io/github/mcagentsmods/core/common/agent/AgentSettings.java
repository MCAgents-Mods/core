package io.github.mcagentsmods.core.common.agent;

import io.github.mcagentsmods.core.api.agent.AgentRequest;
import io.github.mcagentsmods.core.tool.WorkerPool;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/**
 * What the player sets in game: how many tool calls run at once, and how many rounds of tool
 * calls one question may take.
 *
 * <p>Kept in {@code {game_dir}/MCAgents/core/settings.properties}. A file that is missing,
 * unreadable or holds a value out of range is not an error: the value falls back to its default
 * or is clamped into range, so a hand-edited file can never stop the mod.
 *
 * @param workerLimit how many tool calls run at once, {@link WorkerPool#MIN_LIMIT} to
 *                    {@link WorkerPool#MAX_LIMIT}; 2 by default
 * @param toolRounds  how many times the model may ask for tools before it must answer, 1 to
 *                    {@link AgentRequest#MAX_TOOL_ROUNDS}; 1 by default
 */
public record AgentSettings(int workerLimit, int toolRounds) {

    public static final int DEFAULT_TOOL_ROUNDS = 1;

    public static final AgentSettings DEFAULTS =
            new AgentSettings(WorkerPool.DEFAULT_LIMIT, DEFAULT_TOOL_ROUNDS);

    private static final String WORKER_LIMIT = "worker-limit";
    private static final String TOOL_ROUNDS = "tool-rounds";

    public AgentSettings {
        workerLimit = WorkerPool.clampLimit(workerLimit);
        toolRounds = clampToolRounds(toolRounds);
    }

    /** {@code rounds} clamped to 1 to {@link AgentRequest#MAX_TOOL_ROUNDS}. */
    public static int clampToolRounds(int rounds) {
        return Math.max(1, Math.min(AgentRequest.MAX_TOOL_ROUNDS, rounds));
    }

    public AgentSettings withWorkerLimit(int limit) {
        return new AgentSettings(limit, toolRounds);
    }

    public AgentSettings withToolRounds(int rounds) {
        return new AgentSettings(workerLimit, rounds);
    }

    /**
     * Reads the settings from {@code file}, falling back to {@link #DEFAULTS} for anything
     * missing or unreadable.
     */
    public static AgentSettings load(Path file) {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException | IllegalArgumentException e) {
            // Missing on first start, unreadable on a locked or hand-mangled file: either way
            // the defaults answer, and the next save writes a good file.
            return DEFAULTS;
        }
        return new AgentSettings(
                integer(properties, WORKER_LIMIT, DEFAULTS.workerLimit()),
                integer(properties, TOOL_ROUNDS, DEFAULTS.toolRounds()));
    }

    /**
     * Writes the settings to {@code file}, creating its directory if needed.
     *
     * <p>Written beside the file and moved over it, so a game that stops mid-write leaves the
     * old settings rather than half of the new ones.
     */
    public void save(Path file) throws IOException {
        Path directory = file.toAbsolutePath().getParent();
        Files.createDirectories(directory);
        Properties properties = new Properties();
        properties.setProperty(WORKER_LIMIT, Integer.toString(workerLimit));
        properties.setProperty(TOOL_ROUNDS, Integer.toString(toolRounds));
        Path temporary = Files.createTempFile(directory, "settings", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                properties.store(writer, "MCAgents agents settings");
            }
            try {
                Files.move(temporary, file,
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static int integer(Properties properties, String key, int fallback) {
        String value = properties.getProperty(key);
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.strip());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
