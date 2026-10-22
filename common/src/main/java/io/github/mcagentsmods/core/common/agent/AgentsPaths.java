package io.github.mcagentsmods.core.common.agent;

import io.github.mcagentsmods.core.common.file.ModFiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Everything core keeps on disk, resolved from a game directory it is handed.
 *
 * <p>This class knows nothing about Minecraft. A loader module resolves the game directory
 * and calls {@link #under(Path)}; from there the layout is the same whether it came from a
 * real installation or from a temporary directory in a test.
 */
public final class AgentsPaths {

    /** Directory every MCAgents mod keeps its files under, inside the game directory. */
    public static final String DATA_DIRECTORY = "MCAgents";

    /**
     * Core's own directory inside {@link #DATA_DIRECTORY}. Every MCAgents mod keeps its files in
     * {@code MCAgents/{mod}/}, named for its repository.
     */
    public static final String MOD_DIRECTORY = "core";

    /**
     * Where core kept its files before 0.14.0, named for the agents mod the agent layer came
     * from. {@link #moveLegacyFiles()} moves them out.
     */
    public static final String LEGACY_DIRECTORY = "agents";

    private static final String SETTINGS_FILE = "settings.properties";
    private static final String MODELS_FILE = "models.properties";

    /** Held while files move, so two mods starting at once cannot move the same file twice. */
    private static final Object MOVING = new Object();

    private final Path dataDirectory;
    private final Path legacyDirectory;

    private AgentsPaths(Path dataDirectory, Path legacyDirectory) {
        this.dataDirectory = dataDirectory;
        this.legacyDirectory = legacyDirectory;
    }

    /**
     * Describes the layout under {@code gameDirectory}. Creates nothing — call
     * {@link #createDataDirectory()} for that.
     *
     * @param gameDirectory the Minecraft game directory
     */
    public static AgentsPaths under(Path gameDirectory) {
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        Path mods = gameDirectory.resolve(DATA_DIRECTORY);
        return new AgentsPaths(mods.resolve(MOD_DIRECTORY).normalize(),
                mods.resolve(LEGACY_DIRECTORY).normalize());
    }

    /** {@code {game_dir}/MCAgents/core} — the directory holding every file core owns. */
    public Path dataDirectory() {
        return dataDirectory;
    }

    /** {@code {game_dir}/MCAgents/core/settings.properties} — what the player set in game. */
    public Path settingsFile() {
        return dataDirectory.resolve(SETTINGS_FILE);
    }

    /** Where the global model is kept: {@code models.properties} beside the settings. */
    public Path modelsFile() {
        return dataDirectory.resolve(MODELS_FILE);
    }

    /** {@code {game_dir}/MCAgents/agents} — where the settings and the global model were kept before 0.14.0. */
    public Path legacyDirectory() {
        return legacyDirectory;
    }

    /** Creates {@link #dataDirectory()} and its parents if they do not exist yet. */
    public Path createDataDirectory() throws IOException {
        return Files.createDirectories(dataDirectory);
    }

    /**
     * Moves the settings and the global model from {@link #legacyDirectory()} into
     * {@link #dataDirectory()}, then removes the legacy directory if that left it empty. A file
     * already in the new place stays, and its old copy is left alone. Safe to call on every
     * start and from more than one mod: core calls it when the client starts, and a mod that
     * reads the global model before that calls it first.
     *
     * @return the files this call moved, in their new place
     * @throws IOException if a move was needed and failed; the files not yet moved stay put
     */
    public List<Path> moveLegacyFiles() throws IOException {
        synchronized (MOVING) {
            List<Path> moved = new ArrayList<>();
            for (String name : List.of(SETTINGS_FILE, MODELS_FILE)) {
                Path target = dataDirectory.resolve(name);
                if (ModFiles.moveIfAbsent(legacyDirectory.resolve(name), target)) {
                    moved.add(target);
                }
            }
            ModFiles.deleteIfEmpty(legacyDirectory);
            return List.copyOf(moved);
        }
    }
}
