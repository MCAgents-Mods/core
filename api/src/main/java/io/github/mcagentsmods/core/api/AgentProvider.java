package io.github.mcagentsmods.core.api;

import io.github.mcagentsmods.core.api.chat.ChatRequest;
import io.github.mcagentsmods.core.api.chat.ChatResponse;
import io.github.mcagentsmods.core.api.llm.LlmCredentials;
import io.github.mcagentsmods.core.api.llm.LlmVendor;
import io.github.mcagentsmods.core.api.llm.ModelInfo;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Everything the core can do, in one contract.
 *
 * <p>A provider holds one client per registered {@link LlmVendor} and routes
 * each call to the right one, so a consumer talks to OpenRouter, OpenAI,
 * DeepSeek, and Anthropic through a single object rather than juggling four.
 * The implementation is
 * {@code io.github.mcagentsmods.core.common.MCAgentsProvider}, which is the only
 * class a consumer needs to read.</p>
 *
 * <h2>No memory</h2>
 *
 * <p>A provider stores no conversation, no cache, and no per player state. The
 * only thing it holds is the set of registered vendors. Anything resembling
 * history is the caller's to keep and to replay — see
 * {@link ChatResponse#asMessage()}.</p>
 *
 * <h2>Threading</h2>
 *
 * <p>Every remote call returns a {@link CompletableFuture} and does its work off
 * the calling thread, so none of them may be assumed to complete on the thread
 * that started them. Registration and lookup are safe to call from any
 * thread.</p>
 */
public interface AgentProvider extends AutoCloseable {

    /**
     * Registers a vendor, building the client that will serve it.
     *
     * <p>Registering a vendor that is already registered replaces it, and the
     * client it replaces is closed. That is how a key or endpoint is rotated
     * without restarting the server.</p>
     *
     * <p>No network call is made here — a bad key is only discovered on the
     * first real request, or on {@link #ping(LlmVendor)}.</p>
     *
     * @param credentials The key, endpoint, and timeout to reach the vendor
     *                    with. The vendor itself comes from
     *                    {@link LlmCredentials#vendor()}.
     * @throws NullPointerException When {@code credentials} is {@code null}.
     */
    void register(LlmCredentials credentials);

    /**
     * Removes a vendor and closes its client.
     *
     * @param vendor The vendor to remove.
     * @return {@code true} when a client was registered and has now been
     *         closed, {@code false} when nothing was registered.
     */
    boolean unregister(LlmVendor vendor);

    /**
     * Reports whether a vendor can currently be called.
     *
     * @param vendor The vendor to check.
     * @return {@code true} when a client is registered for it.
     */
    boolean isRegistered(LlmVendor vendor);

    /**
     * Lists the vendors currently registered.
     *
     * @return An unmodifiable snapshot, empty when nothing is registered. Later
     *         registrations do not appear in an already returned set.
     */
    Set<LlmVendor> registeredVendors();

    /**
     * Sends one exchange to a vendor and returns the model's reply.
     *
     * @param vendor The vendor to ask.
     * @param request What to ask, including the model and the full message
     *                list.
     * @return A CompletableFuture containing the reply, failing with an
     *         {@link AgentException} when the vendor is not registered, rejects
     *         the request, or cannot be reached.
     * @throws NullPointerException When either argument is {@code null}.
     */
    CompletableFuture<ChatResponse> chat(LlmVendor vendor, ChatRequest request);

    /**
     * Sends a single prompt and returns just the reply text.
     *
     * <p>The shorthand for the common case. Equivalent to
     * {@link #chat(LlmVendor, ChatRequest)} with a one message request, keeping
     * only {@link ChatResponse#content()}.</p>
     *
     * @param vendor The vendor to ask.
     * @param model The model identifier to send to.
     * @param prompt The single user turn.
     * @return A CompletableFuture containing the reply text, failing with an
     *         {@link AgentException} on the same conditions as
     *         {@link #chat(LlmVendor, ChatRequest)}.
     */
    CompletableFuture<String> ask(LlmVendor vendor, String model, String prompt);

    /**
     * Lists the models a vendor currently offers.
     *
     * @param vendor The vendor to query.
     * @return A CompletableFuture containing the available models, failing with
     *         an {@link AgentException} when the vendor is not registered or
     *         cannot be reached.
     */
    CompletableFuture<List<ModelInfo>> listModels(LlmVendor vendor);

    /**
     * Checks that a vendor is reachable and its credentials are accepted.
     *
     * <p>Resolves to {@code false} rather than failing, so a startup check can
     * report a bad key without unwrapping an exception. An unregistered vendor
     * also resolves to {@code false}.</p>
     *
     * @param vendor The vendor to check.
     * @return A CompletableFuture containing {@code true} when the vendor
     *         answered and accepted the credentials.
     */
    CompletableFuture<Boolean> ping(LlmVendor vendor);

    /**
     * Registers OpenRouter with its default endpoint.
     *
     * @param apiKey The OpenRouter API key.
     */
    default void registerOpenRouter(String apiKey) {
        register(LlmCredentials.of(LlmVendor.OPENROUTER, apiKey));
    }

    /**
     * Registers OpenAI with its default endpoint.
     *
     * @param apiKey The OpenAI API key.
     */
    default void registerOpenAi(String apiKey) {
        register(LlmCredentials.of(LlmVendor.OPENAI, apiKey));
    }

    /**
     * Registers DeepSeek with its default endpoint.
     *
     * @param apiKey The DeepSeek API key.
     */
    default void registerDeepSeek(String apiKey) {
        register(LlmCredentials.of(LlmVendor.DEEPSEEK, apiKey));
    }

    /**
     * Registers Anthropic with its default endpoint.
     *
     * @param apiKey The Anthropic API key.
     */
    default void registerAnthropic(String apiKey) {
        register(LlmCredentials.of(LlmVendor.ANTHROPIC, apiKey));
    }

    /**
     * Closes every registered client and clears the registry.
     *
     * <p>Idempotent, and the natural thing to call when a mod is
     * disabled. A provider is not reusable afterwards: calls made after closing
     * fail with an {@link AgentException}.</p>
     */
    @Override
    void close();
}
