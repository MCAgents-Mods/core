package io.github.mcagentsmods.core.api.model;

import io.github.mcagentsmods.core.api.chat.ChatMessage;
import io.github.mcagentsmods.core.api.chat.ChatRole;
import org.junit.jupiter.api.Test;

import java.util.List;

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
}
