package io.github.mcagentsmods.core.mods.server;

import io.github.mcagentsmods.core.mods.environment.ServerOnly;
import io.github.mcagentsmods.core.mods.store.MinecraftDirectory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Where a dedicated server keeps its credential file.
 *
 * <p>Not where a client keeps one, and the difference matters. On a client the
 * file is shared: one {@code mcagents.json} under the player's Minecraft
 * directory, found by every MCAgents mod they install, holding the keys they
 * pay for. A server has no business reading that — it is a different machine,
 * a different owner, and a different bill. So the server's file lives under the
 * server's own run directory, in {@value #CONFIG_FOLDER}, which is where both
 * loaders already put mod configuration.</p>
 *
 * <h2>The fallback is the working directory, not the home directory</h2>
 *
 * <p>{@link MinecraftDirectory} falls back to {@code ~/.minecraft} when a
 * loader reports nothing, which is right on a client and wrong here: a server
 * is started from its own folder, and the account running it may not have a
 * Minecraft directory at all. Falling back to the process working directory
 * puts the file beside the server jar, which is where an owner looks.</p>
 */
@ServerOnly
public final class ServerRunDirectory {

    /**
     * The folder under the run directory that mod configuration lives in.
     */
    public static final String CONFIG_FOLDER = "config";

    /**
     * Not instantiable — this class is a resolver.
     */
    private ServerRunDirectory() {
    }

    /**
     * Resolves the directory the server's credential file belongs in.
     *
     * @param runDirectory The run directory the loader reported, or
     *                     {@code null} when it reported none.
     * @return The configuration directory. Never {@code null}, and not
     *         necessarily existing yet.
     */
    public static Path resolveDirectory(Path runDirectory) {
        Path base = runDirectory != null ? runDirectory : workingDirectory();
        return base.resolve(CONFIG_FOLDER);
    }

    /**
     * Resolves the server's credential file.
     *
     * @param runDirectory The run directory the loader reported, or
     *                     {@code null}.
     * @return The path to the credential file. The file itself may not exist
     *         yet.
     */
    public static Path resolveFile(Path runDirectory) {
        return resolveDirectory(runDirectory).resolve(MinecraftDirectory.FILE_NAME);
    }

    /**
     * Returns the directory the server process was started in.
     *
     * @return The working directory, falling back to {@code .} when the
     *         property is unset.
     */
    private static Path workingDirectory() {
        return Paths.get(System.getProperty("user.dir", "."));
    }
}
