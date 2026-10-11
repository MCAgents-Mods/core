package io.github.mcagentsmods.core.common;

import io.github.mcagentsmods.core.api.llm.LlmClient;
import io.github.mcagentsmods.core.api.llm.LlmCredentials;

/**
 * The only place in the project that names a concrete client.
 *
 * <p>Keeping construction here is what lets every other class work through
 * {@link LlmClient} alone. Adding a vendor means adding a branch in one method
 * rather than touching the provider, the registry, and every call site.</p>
 *
 * <p>Package-private on purpose: outside {@code common}, clients are neither
 * constructed nor held.</p>
 */
final class LlmClientFactory {

    /**
     * Not instantiable — this class is a single static factory.
     */
    private LlmClientFactory() {
    }

    /**
     * Builds the client that serves a vendor.
     *
     * <p>The choice is made by
     * {@link io.github.mcagentsmods.core.api.llm.LlmVendor#usesOpenAiDialect()}
     * rather than by switching over the constants, so a vendor added to the
     * enum that speaks the OpenAI dialect is served correctly without a change
     * here.</p>
     *
     * @param credentials The key, endpoint, and timeout to reach the vendor
     *                    with.
     * @return A client bound to those credentials. No connection is opened.
     */
    static LlmClient create(LlmCredentials credentials) {
        return credentials.vendor().usesOpenAiDialect()
                ? new OpenAiCompatibleClient(credentials)
                : new AnthropicClient(credentials);
    }
}
