package io.github.mcagentsmods.core.api.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * An API a model request can be sent to: an id and the address its OpenAI-compatible API lives
 * at.
 *
 * <p>Three kinds exist, and this record holds all of them: {@link ModelPlatform#OPENROUTER the
 * gateway}, the platforms this release ships an address for ({@link #builtIns()}), and the
 * custom providers a player adds. A custom provider named like a built-in one replaces that
 * built-in's address, which is how a regional or self-hosted endpoint is reached.
 *
 * <p>The id is what an API key is stored under and what a {@link ModelChoice} names, so it
 * follows the platform half of {@link ModelName}: one lowercase segment.
 *
 * @param id      one lowercase segment of letters, digits, dots, dashes or underscores
 * @param baseUrl the API root, https or loopback, with no trailing slash
 */
public record ModelProvider(String id, String baseUrl) {

    private static final Pattern ID = Pattern.compile("[a-z0-9._-]{1,32}");

    public ModelProvider {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(baseUrl, "baseUrl");
        id = ModelPlatform.normalise(id);
        if (!isValidId(id)) {
            throw new IllegalArgumentException("A provider id is 1 to 32 lowercase letters, digits,"
                    + " dots, dashes or underscores, not '" + id + "'");
        }
        if (!ModelPlatform.isUsableBaseUrl(baseUrl)) {
            throw new IllegalArgumentException(
                    "A provider needs an https address, or a loopback one");
        }
        String root = baseUrl.trim();
        baseUrl = root.endsWith("/") ? root.substring(0, root.length() - 1) : root;
    }

    /** Whether {@code id}, once trimmed and lowercased, is a usable provider id. */
    public static boolean isValidId(String id) {
        return id != null && ID.matcher(id.trim().toLowerCase(java.util.Locale.ROOT)).matches();
    }

    /** The gateway, at its fixed address. */
    public static ModelProvider openRouter() {
        return new ModelProvider(ModelPlatform.OPENROUTER, ModelPlatform.OPENROUTER_BASE_URL);
    }

    /** Every provider this release can call without the player typing an address, gateway first. */
    public static List<ModelProvider> builtIns() {
        List<ModelProvider> providers = new ArrayList<>();
        providers.add(openRouter());
        for (Map.Entry<String, String> known : ModelPlatform.knownDirectBaseUrls().entrySet()) {
            providers.add(new ModelProvider(known.getKey(), known.getValue()));
        }
        return List.copyOf(providers);
    }

    /** Whether this is the gateway. */
    public boolean isOpenRouter() {
        return ModelPlatform.isOpenRouter(id);
    }

    /** Whether this release ships this exact provider, id and address alike. */
    public boolean isBuiltIn() {
        return builtIns().contains(this);
    }
}
