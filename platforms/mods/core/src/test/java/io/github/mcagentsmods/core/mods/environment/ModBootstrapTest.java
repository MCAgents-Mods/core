package io.github.mcagentsmods.core.mods.environment;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ModBootstrap} and {@link ModContext} on the shared module's
 * own classpath.
 *
 * <p>Neither half is present here — {@code platforms:mods:core} depends on
 * neither — which makes this the right place to check what the bootstrap does
 * when a half is missing. That is not a hypothetical: a distribution built for
 * one side only is exactly this situation, and the failure has to say so rather
 * than surfacing as a bare {@code ClassNotFoundException}.</p>
 *
 * <p>The tests that start a half for real live in the two side modules, where
 * that half is on the classpath and the other still is not.</p>
 */
@DisplayName("ModBootstrap")
class ModBootstrapTest {

    @AfterEach
    void restore() {
        ModEnvironment.reset();
    }

    @Test
    @DisplayName("names a different entry point for each side")
    void namesOneEntrypointPerSide() {
        assertEquals(ModBootstrap.CLIENT_ENTRYPOINT, ModBootstrap.entrypointFor(PhysicalSide.CLIENT));
        assertEquals(ModBootstrap.DEDICATED_SERVER_ENTRYPOINT,
                ModBootstrap.entrypointFor(PhysicalSide.DEDICATED_SERVER));
    }

    @Test
    @DisplayName("names each entry point inside its own module's package")
    void namesTheRightPackages() {
        // The names are the seam. If one drifts from the class it points at,
        // the loader fails at startup with a missing class — so the shape is
        // pinned here, and the names are resolved for real in the side modules.
        assertTrue(ModBootstrap.CLIENT_ENTRYPOINT.startsWith("io.github.mcagentsmods.core.mods.client."),
                ModBootstrap.CLIENT_ENTRYPOINT);
        assertTrue(ModBootstrap.DEDICATED_SERVER_ENTRYPOINT.startsWith("io.github.mcagentsmods.core.mods.server."),
                ModBootstrap.DEDICATED_SERVER_ENTRYPOINT);
    }

    @Test
    @DisplayName("refuses a null side")
    void refusesNullSide() {
        assertThrows(NullPointerException.class, () -> ModBootstrap.entrypointFor(null));
        assertThrows(NullPointerException.class, () -> ModBootstrap.load(null));
    }

    @Test
    @DisplayName("explains which half is missing rather than throwing a bare reflection failure")
    void explainsAMissingHalf() {
        for (PhysicalSide side : PhysicalSide.values()) {
            IllegalStateException thrown =
                    assertThrows(IllegalStateException.class, () -> ModBootstrap.load(side));

            String message = thrown.getMessage();
            assertTrue(message.contains(ModBootstrap.entrypointFor(side)), message);
            assertTrue(message.contains(side.code()), message);
        }
    }

    @Test
    @DisplayName("refuses a null context")
    void refusesNullContext() {
        assertThrows(NullPointerException.class, () -> ModBootstrap.start(null));
    }

    @Test
    @DisplayName("builds a context that carries the side the loader reported")
    void contextCarriesTheSide() {
        ModContext context = ModContext.of(PhysicalSide.DEDICATED_SERVER, null, Logger.getAnonymousLogger());

        assertEquals(PhysicalSide.DEDICATED_SERVER, context.side());
        assertTrue(context.requestTimeout().toSeconds() > 0);
    }

    @Test
    @DisplayName("refuses a context that could not work")
    void refusesAnUnusableContext() {
        Logger logger = Logger.getAnonymousLogger();

        assertThrows(NullPointerException.class,
                () -> new ModContext(null, null, logger, Duration.ofSeconds(60)));
        assertThrows(NullPointerException.class,
                () -> new ModContext(PhysicalSide.CLIENT, null, null, Duration.ofSeconds(60)));
        assertThrows(IllegalArgumentException.class,
                () -> new ModContext(PhysicalSide.CLIENT, null, logger, Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> new ModContext(PhysicalSide.CLIENT, null, logger, Duration.ofSeconds(-1)));
    }
}
