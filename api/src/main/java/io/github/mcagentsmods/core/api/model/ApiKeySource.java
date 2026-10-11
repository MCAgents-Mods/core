package io.github.mcagentsmods.core.api.model;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Supplies the key a request should use, and is told when one is rejected.
 *
 * <p>The model client depends on this rather than on the database, so key rotation can be
 * exercised without a database and so the client never learns where keys are stored.
 *
 * <p>Nothing here ever renders or logs a key value. Where keys are stored, and
 * how they are protected at rest, is the consuming mod's business.
 */
public interface ApiKeySource {

    /** The gateway key to use next, or empty when the player has none active. */
    CompletableFuture<Optional<String>> next();

    /**
     * The key {@code platform} should be called with next, or empty when the player has none
     * active for it.
     *
     * <p>By default the gateway key answers for the gateway and nothing answers for any other
     * platform. A source that holds one list of keys holds gateway keys, and handing one to
     * another provider would send it to a host it was never issued for. A source that stores
     * keys per platform overrides this.
     *
     * @param platform the platform id a route resolved to
     */
    default CompletableFuture<Optional<String>> next(String platform) {
        return ModelPlatform.isOpenRouter(platform)
                ? next()
                : CompletableFuture.completedFuture(Optional.empty());
    }

    /**
     * Reports that the provider rejected {@code key}.
     *
     * <p>The key is deactivated rather than deleted, so the player can see what happened
     * instead of watching a key silently vanish.
     */
    CompletableFuture<Void> reject(String key);
}
