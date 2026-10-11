package io.github.mcagentsmods.core.shared;

import io.github.mcagentsmods.core.common.web.CoreWebRegistry;
import io.github.mcagentsmods.core.shared.environment.ModBootstrap;
import io.github.mcagentsmods.core.shared.environment.ModContext;
import io.github.mcagentsmods.core.shared.environment.ModEnvironment;
import io.github.mcagentsmods.core.shared.environment.PhysicalSide;
import io.github.mcagentsmods.core.shared.environment.SideEntrypoint;

import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * What each loader's entry point calls, so the three loaders share one start
 * and one stop instead of three copies of them.
 *
 * <p>A loader knows two things core needs — which physical side is running and
 * where the game directory is — and one moment it must report: shutdown. It
 * hands the first two to {@link #start(PhysicalSide, Path)} and calls
 * {@link #stop()} at the third. Everything between is loader-agnostic.</p>
 *
 * <h2>Threading</h2>
 *
 * <p>Both methods are synchronized. A loader calls them from its own startup and
 * shutdown threads, never from a render or tick loop.</p>
 */
public final class CoreRuntime {

    /** The mod id every loader manifest declares. */
    public static final String MOD_ID = "mcagents_core";

    private static final Logger LOGGER = Logger.getLogger("MCAgents Core");

    private static SideEntrypoint running;

    private CoreRuntime() {
    }

    /**
     * Records the side the loader reported and starts the matching half.
     *
     * <p>Idempotent: a second call while a half is running returns that half
     * rather than starting another.</p>
     *
     * @param side          The physical side the loader says is running.
     * @param gameDirectory The game directory the loader resolved.
     * @return The running half.
     */
    public static synchronized SideEntrypoint start(PhysicalSide side, Path gameDirectory) {
        Objects.requireNonNull(side, "side cannot be null");
        Objects.requireNonNull(gameDirectory, "gameDirectory cannot be null");
        if (running != null) {
            return running;
        }
        ModEnvironment.install(side);
        running = ModBootstrap.start(ModContext.of(side, gameDirectory, LOGGER));
        return running;
    }

    /**
     * Stops the running half, if any, and the web console if it is open. Safe to
     * call more than once.
     */
    public static synchronized void stop() {
        SideEntrypoint current = running;
        running = null;
        if (current != null) {
            current.stop();
        }
        // The web console's port must not outlive the game. Registrations are kept: they
        // are a mod's pages, not state, and cost nothing while nothing listens.
        CoreWebRegistry.shared().close();
    }

    /**
     * Returns the running half.
     *
     * @return The running half, or {@code null} before {@link #start} and after
     *         {@link #stop()}.
     */
    public static synchronized SideEntrypoint running() {
        return running;
    }
}
