package io.github.mcagentsmods.core.api.model;


import java.util.Objects;

/**
 * Where one request goes, and what it calls the model when it gets there.
 *
 * <p>Two routes exist for the same model, and a stored {@link ModelName} produces either:
 *
 * <table border="1">
 *   <caption>The same model, either way round</caption>
 *   <tr><th></th><th>Sent to</th><th>{@code "model"} in the body</th><th>Key used</th></tr>
 *   <tr><td>Through OpenRouter</td><td>{@code openrouter.ai/api/v1}</td>
 *       <td>{@code openai/gpt-4o-mini}</td><td>an {@code openrouter} key</td></tr>
 *   <tr><td>Direct</td><td>{@code api.openai.com/v1}</td>
 *       <td>{@code gpt-4o-mini}</td><td>an {@code openai} key</td></tr>
 * </table>
 *
 * <p>That is the whole of the difference, and it is why the naming convention is worth
 * following: nothing stored has to change when a player moves a model from one route to the
 * other, because both routes are built from the same two halves of one stored name.
 *
 * @param platform which platform's key authenticates the request
 * @param baseUrl  the API root, with no trailing slash
 * @param modelId  what to put in the request body's {@code model} field
 */
public record ProviderRoute(String platform, String baseUrl, String modelId) {

    public ProviderRoute {
        Objects.requireNonNull(platform, "platform");
        Objects.requireNonNull(baseUrl, "baseUrl");
        Objects.requireNonNull(modelId, "modelId");
        if (platform.isBlank() || modelId.isBlank()) {
            throw new IllegalArgumentException("A route needs a platform and a model id");
        }
        if (!ModelPlatform.isUsableBaseUrl(baseUrl)) {
            throw new IllegalArgumentException(
                    "A route needs an https base URL, or a loopback one");
        }
        String root = baseUrl.trim();
        baseUrl = root.endsWith("/") ? root.substring(0, root.length() - 1) : root;
        platform = ModelPlatform.normalise(platform);
    }

    /**
     * The gateway route: the whole stored name travels as the model id, which is exactly what
     * OpenRouter expects.
     *
     * <p>This is the fallback for everything — an unqualified id, a platform with no URL, a
     * platform the player holds no key for.
     */
    public static ProviderRoute throughOpenRouter(String storedModelName) {
        Objects.requireNonNull(storedModelName, "storedModelName");
        return new ProviderRoute(ModelPlatform.OPENROUTER, ModelPlatform.OPENROUTER_BASE_URL,
                ModelName.normalise(storedModelName));
    }

    /**
     * A direct route: the platform's own API, named by the platform's own model id.
     *
     * @param name    the stored name, whose two halves become the key to use and the id to send
     * @param baseUrl where that platform's API lives
     */
    public static ProviderRoute direct(ModelName name, String baseUrl) {
        Objects.requireNonNull(name, "name");
        return new ProviderRoute(name.platform(), baseUrl, name.bare());
    }

    /**
     * The route to {@code provider} for {@code storedModelName}: the provider's own address, and
     * the name it knows the model by.
     *
     * <p>Through OpenRouter that is the whole stored name. At any other provider it is the bare
     * id when the stored name's platform is that provider ({@code deepseek/deepseek-v4-flash}
     * at {@code deepseek} is {@code deepseek-v4-flash}), and the whole stored name otherwise,
     * because a host serving other platforms' models, Together for one, names them
     * {@code {platform}/{model}} itself.
     */
    public static ProviderRoute to(ModelProvider provider, String storedModelName) {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(storedModelName, "storedModelName");
        String stored = ModelName.normalise(storedModelName);
        if (provider.isOpenRouter()) {
            return new ProviderRoute(ModelPlatform.OPENROUTER, provider.baseUrl(), stored);
        }
        String sent = ModelName.parse(stored)
                .filter(name -> name.platform().equals(provider.id()))
                .map(ModelName::bare)
                .orElse(stored);
        return new ProviderRoute(provider.id(), provider.baseUrl(), sent);
    }

    /** Whether this route goes through the gateway rather than to a platform directly. */
    public boolean isThroughOpenRouter() {
        return ModelPlatform.isOpenRouter(platform);
    }

    /** Where a chat completion is posted. */
    public String chatUrl() {
        return baseUrl + "/chat/completions";
    }

    /** Where an embedding is posted. */
    public String embeddingsUrl() {
        return baseUrl + "/embeddings";
    }
}
