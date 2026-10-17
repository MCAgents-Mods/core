package io.github.mcagentsmods.core.common.model;

import io.github.mcagentsmods.core.api.model.ChatRequest;
import io.github.mcagentsmods.core.api.model.ModelChoice;
import io.github.mcagentsmods.core.api.model.ModelException;
import io.github.mcagentsmods.core.api.model.PromptMessage;
import io.github.mcagentsmods.core.api.model.ProviderRoutes;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A request that names its provider goes to that provider, with that provider's key, and never
 * falls back to the gateway.
 */
class ChosenProviderTest {

    private static final String ANSWER = """
            {"model":"deepseek-v4-flash",
             "choices":[{"message":{"role":"assistant","content":"Hi."}}]}
            """;

    private static final String VECTOR = """
            {"data":[{"embedding":[0.5,0.25]}]}
            """;

    /** Routing by model name sends everything to the gateway, so a direct call proves the choice won. */
    private static final ProviderRoutes ROUTES = ProviderRoutes.throughOpenRouter();

    private static ChatRequest ask(String provider) {
        return new ChatRequest("deepseek/deepseek-v4-flash", List.of(PromptMessage.user("Hello")))
                .withProvider(provider);
    }

    @Test
    void sendsToTheChosenProvidersOwnAddressWithItsOwnKey() {
        var transport = TestAccess.transport().reply(200, ANSWER);
        var keys = TestAccess.keys("sk-or-gateway").forPlatform("deepseek", "sk-deepseek");

        try (var client = TestAccess.client(transport, keys, ROUTES)) {
            assertEquals("Hi.", await(client.complete(ask("deepseek"))).text());
        }

        var call = transport.lastCall();
        assertEquals("https://api.deepseek.com/v1/chat/completions", call.url());
        assertEquals("Bearer sk-deepseek", call.headers().get("Authorization"));
        assertTrue(call.body().contains("\"model\":\"deepseek-v4-flash\""));
    }

    @Test
    void sendsTheWholeNameThroughOpenRouterWhenThatIsTheChoice() {
        var transport = TestAccess.transport().reply(200, ANSWER);
        var keys = TestAccess.keys("sk-or-gateway").forPlatform("deepseek", "sk-deepseek");

        try (var client = TestAccess.client(transport, keys, ROUTES)) {
            await(client.complete(ask("openrouter")));
        }

        var call = transport.lastCall();
        assertEquals("https://openrouter.ai/api/v1/chat/completions", call.url());
        assertEquals("Bearer sk-or-gateway", call.headers().get("Authorization"));
        assertTrue(call.body().contains("\"model\":\"deepseek/deepseek-v4-flash\""));
    }

    @Test
    void failsNamingTheMissingKeyRatherThanGoingThroughTheGateway() {
        var transport = TestAccess.transport().reply(200, ANSWER);
        var keys = TestAccess.keys("sk-or-gateway");

        try (var client = TestAccess.client(transport, keys, ROUTES)) {
            ModelException error = failure(client.complete(ask("deepseek")));
            assertEquals(ModelException.Kind.NO_KEY, error.kind());
            assertTrue(error.getMessage().contains("deepseek"));
        }
        assertTrue(transport.calls().isEmpty(), "nothing may leave without the provider's own key");
    }

    @Test
    void failsForAProviderWithNoKnownAddress() {
        var transport = TestAccess.transport().reply(200, ANSWER);
        var keys = TestAccess.keys().forPlatform("homelab", "sk-home");

        try (var client = TestAccess.client(transport, keys, ROUTES)) {
            ModelException error = failure(client.complete(ask("homelab")));
            assertEquals(ModelException.Kind.REQUEST, error.kind());
            assertTrue(error.getMessage().contains("homelab"));
        }
        assertTrue(transport.calls().isEmpty());
    }

    @Test
    void embedsAtTheChosenProvider() {
        var transport = TestAccess.transport().reply(200, VECTOR);
        var keys = TestAccess.keys("sk-or-gateway").forPlatform("openai", "sk-openai");

        try (var client = TestAccess.client(transport, keys, ROUTES)) {
            float[] vector = await(client.embed(
                    new ModelChoice("openai", "openai/text-embedding-3-small"), "netherite"));
            assertEquals(2, vector.length);
        }

        var call = transport.lastCall();
        assertEquals("https://api.openai.com/v1/embeddings", call.url());
        assertEquals("Bearer sk-openai", call.headers().get("Authorization"));
        assertTrue(call.body().contains("\"model\":\"text-embedding-3-small\""));
    }

    @Test
    void aRequestWithNoProviderIsRoutedByItsNameAsBefore() {
        var transport = TestAccess.transport().reply(200, ANSWER);
        var keys = TestAccess.keys("sk-or-gateway").forPlatform("deepseek", "sk-deepseek");

        try (var client = TestAccess.client(transport, keys, ROUTES)) {
            await(client.complete(ask("")));
        }

        assertEquals("https://openrouter.ai/api/v1/chat/completions", transport.lastCall().url());
    }

    private static <T> T await(CompletableFuture<T> future) {
        return assertDoesNotThrow(() -> future.get(10, TimeUnit.SECONDS));
    }

    private static ModelException failure(CompletableFuture<?> future) {
        ExecutionException thrown = assertThrows(ExecutionException.class,
                () -> future.get(10, TimeUnit.SECONDS));
        return assertInstanceOf(ModelException.class, thrown.getCause());
    }
}
