package io.github.mcagentsmods.core.shared.client;

import io.github.mcagentsmods.core.shared.environment.ClientOnly;
import io.github.mcagentsmods.core.shared.environment.ModBootstrap;
import io.github.mcagentsmods.core.shared.environment.ModContext;
import io.github.mcagentsmods.core.shared.environment.ModEnvironment;
import io.github.mcagentsmods.core.shared.environment.PhysicalSide;
import io.github.mcagentsmods.core.shared.environment.SideEntrypoint;
import io.github.mcagentsmods.core.shared.environment.WrongSideException;
import io.github.mcagentsmods.core.shared.store.MinecraftDirectory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ClientEntrypoint}.
 *
 * <p>The client half never imports the server half — {@code SideSeparationTest}
 * checks that — so nothing here leans on the other side being present.</p>
 */
@DisplayName("ClientEntrypoint")
class ClientEntrypointTest {

    /**
     * A logger that discards, so a test run does not print the entry point's
     * startup lines.
     */
    private static Logger quietLogger() {
        Logger logger = Logger.getAnonymousLogger();
        logger.setLevel(Level.OFF);
        return logger;
    }

    @AfterEach
    void restore() {
        ModEnvironment.reset();
    }

    @Test
    @DisplayName("is marked as client-only")
    void isMarked() {
        // Cheap, and it keeps the convention from rotting: a class that moves
        // into this module without the marker fails here rather than in review.
        assertNotNull(ClientEntrypoint.class.getAnnotation(ClientOnly.class));
        assertNotNull(ClientCredentialDisplay.class.getAnnotation(ClientOnly.class));
    }

    @Test
    @DisplayName("refuses to exist on a dedicated server")
    void refusesToBeConstructedOnAServer() {
        ModEnvironment.install(PhysicalSide.DEDICATED_SERVER);

        assertThrows(WrongSideException.class, ClientEntrypoint::new);
    }

    @Test
    @DisplayName("is what the bootstrap resolves for the client side")
    void isResolvedByName() {
        ModEnvironment.install(PhysicalSide.CLIENT);

        SideEntrypoint loaded = ModBootstrap.load(PhysicalSide.CLIENT);

        // The bootstrap names this class as a string and never as a type. This
        // is the only place both ends of that seam exist at once, so it is the
        // only place the name can be checked against the class.
        assertInstanceOf(ClientEntrypoint.class, loaded);
        assertEquals(ClientEntrypoint.class.getName(), ModBootstrap.CLIENT_ENTRYPOINT);
        assertEquals(PhysicalSide.CLIENT, loaded.side());
    }

    @Test
    @DisplayName("opens the shared credential file under the directory the loader reported")
    void opensTheSharedFile(@TempDir Path gameDirectory) {
        ModEnvironment.install(PhysicalSide.CLIENT);
        ClientEntrypoint entrypoint = new ClientEntrypoint();

        entrypoint.start(ModContext.of(PhysicalSide.CLIENT, gameDirectory, quietLogger()));

        Path expected = gameDirectory.resolve(MinecraftDirectory.FILE_NAME);
        assertEquals(expected, entrypoint.credentialFile());
        assertTrue(Files.exists(expected), "the shared credential file should have been created");
        assertNotNull(entrypoint.commands());
        assertTrue(entrypoint.describe().contains(expected.toString()));
    }

    @Test
    @DisplayName("refuses to start on the other side")
    void refusesAServerContext(@TempDir Path gameDirectory) {
        ModEnvironment.install(PhysicalSide.CLIENT);
        ClientEntrypoint entrypoint = new ClientEntrypoint();

        // Constructed on a client, handed a server's context: a wiring mistake
        // rather than a real dedicated server, and it must not be papered over.
        assertThrows(WrongSideException.class, () -> entrypoint.start(
                ModContext.of(PhysicalSide.DEDICATED_SERVER, gameDirectory, quietLogger())));
    }

    @Test
    @DisplayName("says it has not started rather than handing out a null")
    void refusesToServeBeforeStarting() {
        ModEnvironment.install(PhysicalSide.CLIENT);
        ClientEntrypoint entrypoint = new ClientEntrypoint();

        assertThrows(IllegalStateException.class, entrypoint::commands);
        assertNull(entrypoint.credentialFile());
        assertTrue(entrypoint.describe().contains("not started"));
    }

    @Test
    @DisplayName("can be stopped twice, and before it ever started")
    void stopsIdempotently(@TempDir Path gameDirectory) {
        ModEnvironment.install(PhysicalSide.CLIENT);
        ClientEntrypoint entrypoint = new ClientEntrypoint();

        entrypoint.stop();
        entrypoint.start(ModContext.of(PhysicalSide.CLIENT, gameDirectory, quietLogger()));
        entrypoint.stop();
        entrypoint.stop();

        assertNull(entrypoint.credentialFile());
    }
}
