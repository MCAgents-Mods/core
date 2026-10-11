package io.github.mcagentsmods.core.common;

import io.github.mcagentsmods.core.api.llm.LlmCredentials;
import io.github.mcagentsmods.core.api.llm.LlmVendor;
import io.github.mcagentsmods.core.api.token.TokenState;
import io.github.mcagentsmods.core.api.token.TokenStore;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The credential pool for each vendor a consumer supplied a store for.
 *
 * <p>A vendor is in here only when the consumer registered a
 * {@link TokenStore} for it. A vendor registered the older way — with a single
 * {@link io.github.mcagentsmods.core.api.llm.LlmCredentials} — has no pool, and
 * nothing rotates for it. Both paths stay supported on purpose: a consumer with
 * one hardcoded key should not be forced to implement a store.</p>
 *
 * <p>Package-private, like everything else behind {@link MCAgentsProvider}.</p>
 */
final class TokenPools {

    /**
     * The pool serving each vendor that has a store.
     *
     * <p>An {@link EnumMap} because the key space is a small enum, and because
     * it iterates in constant order.</p>
     */
    private final Map<LlmVendor, TokenPool> pools = new EnumMap<>(LlmVendor.class);

    /**
     * The connection settings each vendor's credentials are used with.
     *
     * <p>Everything about reaching the vendor <em>except</em> the key: the base
     * URL, the timeout, and any extra headers. Rotation swaps only the key and
     * rebuilds from this, which is what stops a deployment behind a proxy or a
     * self-hosted gateway from being silently redirected to the public endpoint
     * the first time a credential rotates.</p>
     */
    private final Map<LlmVendor, LlmCredentials> templates = new EnumMap<>(LlmVendor.class);

    /**
     * Builds a pool for a vendor, replacing any pool already serving it.
     *
     * @param vendor The vendor the store holds credentials for.
     * @param store Where those credentials live.
     * @param template The connection settings to use with every credential from
     *                 this store. Its own key is ignored — only the base URL,
     *                 timeout, and headers are kept.
     * @return The new pool, so the caller can install its first credential.
     * @throws NullPointerException When any argument is {@code null}.
     */
    synchronized TokenPool install(LlmVendor vendor, TokenStore store, LlmCredentials template) {
        Objects.requireNonNull(vendor, "vendor cannot be null");
        Objects.requireNonNull(store, "store cannot be null");
        Objects.requireNonNull(template, "template cannot be null");

        TokenPool pool = new TokenPool(vendor, store);
        pools.put(vendor, pool);
        templates.put(vendor, template);
        return pool;
    }

    /**
     * Builds credentials for a vendor from a token and that vendor's stored
     * connection settings.
     *
     * @param vendor The vendor.
     * @param token The credential to use.
     * @return Credentials carrying the token and the vendor's configured base
     *         URL, timeout, and headers.
     */
    synchronized LlmCredentials credentialsFor(LlmVendor vendor, String token) {
        LlmCredentials template = templates.get(vendor);
        if (template == null) {
            return LlmCredentials.of(vendor, token);
        }
        return new LlmCredentials(vendor, token, template.baseUrl(), template.timeout(), template.headers());
    }

    /**
     * Removes a vendor's pool.
     *
     * @param vendor The vendor to forget.
     * @return {@code true} when a pool was removed.
     */
    synchronized boolean remove(LlmVendor vendor) {
        templates.remove(vendor);
        return pools.remove(vendor) != null;
    }

    /**
     * Returns the pool serving a vendor.
     *
     * @param vendor The vendor to look up.
     * @return The pool, or empty when the vendor has no store.
     */
    synchronized Optional<TokenPool> find(LlmVendor vendor) {
        return Optional.ofNullable(pools.get(vendor));
    }

    /**
     * Reports whether a vendor can currently be called, and if not, why not.
     *
     * <p>A vendor with no pool reads as {@link TokenState#NOT_SET}: from a
     * consumer's point of view, no store means no credentials it can speak
     * about.</p>
     *
     * @param vendor The vendor to check.
     * @return The credential state.
     */
    synchronized TokenState state(LlmVendor vendor) {
        TokenPool pool = pools.get(vendor);
        return pool == null ? TokenState.NOT_SET : pool.state();
    }

    /**
     * Re-reads one vendor's credentials from its store.
     *
     * @param vendor The vendor to reload.
     * @return The credential state afterwards, or empty when the vendor has no
     *         store to reload.
     */
    synchronized Optional<TokenPool> reload(LlmVendor vendor) {
        TokenPool pool = pools.get(vendor);
        if (pool == null) {
            return Optional.empty();
        }
        pool.reload();
        return Optional.of(pool);
    }

    /**
     * Re-reads every vendor's credentials.
     *
     * @return Every pool, already reloaded, so the caller can reinstall each
     *         one's current credential.
     */
    synchronized Iterable<TokenPool> reloadAll() {
        pools.values().forEach(TokenPool::reload);
        return Map.copyOf(pools).values();
    }

    /**
     * Forgets every pool.
     */
    synchronized void clear() {
        pools.clear();
        templates.clear();
    }
}
