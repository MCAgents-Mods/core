package io.github.mcagentsmods.core.common.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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

        assertEquals(gameDirectory.resolve("MCAgents").resolve("core"), paths.dataDirectory());
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
    void theFilesTheAgentsModLeftAreMovedIntoCoresDirectory() throws Exception {
        AgentsPaths paths = AgentsPaths.under(gameDirectory);
        Path legacy = gameDirectory.resolve("MCAgents").resolve("agents");
        Files.createDirectories(legacy);
        Files.writeString(legacy.resolve("settings.properties"), "worker-limit=4\n");
        Files.writeString(legacy.resolve("models.properties"), "chat.model=a/b\n");

        List<Path> moved = paths.moveLegacyFiles();

        assertEquals(List.of(paths.settingsFile(), paths.modelsFile()), moved);
        assertEquals("worker-limit=4\n", Files.readString(paths.settingsFile()));
        assertEquals("chat.model=a/b\n", Files.readString(paths.modelsFile()));
        assertFalse(Files.exists(legacy), "the emptied agents directory should be removed");
    }

    @Test
    void aFileAlreadyInCoresDirectoryIsNeverOverwritten() throws Exception {
        AgentsPaths paths = AgentsPaths.under(gameDirectory);
        Files.createDirectories(paths.legacyDirectory());
        Files.writeString(paths.legacyDirectory().resolve("settings.properties"), "old");
        paths.createDataDirectory();
        Files.writeString(paths.settingsFile(), "new");

        assertEquals(List.of(), paths.moveLegacyFiles());

        assertEquals("new", Files.readString(paths.settingsFile()));
        assertEquals("old", Files.readString(paths.legacyDirectory().resolve("settings.properties")));
    }

    @Test
    void movingWithNothingToMoveDoesNothing() throws Exception {
        AgentsPaths paths = AgentsPaths.under(gameDirectory);

        assertEquals(List.of(), paths.moveLegacyFiles());
        assertFalse(Files.exists(gameDirectory.resolve("MCAgents")));
    }

    @Test
    void aMissingGameDirectoryIsRejected() {
        assertThrows(NullPointerException.class, () -> AgentsPaths.under(null));
    }
}
