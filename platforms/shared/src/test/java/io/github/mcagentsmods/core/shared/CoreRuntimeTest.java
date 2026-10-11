package io.github.mcagentsmods.core.shared;

import io.github.mcagentsmods.core.shared.client.ClientEntrypoint;
import io.github.mcagentsmods.core.shared.environment.ModEnvironment;
import io.github.mcagentsmods.core.shared.environment.PhysicalSide;
import io.github.mcagentsmods.core.shared.environment.SideEntrypoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("CoreRuntime")
class CoreRuntimeTest {

    @AfterEach
    void restore() {
        CoreRuntime.stop();
        ModEnvironment.reset();
    }

    @Test
    @DisplayName("starts the half for the reported side, once")
    void startsOnce(@TempDir Path gameDirectory) {
        Logger.getLogger("MCAgents Core").setLevel(Level.OFF);

        SideEntrypoint first = CoreRuntime.start(PhysicalSide.CLIENT, gameDirectory);
        SideEntrypoint second = CoreRuntime.start(PhysicalSide.CLIENT, gameDirectory);

        assertInstanceOf(ClientEntrypoint.class, first);
        assertSame(first, second);
        assertEquals(PhysicalSide.CLIENT, ModEnvironment.current());
    }

    @Test
    @DisplayName("stops idempotently")
    void stopsIdempotently(@TempDir Path gameDirectory) {
        Logger.getLogger("MCAgents Core").setLevel(Level.OFF);
        CoreRuntime.start(PhysicalSide.DEDICATED_SERVER, gameDirectory);

        CoreRuntime.stop();
        CoreRuntime.stop();

        assertNull(CoreRuntime.running());
    }

    @Test
    @DisplayName("refuses a missing side or directory")
    void refusesNulls(@TempDir Path gameDirectory) {
        assertThrows(NullPointerException.class, () -> CoreRuntime.start(null, gameDirectory));
        assertThrows(NullPointerException.class, () -> CoreRuntime.start(PhysicalSide.CLIENT, null));
    }
}
