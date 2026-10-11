package io.github.mcagentsmods.core.shared.server;

import io.github.mcagentsmods.core.shared.environment.ModBootstrap;
import io.github.mcagentsmods.core.shared.environment.ModContext;
import io.github.mcagentsmods.core.shared.environment.ModEnvironment;
import io.github.mcagentsmods.core.shared.environment.PhysicalSide;
import io.github.mcagentsmods.core.shared.environment.ServerOnly;
import io.github.mcagentsmods.core.shared.environment.SideEntrypoint;
import io.github.mcagentsmods.core.shared.store.MinecraftDirectory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ServerEntrypoint}.
 *
 * <p>The server half never imports the client half — {@code SideSeparationTest}
 * checks that — so what these tests start is what a dedicated server starts.</p>
 */
@DisplayName("ServerEntrypoint")
class ServerEntrypointTest {

    /**
     * A logger that discards, so a test run does not print startup lines.
     */
    private static Logger quietLogger() {
        Logger logger = Logger.getAnonymousLogger();
        logger.setLevel(Level.OFF);
        return logger;
    }

    /**
     * Starts an entry point against a run directory.
     *
     * @param runDirectory Where the server was started.
     * @return The started entry point.
     */
    private static ServerEntrypoint started(Path runDirectory) {
        ServerEntrypoint entrypoint = new ServerEntrypoint();
        entrypoint.start(ModContext.of(PhysicalSide.DEDICATED_SERVER, runDirectory, quietLogger()));
        return entrypoint;
    }

    @AfterEach
    void restore() {
        ModEnvironment.reset();
    }

    @Test
    @DisplayName("is marked as server-only")
    void isMarked() {
        assertNotNull(ServerEntrypoint.class.getAnnotation(ServerOnly.class));
        assertNotNull(ServerCommandAuthority.class.getAnnotation(ServerOnly.class));
        assertNotNull(ServerRunDirectory.class.getAnnotation(ServerOnly.class));
    }

    @Test
    @DisplayName("starts on a dedicated server with no client class in sight")
    void startsWithoutTheClientHalf(@TempDir Path runDirectory) {
        ModEnvironment.install(PhysicalSide.DEDICATED_SERVER);

        SideEntrypoint loaded = ModBootstrap.start(
                ModContext.of(PhysicalSide.DEDICATED_SERVER, runDirectory, quietLogger()));

        assertInstanceOf(ServerEntrypoint.class, loaded);
        assertEquals(ServerEntrypoint.class.getName(), ModBootstrap.DEDICATED_SERVER_ENTRYPOINT);
        assertEquals(PhysicalSide.DEDICATED_SERVER, loaded.side());
    }


    @Test
    @DisplayName("also starts inside a client, because a single player world is a server")
    void startsOnAClientToo(@TempDir Path runDirectory) {
        ModEnvironment.install(PhysicalSide.CLIENT);

        // No guard refuses this, on purpose: server logic belongs wherever a
        // world is hosted, and a client hosts one.
        ServerEntrypoint entrypoint = started(runDirectory);

        assertNotNull(entrypoint.credentialFile());
    }

    @Test
    @DisplayName("keeps its credentials under the run directory, not a player's Minecraft folder")
    void usesTheRunDirectory(@TempDir Path runDirectory) {
        ServerEntrypoint entrypoint = started(runDirectory);

        Path expected = runDirectory
                .resolve(ServerRunDirectory.CONFIG_FOLDER)
                .resolve(MinecraftDirectory.FILE_NAME);

        assertEquals(expected, entrypoint.credentialFile());
        assertTrue(Files.exists(expected), "the server credential file should have been created");
    }

    @Test
    @DisplayName("says it has not started rather than working on a null store")
    void refusesToServeBeforeStarting() {
        ServerEntrypoint entrypoint = new ServerEntrypoint();

        assertThrows(IllegalStateException.class,
                () -> entrypoint.status(CommandCaller.console()));
        assertTrue(entrypoint.describe().contains("not started"));
    }

    @Test
    @DisplayName("checks the caller on every credential operation")
    void checksEveryOperation(@TempDir Path runDirectory) {
        ServerEntrypoint entrypoint = started(runDirectory);
        CommandCaller player = CommandCaller.player(UUID.randomUUID(), "Steve", 0);

        // The check lives in the entry point rather than in the command, so a
        // second way in — a console command, a panel, an RCON bridge — cannot
        // reach the store without passing it.
        List<String> status = entrypoint.status(player);
        assertEquals(1, status.size());
        assertTrue(status.get(0).contains("not allowed"), status.get(0));

        assertTrue(entrypoint.reload(player).contains("not allowed"));
        assertTrue(entrypoint.addToken(player, "openai", "sk-secret").contains("not allowed"));
        assertTrue(entrypoint.removeToken(player, "openai", "#1:cret").contains("not allowed"));
    }

    @Test
    @DisplayName("never echoes a credential back, allowed or refused")
    void neverEchoesACredential(@TempDir Path runDirectory) {
        ServerEntrypoint entrypoint = started(runDirectory);
        String token = "sk-or-v1-0123456789abcdef";

        String refused = entrypoint.addToken(CommandCaller.player(UUID.randomUUID(), "Steve", 0),
                "openrouter", token);
        String allowed = entrypoint.addToken(CommandCaller.console(), "openrouter", token);

        assertFalse(refused.contains(token), refused);
        assertFalse(allowed.contains(token), allowed);
    }

    @Test
    @DisplayName("lets the owner through")
    void letsTheOwnerThrough(@TempDir Path runDirectory) {
        ServerEntrypoint entrypoint = started(runDirectory);

        List<String> status = entrypoint.status(CommandCaller.console());

        assertTrue(status.size() > 1, "the owner should see a line per platform");
        assertFalse(status.get(0).contains("not allowed"), status.get(0));
    }

    @Test
    @DisplayName("can be stopped twice, and before it ever started")
    void stopsIdempotently(@TempDir Path runDirectory) {
        ServerEntrypoint entrypoint = new ServerEntrypoint();

        entrypoint.stop();
        entrypoint.start(ModContext.of(PhysicalSide.DEDICATED_SERVER, runDirectory, quietLogger()));
        entrypoint.stop();
        entrypoint.stop();

        assertTrue(entrypoint.describe().contains("not started"));
    }
}
