package io.github.mcagentsmods.core.api.llm;

/**
 * The language model services the core can talk to.
 *
 * <p>Three of the four — OpenRouter, OpenAI, and DeepSeek — speak the OpenAI
 * chat completions dialect, so they differ only in base URL and model naming.
 * Anthropic speaks its own Messages API, which is why
 * {@link #usesOpenAiDialect()} exists: it is the one branch a client has to
 * take, and stating it here keeps that decision out of every call site.</p>
 *
 * <p>Each constant carries the default base URL of its service. A deployment
 * that fronts a vendor with a proxy or a self hosted gateway overrides it
 * through {@link LlmCredentials}.</p>
 */
public enum LlmVendor {

    /**
     * OpenRouter — a broker that fronts many providers behind one key and one
     * OpenAI compatible endpoint. Its model identifiers are namespaced by
     * provider, for example {@code "anthropic/claude-opus-4"}.
     */
    OPENROUTER("openrouter", "https://openrouter.ai/api/v1", true),

    /**
     * OpenAI — the reference implementation of the chat completions dialect.
     */
    OPENAI("openai", "https://api.openai.com/v1", true),

    /**
     * DeepSeek — OpenAI compatible, with its own model identifiers such as
     * {@code "deepseek-chat"}.
     */
    DEEPSEEK("deepseek", "https://api.deepseek.com/v1", true),

    /**
     * Anthropic — the Claude Messages API. Framing instructions travel as a
     * top level field rather than a message, a token bound is mandatory, and
     * the key is sent in {@code x-api-key} instead of an
     * {@code Authorization} header.
     */
    ANTHROPIC("anthropic", "https://api.anthropic.com/v1", false);

    /**
     * The stable identifier this vendor is named by in configuration files.
     * Fixed per constant so renaming a constant cannot invalidate a
     * deployment's config.
     */
    private final String code;

    /**
     * The service's public base URL, used when
     * {@link LlmCredentials} supplies no override. Carries no trailing slash.
     */
    private final String defaultBaseUrl;

    /**
     * Whether this vendor speaks the OpenAI chat completions dialect.
     */
    private final boolean openAiDialect;

    /**
     * Binds a vendor to its configuration name, endpoint, and wire dialect.
     *
     * @param code The configuration identifier.
     * @param defaultBaseUrl The public base URL, without a trailing slash.
     * @param openAiDialect Whether the service speaks the OpenAI dialect.
     */
    LlmVendor(String code, String defaultBaseUrl, boolean openAiDialect) {
        this.code = code;
        this.defaultBaseUrl = defaultBaseUrl;
        this.openAiDialect = openAiDialect;
    }

    /**
     * Returns the stable identifier this vendor is named by in configuration.
     *
     * @return The configuration identifier, for example {@code "openrouter"}.
     */
    public String code() {
        return code;
    }

    /**
     * Returns the service's public base URL.
     *
     * @return The base URL, without a trailing slash.
     */
    public String defaultBaseUrl() {
        return defaultBaseUrl;
    }

    /**
     * Reports whether this vendor speaks the OpenAI chat completions dialect.
     *
     * <p>{@code true} for OpenRouter, OpenAI, and DeepSeek; {@code false} for
     * Anthropic, whose Messages API differs in how it carries framing
     * instructions, the token bound, and authentication.</p>
     *
     * @return {@code true} when the OpenAI request and response shapes apply.
     */
    public boolean usesOpenAiDialect() {
        return openAiDialect;
    }

    /**
     * Resolves a vendor from its configuration identifier.
     *
     * <p>Matching ignores case and surrounding whitespace, so a value typed
     * into a config file resolves regardless of how it was capitalized.</p>
     *
     * @param code The configuration identifier to resolve.
     * @return The matching vendor.
     * @throws IllegalArgumentException When {@code code} is {@code null} or
     *                                  names no vendor.
     */
    public static LlmVendor fromCode(String code) {
        if (code != null) {
            String normalized = code.trim();
            for (LlmVendor vendor : values()) {
                if (vendor.code.equalsIgnoreCase(normalized)) {
                    return vendor;
                }
            }
        }
        throw new IllegalArgumentException("Unknown vendor: " + code);
    }
}
