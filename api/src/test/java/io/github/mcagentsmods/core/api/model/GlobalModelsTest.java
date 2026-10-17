package io.github.mcagentsmods.core.api.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The global model: the fallback rule, the providers a request can go to, and its store. */
class GlobalModelsTest {

    private static final ModelProvider HOMELAB = new ModelProvider("homelab", "http://127.0.0.1:8080/v1");

    @Test
    void aTaskWithNoModelOfItsOwnGetsTheGlobalOne() {
        GlobalModels global = GlobalModels.DEFAULTS;
        ModelChoice own = new ModelChoice("openai", "openai/gpt-4o-mini");

        assertSame(global.chat(), global.chatOr(null));
        assertSame(own, global.chatOr(own));
        assertSame(global.embedding(), global.embeddingOr(null));
        assertEquals("openrouter", global.chat().provider());
        assertEquals(GlobalModels.DEFAULT_CHAT_MODEL, global.chat().model());
    }

    @Test
    void offersTheGatewayTheBuiltInsAndThenTheCustomProviders() {
        GlobalModels global = GlobalModels.DEFAULTS.withProvider(HOMELAB);
        List<String> ids = global.providerIds();

        assertEquals("openrouter", ids.get(0));
        assertTrue(ids.contains("deepseek"));
        assertEquals("homelab", ids.get(ids.size() - 1));
        assertTrue(global.isCustom("HomeLab"));
        assertFalse(global.isCustom("deepseek"));
    }

    @Test
    void aCustomProviderNamedLikeABuiltInReplacesItsAddressInPlace() {
        ModelProvider regional = new ModelProvider("deepseek", "https://eu.deepseek.example/v1");
        GlobalModels global = GlobalModels.DEFAULTS.withProvider(regional);

        assertEquals("https://eu.deepseek.example/v1", global.provider("deepseek").orElseThrow().baseUrl());
        assertEquals(GlobalModels.DEFAULTS.providerIds(), global.providerIds());
    }

    @Test
    void neverTakesACustomAddressForOpenRouter() {
        GlobalModels global = new GlobalModels(GlobalModels.DEFAULTS.chat(), GlobalModels.DEFAULTS.embedding(),
                List.of(new ModelProvider("openrouter", "https://evil.example/v1")));

        assertTrue(global.customProviders().isEmpty());
        assertEquals(ModelPlatform.OPENROUTER_BASE_URL, global.provider("openrouter").orElseThrow().baseUrl());
    }

    @Test
    void addingAProviderTwiceKeepsOneWithTheNewerAddress() {
        GlobalModels global = GlobalModels.DEFAULTS
                .withProvider(HOMELAB)
                .withProvider(new ModelProvider("homelab", "http://localhost:9090/v1"));

        assertEquals(1, global.customProviders().size());
        assertEquals("http://localhost:9090/v1", global.provider("homelab").orElseThrow().baseUrl());
    }

    @Test
    void removingAProviderMovesTheModelsThatUsedItToOpenRouter() {
        GlobalModels global = GlobalModels.DEFAULTS.withProvider(HOMELAB)
                .withChat(ModelChoice.qualified("homelab", "llama3"))
                .withoutProvider("homelab");

        assertTrue(global.customProviders().isEmpty());
        assertEquals("openrouter", global.chat().provider());
        assertEquals("homelab/llama3", global.chat().model());
    }

    @Test
    void removingAnOverrideOfABuiltInKeepsTheChoiceOnThatPlatform() {
        GlobalModels global = GlobalModels.DEFAULTS
                .withProvider(new ModelProvider("deepseek", "https://eu.deepseek.example/v1"))
                .withChat(new ModelChoice("deepseek", "deepseek/deepseek-v4-flash"))
                .withoutProvider("deepseek");

        assertEquals("deepseek", global.chat().provider());
        assertEquals("https://api.deepseek.com/v1", global.provider("deepseek").orElseThrow().baseUrl());
    }

    @Test
    void routesAChoiceToTheProviderItNamesCustomOnesIncluded() {
        GlobalModelStore store = GlobalModelStore.inMemory(GlobalModels.DEFAULTS.withProvider(HOMELAB));
        ProviderRoutes routes = store.routes();

        ProviderRoute home = routes.route(ModelChoice.qualified("homelab", "llama3")).join();
        assertEquals("http://127.0.0.1:8080/v1/chat/completions", home.chatUrl());
        assertEquals("llama3", home.modelId());
        assertTrue(routes.route(new ModelChoice("nowhere", "nowhere/x")).isCompletedExceptionally());
        assertTrue(routes.route("deepseek/deepseek-v4-flash").join().isThroughOpenRouter());
    }

    @Test
    void anInMemoryStoreSaysWhetherItWasEverSaved() {
        GlobalModelStore store = GlobalModelStore.inMemory(GlobalModels.DEFAULTS);
        assertFalse(store.saved());

        GlobalModels next = GlobalModels.DEFAULTS.withChat(new ModelChoice("deepseek", "deepseek/deepseek-v4-flash"));
        store.save(next).join();

        assertTrue(store.saved());
        assertSame(next, store.current());
    }
}
