package io.github.mcagentsmods.core.api.llm;

import io.github.mcagentsmods.core.api.chat.ChatRequest;
import io.github.mcagentsmods.core.api.chat.ChatResponse;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The conversation with one vendor.
 *
 * <p>A client is bound to a single {@link LlmVendor} and one set of
 * {@link LlmCredentials} for its whole life. It holds no conversation state: a
 * {@link ChatRequest} carries everything the vendor needs, and nothing about it
 * is remembered afterwards.</p>
 *
 * <h2>Threading</h2>
 *
 * <p>Every method returns immediately with a {@link CompletableFuture} and
 * performs its work off the calling thread. That is not a convenience — a
 * Minecraft tick or the client's render thread must never block on a network
 * call. Implementations are safe to call from any thread.</p>
 *
 * <p>Nothing guarantees which thread a returned future completes on, so a
 * callback that touches game state must hop back onto the right scheduler
 * itself.</p>
 *
 * <h2>Failure</h2>
 *
 * <p>Failures arrive as a failed future, never as a thrown exception, and the
 * cause is always an
 * {@link io.github.mcagentsmods.core.api.AgentException}. Argument validation is
 * the exception to that rule: a {@code null} or malformed argument throws
 * immediately, because it is a programming error rather than a remote
 * failure.</p>
 */
public interface LlmClient extends AutoCloseable {

    /**
     * Returns the vendor this client talks to.
     *
     * @return The bound vendor, never {@code null}.
     */
    LlmVendor vendor();

    /**
     * Sends one exchange and returns the model's reply.
     *
     * @param request What to ask, including the model and the full message
     *                list.
     * @return A CompletableFuture containing the reply, failing with an
     *         {@link io.github.mcagentsmods.core.api.AgentException} when the
     *         vendor rejects the request or cannot be reached.
     * @throws NullPointerException When {@code request} is {@code null}.
     */
    CompletableFuture<ChatResponse> chat(ChatRequest request);

    /**
     * Lists the models this vendor currently offers.
     *
     * <p>The result is whatever the vendor reports at that moment and is not
     * cached, so a caller that needs it repeatedly should hold onto it.</p>
     *
     * @return A CompletableFuture containing the available models, failing with
     *         an {@link io.github.mcagentsmods.core.api.AgentException} when the
     *         vendor cannot be reached or does not offer a catalog.
     */
    CompletableFuture<List<ModelInfo>> listModels();

    /**
     * Checks that the vendor is reachable and the credentials are accepted.
     *
     * <p>Intended for a startup or diagnostic check. It resolves to
     * {@code false} rather than failing, so a caller can report a bad key
     * without unwrapping an exception.</p>
     *
     * @return A CompletableFuture containing {@code true} when the vendor
     *         answered and accepted the credentials.
     */
    CompletableFuture<Boolean> ping();

    /**
     * Releases whatever this client holds.
     *
     * <p>Idempotent: closing an already closed client does nothing. Calls made
     * after closing fail with an
     * {@link io.github.mcagentsmods.core.api.AgentException} rather than throwing.
     * Declared without a checked exception so it can be used in a
     * try-with-resources block without forcing a catch.</p>
     */
    @Override
    void close();
}
