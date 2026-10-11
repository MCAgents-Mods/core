package io.github.mcagentsmods.core.mods.server;

import io.github.mcagentsmods.core.mods.store.MinecraftDirectory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ServerRunDirectory}.
 *
 * <p>The point of this class is one difference from the client: a server's
 * credentials belong to the server's owner and must never be read from, or
 * written to, a player's Minecraft directory. So that is what the tests check
 * — including the fallback, which is where the mistake would actually
 * happen.</p>
 */
@DisplayName("ServerRunDirectory")
class ServerRunDirectoryTest {

    @Test
    @DisplayName("puts the credential file under the run directory's config folder")
    void usesTheConfigFolder(@TempDir Path runDirectory) {
        Path resolved = ServerRunDirectory.resolveFile(runDirectory);

        assertEquals(
                runDirectory.resolve(ServerRunDirectory.CONFIG_FOLDER).resolve(MinecraftDirectory.FILE_NAME),
                resolved);
    }

    @Test
    @DisplayName("shares the file name with the client, so the format is recognisable")
    void sharesTheFileName(@TempDir Path runDirectory) {
        assertEquals(MinecraftDirectory.FILE_NAME,
                ServerRunDirectory.resolveFile(runDirectory).getFileName().toString());
    }

    @Test
    @DisplayName("falls back to the working directory, never to a home directory")
    void fallsBackToTheWorkingDirectory() {
        Path resolved = ServerRunDirectory.resolveDirectory(null);

        Path working = Paths.get(System.getProperty("user.dir", "."));
        assertEquals(working.resolve(ServerRunDirectory.CONFIG_FOLDER), resolved);
    }

    @Test
    @DisplayName("never resolves into the conventional Minecraft directory")
    void staysOutOfTheMinecraftDirectory(@TempDir Path runDirectory) {
        // The client's file is shared between every MCAgents mod a player
        // installs and holds keys they pay for. A server reaching into it would
        // be spending someone else's money.
        Path clientFile = MinecraftDirectory.resolveFile(null);
        Path serverFile = ServerRunDirectory.resolveFile(runDirectory);

        assertFalse(serverFile.equals(clientFile));
        assertTrue(serverFile.startsWith(runDirectory));
    }
}
