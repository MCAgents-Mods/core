package io.github.mcagentsmods.core.shared.environment;

import io.github.mcagentsmods.core.api.llm.LlmCredentials;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Everything a side entry point is given when it starts.
 *
 * <p>Assembled by the loader, because only the loader knows any of it: which
 * side it is, where the game directory is, and which logger to report through.
 * Passing it as one value rather than four arguments is what lets both halves
 * share a single {@link SideEntrypoint} signature.</p>
 *
 * @param side The physical side this process is. Comes from the loader rather
 *             than from {@link ModEnvironment} so the value the entry point
 *             acts on is the same one the bootstrap dispatched on.
 * @param gameDirectory The directory the loader reported, or {@code null} when
 *                      it reported none. Never resolved here — what it means
 *                      differs by side, and each half resolves it its own way.
 * @param logger Where to report problems.
 * @param requestTimeout How long one model request may take before it is
 *                       abandoned.
 */
public record ModContext(
        PhysicalSide side,
        Path gameDirectory,
        Logger logger,
        Duration requestTimeout) {

    /**
     * Validates the components.
     *
     * @throws NullPointerException When the side, logger, or timeout is
     *                              {@code null}. The game directory may be
     *                              {@code null}: a loader is allowed not to
     *                              know, and each side has a fallback.
     * @throws IllegalArgumentException When the timeout is not positive.
     */
    public ModContext {
        Objects.requireNonNull(side, "side cannot be null");
        Objects.requireNonNull(logger, "logger cannot be null");
        Objects.requireNonNull(requestTimeout, "requestTimeout cannot be null");

        if (requestTimeout.isZero() || requestTimeout.isNegative()) {
            throw new IllegalArgumentException("requestTimeout must be positive");
        }
    }

    /**
     * Builds a context with the default request timeout.
     *
     * @param side The physical side this process is.
     * @param gameDirectory The directory the loader reported, or {@code null}.
     * @param logger Where to report problems.
     * @return The new context.
     */
    public static ModContext of(PhysicalSide side, Path gameDirectory, Logger logger) {
        return new ModContext(side, gameDirectory, logger, LlmCredentials.DEFAULT_TIMEOUT);
    }
}
