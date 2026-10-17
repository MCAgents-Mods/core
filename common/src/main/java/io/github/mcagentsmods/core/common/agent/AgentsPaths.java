package io.github.mcagentsmods.core.common.agent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Everything the agent layer keeps on disk, resolved from a game directory it is handed.
 *
 * <p>This class knows nothing about Minecraft. A loader module resolves the game directory
 * and calls {@link #under(Path)}; from there the layout is the same whether it came from a
 * real installation or from a temporary directory in a test.
 */
public final class AgentsPaths {

    /** Directory every MCAgents mod keeps its files under, inside the game directory. */
    public static final String DATA_DIRECTORY = "MCAgents";

    /**
     * The agent layer's own directory inside {@link #DATA_DIRECTORY}. Named for the agents mod
     * this layer came from, so a player's settings carry over to core.
     */
    public static final String AGENTS_DIRECTORY = "agents";

    private final Path dataDirectory;

    private AgentsPaths(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    /**
     * Describes the layout under {@code gameDirectory}. Creates nothing — call
     * {@link #createDataDirectory()} for that.
     *
     * @param gameDirectory the Minecraft game directory
     */
    public static AgentsPaths under(Path gameDirectory) {
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        return new AgentsPaths(gameDirectory
                .resolve(DATA_DIRECTORY)
                .resolve(AGENTS_DIRECTORY)
                .normalize());
    }

    /** {@code {game_dir}/MCAgents/agents} — the directory holding every file the agent layer owns. */
    public Path dataDirectory() {
        return dataDirectory;
    }

    /** {@code {game_dir}/MCAgents/agents/settings.properties} — what the player set in game. */
    public Path settingsFile() {
        return dataDirectory.resolve("settings.properties");
    }

    /** Creates {@link #dataDirectory()} and its parents if they do not exist yet. */
    /** Where the global model is kept: {@code models.properties} beside the settings. */
    public Path modelsFile() {
        return dataDirectory.resolve("models.properties");
    }

    public Path createDataDirectory() throws IOException {
        return Files.createDirectories(dataDirectory);
    }
}
