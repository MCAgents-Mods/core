package io.github.mcagentsmods.core.common.agent;

import io.github.mcagentsmods.core.api.agent.AgentRequest;
import io.github.mcagentsmods.core.api.button.AgentButton;
import io.github.mcagentsmods.core.tool.WorkerPool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentSettingsTest {

    @TempDir
    Path directory;

    @Test
    void defaultsToTwoWorkersAndOneRound() {
        assertEquals(2, AgentSettings.DEFAULTS.workerLimit());
        assertEquals(1, AgentSettings.DEFAULTS.toolRounds());
        assertEquals(AgentSettings.DEFAULTS, AgentSettings.load(directory.resolve("missing.properties")));
    }

    @Test
    void savesAndLoadsBack() throws Exception {
        Path file = directory.resolve("nested").resolve("settings.properties");
        new AgentSettings(5, 3).save(file);

        assertEquals(new AgentSettings(5, 3), AgentSettings.load(file));
        try (var leftovers = Files.list(file.getParent())) {
            assertEquals(List.of(file), leftovers.toList());
        }
    }

    @Test
    void clampsValuesOutOfRange() {
        AgentSettings settings = new AgentSettings(0, 99);

        assertEquals(WorkerPool.MIN_LIMIT, settings.workerLimit());
        assertEquals(AgentRequest.MAX_TOOL_ROUNDS, settings.toolRounds());
        assertEquals(WorkerPool.MAX_LIMIT, settings.withWorkerLimit(1000).workerLimit());
    }

    @Test
    void aMangledFileFallsBackValueByValue() throws Exception {
        Path file = directory.resolve("settings.properties");
        Files.writeString(file, "worker-limit=four\ntool-rounds=3\n");

        assertEquals(new AgentSettings(2, 3), AgentSettings.load(file));
    }

    @Test
    void theSettingsFileLivesInTheModsDirectory() {
        assertEquals(directory.resolve("MCAgents").resolve("core").resolve("settings.properties"),
                AgentsPaths.under(directory).settingsFile());
    }

    @Test
    void buttonsAreOrderedAndIdsAreUnique() throws Exception {
        AgentButtons buttons = new AgentButtons();
        buttons.register(new AgentButton("b.second", "Second", 20, () -> { }));
        AutoCloseable first = buttons.register(new AgentButton("a.first", "First", 10, () -> { }));
        buttons.register(new AgentButton("c.tied", "Tied", 20, () -> { }));

        assertEquals(List.of("a.first", "b.second", "c.tied"),
                buttons.list().stream().map(AgentButton::id).toList());
        assertThrows(IllegalArgumentException.class,
                () -> buttons.register(new AgentButton("a.first", "Again", 0, () -> { })));

        first.close();
        assertEquals(2, buttons.list().size());
    }

    @Test
    void refusesAMalformedButtonOrRequest() {
        assertThrows(IllegalArgumentException.class, () -> new AgentButton("Has Space", "x", 0, () -> { }));
        assertThrows(IllegalArgumentException.class, () -> new AgentButton("ok", " ", 0, () -> { }));
        assertThrows(IllegalArgumentException.class, () -> AgentRequest.of("m", " "));
        assertThrows(IllegalArgumentException.class, () -> AgentRequest.of("m", "p").withToolRounds(9));
        assertTrue(AgentRequest.of("m", "p").conversation().get(0).text().equals("p"));
    }
}
