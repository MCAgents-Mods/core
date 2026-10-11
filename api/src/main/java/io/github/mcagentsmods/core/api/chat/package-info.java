/**
 * The chat exchange model.
 *
 * <p>The records here describe one round trip with a language model: what is
 * sent ({@link io.github.mcagentsmods.core.api.chat.ChatRequest}), what comes back
 * ({@link io.github.mcagentsmods.core.api.chat.ChatResponse}), and what it cost
 * ({@link io.github.mcagentsmods.core.api.chat.TokenUsage}). They are the same
 * shape for every vendor — the differences between the OpenRouter, OpenAI,
 * DeepSeek, and Anthropic wire formats are absorbed by the client
 * implementations, never by these types.</p>
 *
 * <p>Every type in this package is an immutable record or enum, so an instance
 * can be handed to another thread without copying or locking.</p>
 */
package io.github.mcagentsmods.core.api.chat;
