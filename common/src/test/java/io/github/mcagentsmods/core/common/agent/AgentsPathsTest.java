package io.github.mcagentsmods.core.common.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentsPathsTest {

    @TempDir
    Path gameDirectory;

    @Test
    void theModOwnsItsOwnDirectoryUnderMCAgents() {
        AgentsPaths paths = AgentsPaths.under(gameDirectory);

        assertEquals(gameDirectory.resolve("MCAgents").resolve("agents"), paths.dataDirectory());
    }

    @Test
    void describingTheLayoutCreatesNothing() {
        AgentsPaths.under(gameDirectory);

        assertFalse(Files.exists(gameDirectory.resolve("MCAgents")));
    }

    @Test
    void createDataDirectoryCreatesItAndIsSafeToRepeat() throws Exception {
        AgentsPaths paths = AgentsPaths.under(gameDirectory);

        paths.createDataDirectory();
        paths.createDataDirectory();

        assertTrue(Files.isDirectory(paths.dataDirectory()));
    }

    @Test
    void aMissingGameDirectoryIsRejected() {
        assertThrows(NullPointerException.class, () -> AgentsPaths.under(null));
    }
}
