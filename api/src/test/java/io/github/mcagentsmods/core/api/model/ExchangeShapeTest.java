package io.github.mcagentsmods.core.api.model;

import io.github.mcagentsmods.core.api.chat.ChatMessage;
import io.github.mcagentsmods.core.api.chat.ChatRole;
import io.github.mcagentsmods.core.api.http.HttpCall;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The records a model exchange is made of, and the rules they enforce on construction.
 */
class ExchangeShapeTest {

    @Test
    void reportsWhetherTheProviderReusedAnything() {
        TokenUsage cold = new TokenUsage(1000, 50, 0);
        TokenUsage warm = new TokenUsage(1000, 50, 800);

        assertFalse(cold.hadCacheHit());
        assertTrue(warm.hadCacheHit());
        assertEquals(0.8, warm.cacheHitRate(), 1e-9);
        assertEquals(1050, warm.totalTokens());
        assertEquals(0.0, TokenUsage.UNKNOWN.cacheHitRate());
    }

    @Test
    void refusesAToolNameAProviderWouldRejectTheWholeRequestOver() {
        assertTrue(ToolDefinition.isAcceptable("files__read_file"));
        assertFalse(ToolDefinition.isAcceptable("read file"));
        assertFalse(ToolDefinition.isAcceptable("x".repeat(65)));
        assertThrows(IllegalArgumentException.class,
                () -> new ToolDefinition("read.file", "", null));
    }

    @Test
    void givesAToolWithNoSchemaAnEmptyObjectSchema() {
        assertEquals("{\"type\":\"object\",\"properties\":{}}",
                new ToolDefinition("ping", null, " ").parametersSchema());
    }

    @Test
    void keepsAToolResultTiedToTheCallItAnswers() {
        PromptMessage result = PromptMessage.toolResult("call_1", "42");

        assertEquals(ChatRole.TOOL, result.role());
        assertEquals("call_1", result.toolCallId());
        assertTrue(result.isToolExchange());
        assertFalse(PromptMessage.user("hi").isToolExchange());
    }

    @Test
    void refusesAToolTurnThatCannotNameItsCall() {
        // A stored ChatMessage has nowhere to carry the call id, and every vendor rejects a
        // tool turn without one.
        assertThrows(IllegalArgumentException.class, () -> new ChatMessage(ChatRole.TOOL, "42"));
        assertEquals(ChatRole.TOOL, ChatRole.fromCode(" Tool "));
    }

    @Test
    void continuesARequestWithTheSameModelAndTools() {
        ToolDefinition ping = new ToolDefinition("ping", "", null);
        ChatRequest first = new ChatRequest("openai/gpt-4o-mini",
                List.of(PromptMessage.user("hi")), List.of(ping));

        ChatRequest next = first.continuedWith(List.of(PromptMessage.user("hi"),
                PromptMessage.toolResult("call_1", "pong")));

        assertEquals(first.model(), next.model());
        assertEquals(List.of(ping), next.tools());
        assertThrows(IllegalArgumentException.class,
                () -> new ChatRequest(" ", List.of(PromptMessage.user("hi"))));
        assertThrows(IllegalArgumentException.class,
                () -> new ChatRequest("m", List.of()));
    }

    @Test
    void classifiesWhichFailuresAreWorthRetrying() {
        assertTrue(new ModelException(ModelException.Kind.RATE_LIMIT, "").isRetryable());
        assertTrue(new ModelException(ModelException.Kind.TRANSPORT, "").isRetryable());
        // Already billed: sending it again bills the player again for the same broken answer.
        assertFalse(new ModelException(ModelException.Kind.MALFORMED_RESPONSE, "").isRetryable());
        assertFalse(new ModelException(ModelException.Kind.AUTHENTICATION, "").isRetryable());
    }

    @Test
    void namesACallsHeadersWithoutPrintingTheKey() {
        HttpCall call = HttpCall.post("https://openrouter.ai/api/v1/chat/completions",
                Map.of("Authorization", "Bearer sk-or-v1-neverprinted"), "{\"secret\":1}");

        assertTrue(call.toString().contains("Authorization"));
        assertFalse(call.toString().contains("neverprinted"));
        assertFalse(call.toString().contains("secret"));
    }

    @Test
    void neverHandsAGatewayKeyToAnotherPlatformByDefault() {
        ApiKeySource gatewayOnly = new ApiKeySource() {
            @Override
            public CompletableFuture<Optional<String>> next() {
                return CompletableFuture.completedFuture(Optional.of("sk-or-v1-gateway"));
            }

            @Override
            public CompletableFuture<Void> reject(String key) {
                return CompletableFuture.completedFuture(null);
            }
        };

        assertEquals(Optional.of("sk-or-v1-gateway"), gatewayOnly.next("openrouter").join());
        assertEquals(Optional.empty(), gatewayOnly.next("openai").join());
    }

    @Test
    void treatsOnlyAnUnauthorisedOrUnpaidAnswerAsADeadKey() {
        assertEquals(ModelException.Kind.AUTHENTICATION, ModelException.Kind.forStatus(401));
        assertEquals(ModelException.Kind.AUTHENTICATION, ModelException.Kind.forStatus(402));
        // A 403 is a refusal of this request — a moderated prompt, a region — not of the key.
        assertEquals(ModelException.Kind.REQUEST, ModelException.Kind.forStatus(403));
        assertEquals(ModelException.Kind.RATE_LIMIT, ModelException.Kind.forStatus(429));
        assertEquals(ModelException.Kind.SERVER, ModelException.Kind.forStatus(503));
    }
}
