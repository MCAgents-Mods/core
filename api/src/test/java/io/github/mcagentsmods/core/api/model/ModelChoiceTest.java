package io.github.mcagentsmods.core.api.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The provider a model is called through, what a direct call names, and the fallback rule. */
class ModelChoiceTest {

    @Test
    void lowercasesBothHalvesAndTakesBlankAsTheGateway() {
        ModelChoice choice = new ModelChoice("  ", " DeepSeek/DeepSeek-V4-Flash ");
        assertEquals("openrouter", choice.provider());
        assertEquals("deepseek/deepseek-v4-flash", choice.model());
        assertTrue(choice.isThroughOpenRouter());
        assertThrows(IllegalArgumentException.class, () -> new ModelChoice("openai", " "));
    }

    @Test
    void addsTheProviderToAModelTypedWithoutItsPlatform() {
        assertEquals("homelab/llama3", ModelChoice.qualified("HomeLab", "llama3").model());
        assertEquals("openai/gpt-4o-mini", ModelChoice.qualified("together", "openai/gpt-4o-mini").model());
        // The gateway serves no model of its own, so nothing is added.
        assertEquals("gpt-4o-mini", ModelChoice.qualified("openrouter", "gpt-4o-mini").model());
    }

    @Test
    void namesTheModelTheWayTheChosenProviderDoes() {
        ModelProvider deepseek = new ModelProvider("deepseek", "https://api.deepseek.com/v1");
        assertEquals("deepseek-v4-flash",
                ProviderRoute.to(deepseek, "deepseek/deepseek-v4-flash").modelId());

        ModelProvider together = new ModelProvider("together", "https://api.together.xyz/v1/");
        ProviderRoute hosted = ProviderRoute.to(together, "meta-llama/llama-3-70b");
        assertEquals("meta-llama/llama-3-70b", hosted.modelId());
        assertEquals("https://api.together.xyz/v1/chat/completions", hosted.chatUrl());

        ProviderRoute gateway = ProviderRoute.to(ModelProvider.openRouter(), "deepseek/deepseek-v4-flash");
        assertTrue(gateway.isThroughOpenRouter());
        assertEquals("deepseek/deepseek-v4-flash", gateway.modelId());
    }

    @Test
    void takesAProviderOnlyWithASafeAddressAndAOneSegmentId() {
        assertEquals("homelab", new ModelProvider(" HomeLab ", "http://127.0.0.1:8080/v1").id());
        assertThrows(IllegalArgumentException.class,
                () -> new ModelProvider("homelab", "http://example.com/v1"));
        assertThrows(IllegalArgumentException.class,
                () -> new ModelProvider("home/lab", "https://example.com/v1"));
        assertFalse(ModelProvider.isValidId(""));
    }

    @Test
    void listsTheGatewayFirstAmongTheBuiltIns() {
        List<ModelProvider> builtIns = ModelProvider.builtIns();
        assertTrue(builtIns.get(0).isOpenRouter());
        assertTrue(builtIns.stream().anyMatch(provider -> provider.id().equals("deepseek")));
        assertTrue(builtIns.get(1).isBuiltIn());
        assertFalse(new ModelProvider("deepseek", "https://eu.deepseek.example/v1").isBuiltIn());
    }

    @Test
    void fallsBackOnlyWhenTheTaskSetNothing() {
        ModelChoice global = ModelChoice.throughOpenRouter("deepseek/deepseek-v4-flash");
        ModelChoice own = new ModelChoice("openai", "openai/gpt-4o-mini");
        assertSame(own, ModelChoice.orElse(own, global));
        assertSame(global, ModelChoice.orElse(null, global));
    }

    @Test
    void routesAChoiceToABuiltInOrRefusesIt() {
        ProviderRoutes routes = ProviderRoutes.throughOpenRouter();
        assertEquals("https://api.deepseek.com/v1",
                routes.route(new ModelChoice("deepseek", "deepseek/deepseek-v4-flash")).join().baseUrl());
        assertTrue(routes.route(new ModelChoice("nowhere", "nowhere/model")).isCompletedExceptionally());
    }
}
