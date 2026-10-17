package io.github.mcagentsmods.core.api.model;

import java.util.concurrent.CompletableFuture;

/**
 * Decides how a model is reached: directly, or through OpenRouter.
 *
 * <p>The model client depends on this rather than on storage, for the same reason it depends
 * on {@link ApiKeySource} rather than on the database — routing can then be exercised without
 * a database, and the client never learns where a base URL is configured.
 *
 * <p><strong>Resolution by model name never fails.</strong> Every unknown ends in the gateway route, because
 * a player whose direct URL is missing, unreadable, or unusable should get the answer they
 * would have got before direct routes existed, not an error about configuration they never
 * knowingly did.
 */
@FunctionalInterface
public interface ProviderRoutes {

    /**
     * Where a request for {@code storedModelName} goes.
     *
     * @param storedModelName the id as stored — {@code {platform}/{model}}, or an unqualified
     *                        id the player typed
     */
    CompletableFuture<ProviderRoute> route(String storedModelName);

    /**
     * Where a request for {@code choice} goes: to the provider it names, and nowhere else.
     *
     * <p>Unlike {@link #route(String)} this does not fall back to the gateway. The player chose
     * the provider, and a request that quietly went somewhere else would be the one thing the
     * choice exists to prevent. A provider with no known address fails with a
     * {@link ModelException.Kind#REQUEST} that names it.
     *
     * <p>By default only the providers this release ships are known; an implementation that
     * knows the player's custom providers overrides this.
     */
    default CompletableFuture<ProviderRoute> route(ModelChoice choice) {
        java.util.Objects.requireNonNull(choice, "choice");
        return ModelProvider.builtIns().stream()
                .filter(provider -> provider.id().equals(choice.provider()))
                .findFirst()
                .map(provider -> CompletableFuture.completedFuture(ProviderRoute.to(provider, choice.model())))
                .orElseGet(() -> CompletableFuture.failedFuture(unknownProvider(choice.provider())));
    }

    /** The failure for a provider with no address: it names the provider and where to add it. */
    static ModelException unknownProvider(String provider) {
        return new ModelException(ModelException.Kind.REQUEST,
                "No address is known for the provider " + provider + ": add it as a custom provider");
    }

    /** The routing every request had before direct routes existed: everything through OpenRouter. */
    static ProviderRoutes throughOpenRouter() {
        return model -> CompletableFuture.completedFuture(ProviderRoute.throughOpenRouter(model));
    }
}
