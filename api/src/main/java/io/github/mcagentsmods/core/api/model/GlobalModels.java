package io.github.mcagentsmods.core.api.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * The global model: what every task without a model of its own is answered and embedded with,
 * and the custom providers the player added.
 *
 * <p>Set on the Models page and read by every MCAgents mod through
 * {@code core.common.model.Models}. A task that lets the player pick its own model keeps that
 * choice, or {@code null} for none, and resolves it with {@link #chatOr(ModelChoice)} or
 * {@link #embeddingOr(ModelChoice)}: its own choice when it has one, this one when it does not.
 * Reading the global model per request, rather than copying it, is what lets a change on the
 * Models page reach every such task at once.
 *
 * @param chat            what questions are answered with
 * @param embedding       what text is embedded with, for retrieval
 * @param customProviders the providers the player added, one per id, in the order added; one
 *                        whose id is a built-in platform's replaces that platform's address
 */
public record GlobalModels(ModelChoice chat, ModelChoice embedding, List<ModelProvider> customProviders) {

    /** The chat model a player who never chose gets. */
    public static final String DEFAULT_CHAT_MODEL = "deepseek/deepseek-v4-flash";

    /** The embedding model a player who never chose gets. */
    public static final String DEFAULT_EMBEDDING_MODEL = "openai/text-embedding-3-small";

    /** Both through OpenRouter, the one key a new player is asked for, and no custom providers. */
    public static final GlobalModels DEFAULTS = new GlobalModels(
            ModelChoice.throughOpenRouter(DEFAULT_CHAT_MODEL),
            ModelChoice.throughOpenRouter(DEFAULT_EMBEDDING_MODEL),
            List.of());

    public GlobalModels {
        Objects.requireNonNull(chat, "chat");
        Objects.requireNonNull(embedding, "embedding");
        Objects.requireNonNull(customProviders, "customProviders");
        // One per id, the last one given winning, in the order first given.
        Map<String, ModelProvider> byId = new LinkedHashMap<>();
        for (ModelProvider provider : customProviders) {
            Objects.requireNonNull(provider, "provider");
            if (provider.isOpenRouter()) {
                // OpenRouter's address is fixed: a key for it must never go anywhere else.
                continue;
            }
            byId.put(provider.id(), provider);
        }
        customProviders = List.copyOf(byId.values());
    }

    /** {@code own} when the task set one, else the global chat model. */
    public ModelChoice chatOr(ModelChoice own) {
        return ModelChoice.orElse(own, chat);
    }

    /** {@code own} when the task set one, else the global embedding model. */
    public ModelChoice embeddingOr(ModelChoice own) {
        return ModelChoice.orElse(own, embedding);
    }

    /**
     * Every provider a request can go to: OpenRouter, the built-in platforms (a custom provider
     * of the same id in its place), then the other custom providers in the order added.
     */
    public List<ModelProvider> providers() {
        Map<String, ModelProvider> byId = new LinkedHashMap<>();
        for (ModelProvider builtIn : ModelProvider.builtIns()) {
            byId.put(builtIn.id(), builtIn);
        }
        for (ModelProvider custom : customProviders) {
            byId.put(custom.id(), custom);
        }
        return List.copyOf(byId.values());
    }

    /** The ids of {@link #providers()}, in the same order. */
    public List<String> providerIds() {
        return providers().stream().map(ModelProvider::id).toList();
    }

    /** The provider called {@code id}, if a request can go there. */
    public Optional<ModelProvider> provider(String id) {
        String wanted = ModelPlatform.normalise(id);
        return providers().stream().filter(provider -> provider.id().equals(wanted)).findFirst();
    }

    /** Whether {@code id} is one of the player's custom providers. */
    public boolean isCustom(String id) {
        String wanted = ModelPlatform.normalise(id);
        return customProviders.stream().anyMatch(provider -> provider.id().equals(wanted));
    }

    /** These settings with another chat model. */
    public GlobalModels withChat(ModelChoice chat) {
        return new GlobalModels(chat, embedding, customProviders);
    }

    /** These settings with another embedding model. */
    public GlobalModels withEmbedding(ModelChoice embedding) {
        return new GlobalModels(chat, embedding, customProviders);
    }

    /** These settings with {@code provider} added, or replacing the custom provider of its id. */
    public GlobalModels withProvider(ModelProvider provider) {
        List<ModelProvider> next = new ArrayList<>(customProviders);
        next.removeIf(existing -> existing.id().equals(provider.id()));
        next.add(provider);
        return new GlobalModels(chat, embedding, next);
    }

    /**
     * These settings without the custom provider {@code id}.
     *
     * <p>A model choice that named it is moved to OpenRouter rather than left pointing at a
     * provider with no address, unless the id is a built-in platform's, which still has its own.
     */
    public GlobalModels withoutProvider(String id) {
        String gone = ModelPlatform.normalise(id);
        List<ModelProvider> next = new ArrayList<>(customProviders);
        next.removeIf(existing -> existing.id().equals(gone));
        boolean builtIn = ModelProvider.builtIns().stream().anyMatch(p -> p.id().equals(gone));
        ModelChoice nextChat = !builtIn && chat.provider().equals(gone)
                ? chat.withProvider(ModelPlatform.OPENROUTER) : chat;
        ModelChoice nextEmbedding = !builtIn && embedding.provider().equals(gone)
                ? embedding.withProvider(ModelPlatform.OPENROUTER) : embedding;
        return new GlobalModels(nextChat, nextEmbedding, next);
    }

    /**
     * Routing over these settings: a chosen provider is looked up among {@link #providers()};
     * a request that chose none goes through OpenRouter, as before providers were chosen.
     */
    public static ProviderRoutes routes(Supplier<GlobalModels> global) {
        Objects.requireNonNull(global, "global");
        return new ProviderRoutes() {
            @Override
            public CompletableFuture<ProviderRoute> route(String storedModelName) {
                return CompletableFuture.completedFuture(
                        ProviderRoute.throughOpenRouter(storedModelName));
            }

            @Override
            public CompletableFuture<ProviderRoute> route(ModelChoice choice) {
                Objects.requireNonNull(choice, "choice");
                return global.get().provider(choice.provider())
                        .map(provider -> CompletableFuture.completedFuture(
                                ProviderRoute.to(provider, choice.model())))
                        .orElseGet(() -> CompletableFuture.failedFuture(
                                ProviderRoutes.unknownProvider(choice.provider())));
            }
        };
    }
}
