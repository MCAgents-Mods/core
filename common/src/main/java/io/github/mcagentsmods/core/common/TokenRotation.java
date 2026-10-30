package io.github.mcagentsmods.core.common;

import io.github.mcagentsmods.core.api.AgentException;
import io.github.mcagentsmods.core.api.chat.ChatRequest;
import io.github.mcagentsmods.core.api.chat.ChatResponse;
import io.github.mcagentsmods.core.api.llm.AbstractLlmClient;
import io.github.mcagentsmods.core.api.llm.LlmClient;
import io.github.mcagentsmods.core.api.llm.LlmVendor;
import io.github.mcagentsmods.core.api.token.TokenState;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Sends chat requests for {@link MCAgentsProvider}, rotating through a vendor's
 * credentials when the one in use fails.
 *
 * <p>The provider decides which vendors exist and which client serves each;
 * this class decides what happens when a request fails. A rejected credential
 * is dropped from its pool and the store, a rate limited one is moved past but
 * kept, and any other failure goes straight back to the caller. A vendor
 * registered with a single credential has no pool, so nothing is rotated.</p>
 */
final class TokenRotation {

    /**
     * Which vendors are configured, and the client serving each.
     */
    private final ClientRegistry clients;

    /**
     * The credential pool for each vendor that has one.
     */
    private final TokenPools tokens;

    /**
     * Binds the rotation to the provider's clients and pools.
     *
     * @param clients The provider's client registry.
     * @param tokens The provider's credential pools.
     */
    TokenRotation(ClientRegistry clients, TokenPools tokens) {
        this.clients = Objects.requireNonNull(clients, "clients cannot be null");
        this.tokens = Objects.requireNonNull(tokens, "tokens cannot be null");
    }

    /**
     * Sends a request to a vendor, rotating through its credentials when it has
     * a pool.
     *
     * @param vendor The vendor to ask.
     * @param request What to ask.
     * @return A CompletableFuture containing the reply, or failed with an
     *         {@link AgentException}.
     * @throws NullPointerException When either argument is {@code null}.
     */
    CompletableFuture<ChatResponse> chat(LlmVendor vendor, ChatRequest request) {
        Objects.requireNonNull(vendor, "vendor cannot be null");
        Objects.requireNonNull(request, "request cannot be null");

        Optional<TokenPool> pool = tokens.find(vendor);
        if (pool.isEmpty()) {
            // Registered with a single credential. Nothing to rotate to, so the
            // failure goes straight back to the caller.
            try {
                return clients.require(vendor).chat(request);
            } catch (AgentException e) {
                return CompletableFuture.failedFuture(e);
            }
        }

        TokenPool credentials = pool.get();
        AgentException unusable = checkUsable(credentials);
        if (unusable != null) {
            return CompletableFuture.failedFuture(unusable);
        }
        return attempt(vendor, request, credentials, credentials.remaining());
    }

    /**
     * Sends a request, rotating to the next credential when the failure says the
     * current one is at fault.
     *
     * @param vendor The vendor to ask.
     * @param request What to ask.
     * @param pool The vendor's credentials.
     * @param attemptsLeft How many credentials may still be tried. Bounds the
     *                     recursion, so an exhausted pool fails after one pass
     *                     rather than looping.
     * @return A CompletableFuture containing the reply.
     */
    private CompletableFuture<ChatResponse> attempt(
            LlmVendor vendor, ChatRequest request, TokenPool pool, int attemptsLeft) {

        LlmClient client;
        CompletableFuture<ChatResponse> sent;
        try {
            client = clients.require(vendor);
            sent = client.chat(request);
        } catch (AgentException e) {
            return CompletableFuture.failedFuture(e);
        }
        // The key this request actually used. A rotation acts on it, never on
        // whatever the pool holds first by the time the failure arrives.
        String used = client instanceof AbstractLlmClient sending
                ? sending.credentials().apiKey()
                : null;

        return sent.handle((response, failure) -> {
            if (failure == null) {
                return CompletableFuture.completedFuture(response);
            }

            AgentException cause = unwrap(vendor, failure);
            Optional<String> next;
            if (cause.isAuthFailure()) {
                // The credential is dead: drop it from the pool and the store.
                next = pool.reject(used);
            } else if (cause.isRateLimited()) {
                // The credential is healthy and busy: move on, but keep it.
                next = pool.rotate(used);
            } else {
                // Nothing was learned about the credential. Do not touch it.
                next = Optional.empty();
            }

            if (next.isEmpty() || attemptsLeft <= 1) {
                return CompletableFuture.<ChatResponse>failedFuture(exhausted(pool, cause));
            }

            // The vendor was unregistered or its pool reloaded while this
            // request ran. Its rotation is no longer this request's to make.
            if (tokens.find(vendor).orElse(null) != pool) {
                return CompletableFuture.<ChatResponse>failedFuture(cause);
            }
            // Rebuilt from the vendor's stored connection settings, so a
            // rotation never silently moves a proxied deployment onto the
            // public endpoint. Installed only if no other failing request got
            // there first; either way the retry uses whichever client is current.
            if (!next.get().equals(used)) {
                clients.replace(client, tokens.credentialsFor(vendor, next.get()));
            }
            return attempt(vendor, request, pool, attemptsLeft - 1);
        }).thenCompose(future -> future);
    }

    /**
     * Fails early when a pool holds nothing usable.
     *
     * @param pool The pool to check.
     * @return The failure to report, or {@code null} when a credential is
     *         available.
     */
    private AgentException checkUsable(TokenPool pool) {
        return switch (pool.state()) {
            case READY -> null;
            case NOT_SET -> new AgentException(pool.vendor(),
                    "No token is configured for " + pool.vendor().code() + ".");
            case EXPIRED -> new AgentException(pool.vendor(),
                    "Every token configured for " + pool.vendor().code()
                            + " was rejected and removed. Add a working token and reload.");
        };
    }

    /**
     * Reports a failure that used up the last credential as an exhaustion.
     *
     * <p>"Every token was rejected" is what a server owner has to act on; the
     * last vendor message is kept as the cause for the log.</p>
     *
     * @param pool The pool that ran out.
     * @param failure The failure that ended the attempt.
     * @return The failure to hand back.
     */
    private AgentException exhausted(TokenPool pool, AgentException failure) {
        if (failure.isAuthFailure() && pool.state() == TokenState.EXPIRED) {
            return new AgentException(pool.vendor(),
                    "Every token configured for " + pool.vendor().code()
                            + " was rejected and removed. Add a working token and reload.",
                    failure.statusCode(), failure);
        }
        return failure;
    }

    /**
     * Normalizes whatever a failed future carried into an
     * {@link AgentException}.
     *
     * @param vendor The vendor the call was aimed at.
     * @param failure What the future completed with.
     * @return The failure as an {@link AgentException}.
     */
    private AgentException unwrap(LlmVendor vendor, Throwable failure) {
        Throwable cause = failure instanceof CompletionException && failure.getCause() != null
                ? failure.getCause()
                : failure;
        if (cause instanceof AgentException agentFailure) {
            return agentFailure;
        }
        return new AgentException(vendor,
                cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage(), cause);
    }
}
