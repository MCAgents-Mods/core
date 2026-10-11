package io.github.mcagentsmods.core.common.model;

import io.github.mcagentsmods.core.api.http.HttpCall;
import io.github.mcagentsmods.core.api.model.ChatRequest;
import io.github.mcagentsmods.core.api.model.PromptMessage;
import io.github.mcagentsmods.core.api.model.ProviderRoutes;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * What the gateway is told about the mod making a request.
 *
 * <p>Several mods share one client implementation, so each must be able to name itself —
 * and none may be able to smuggle a second header in through its name.
 */
class AttributionTest {

    private static final String ANSWER = """
            {"choices":[{"message":{"role":"assistant","content":"Hello."}}]}
            """;

    private static HttpCall callWith(OpenRouterClient.Attribution attribution) {
        var transport = TestAccess.transport().reply(200, ANSWER);
        try (var client = TestAccess.client(transport, TestAccess.keys("sk-or-v1-first"),
                ProviderRoutes.throughOpenRouter(), attribution)) {
            assertDoesNotThrow(() -> client.complete(new ChatRequest("openai/gpt-4o-mini",
                    List.of(PromptMessage.user("Hi")))).get(10, TimeUnit.SECONDS));
        }
        return transport.lastCall();
    }

    @Test
    void namesTheCallingModToTheGateway() {
        HttpCall call = callWith(new OpenRouterClient.Attribution(
                "https://github.com/MCAgents-Mods/chat", "MCAgentsClient"));

        assertEquals("https://github.com/MCAgents-Mods/chat", call.headers().get("HTTP-Referer"));
        assertEquals("MCAgentsClient", call.headers().get("X-Title"));
    }

    @Test
    void sendsNoAttributionWhenAskedForNone() {
        HttpCall call = callWith(OpenRouterClient.Attribution.NONE);

        assertFalse(call.headers().containsKey("HTTP-Referer"));
        assertFalse(call.headers().containsKey("X-Title"));
    }

    @Test
    void refusesANameThatCouldStartAnotherHeader() {
        assertThrows(IllegalArgumentException.class,
                () -> new OpenRouterClient.Attribution("https://example.com", "Mod\r\nX-Evil: 1"));
        assertThrows(IllegalArgumentException.class,
                () -> new OpenRouterClient.Attribution("https://example.com\nX-Evil: 1", "Mod"));
    }
}
