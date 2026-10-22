package io.github.mcagentsmods.core.common.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModFilesTest {

    @TempDir
    Path gameDirectory;

    @Test
    void movesAFileIntoTheModsDirectoryCreatingIt() throws Exception {
        Path legacy = gameDirectory.resolve("MCAgents/data.db");
        Path target = gameDirectory.resolve("MCAgents/chat/data.db");
        Files.createDirectories(legacy.getParent());
        Files.writeString(legacy, "rows");

        assertTrue(ModFiles.moveIfAbsent(legacy, target));

        assertFalse(Files.exists(legacy));
        assertEquals("rows", Files.readString(target));
    }

    @Test
    void movesADirectoryWithWhatIsInIt() throws Exception {
        Path legacy = gameDirectory.resolve("MCAgents/mcps");
        Path target = gameDirectory.resolve("MCAgents/chat/mcps");
        Files.createDirectories(legacy.resolve("owner/repo"));
        Files.writeString(legacy.resolve("owner/repo/mcp.json"), "{}");

        assertTrue(ModFiles.moveIfAbsent(legacy, target));

        assertEquals("{}", Files.readString(target.resolve("owner/repo/mcp.json")));
    }

    @Test
    void neverOverwritesAFileAlreadyInPlace() throws Exception {
        Path legacy = gameDirectory.resolve("old.properties");
        Path target = gameDirectory.resolve("core/new.properties");
        Files.writeString(legacy, "old");
        Files.createDirectories(target.getParent());
        Files.writeString(target, "new");

        assertFalse(ModFiles.moveIfAbsent(legacy, target));

        assertEquals("new", Files.readString(target));
        assertEquals("old", Files.readString(legacy));
    }

    @Test
    void doesNothingWhenThereIsNothingToMove() throws Exception {
        assertFalse(ModFiles.moveIfAbsent(gameDirectory.resolve("missing"), gameDirectory.resolve("core/missing")));
        assertFalse(Files.exists(gameDirectory.resolve("core")));
    }

    @Test
    void deletesOnlyAnEmptyDirectory() throws Exception {
        Path empty = Files.createDirectories(gameDirectory.resolve("empty"));
        Path full = Files.createDirectories(gameDirectory.resolve("full"));
        Files.writeString(full.resolve("kept"), "");

        assertTrue(ModFiles.deleteIfEmpty(empty));
        assertFalse(ModFiles.deleteIfEmpty(full));
        assertFalse(ModFiles.deleteIfEmpty(gameDirectory.resolve("missing")));

        assertFalse(Files.exists(empty));
        assertTrue(Files.exists(full.resolve("kept")));
    }

    @Test
    void nullsAreRejected() {
        assertThrows(NullPointerException.class, () -> ModFiles.moveIfAbsent(null, gameDirectory));
        assertThrows(NullPointerException.class, () -> ModFiles.moveIfAbsent(gameDirectory, null));
    }
}
