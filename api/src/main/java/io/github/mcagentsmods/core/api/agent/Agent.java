package io.github.mcagentsmods.core.api.agent;

import java.util.concurrent.CompletableFuture;

/**
 * Answers a question, calling the registered tools the question needs.
 *
 * <p>It works in three steps, each one request to the model:
 *
 * <ol>
 *   <li>the prompt and every tool category: the model picks the categories that fit, one or
 *       several at once;</li>
 *   <li>the prompt and the name and description of each tool in those categories: the model
 *       picks the tools it needs;</li>
 *   <li>the conversation and the full definition of each picked tool: the model calls them,
 *       for as many rounds as the request allows, and answers.</li>
 * </ol>
 *
 * <p>So the model is never sent every tool at once. Everything is asynchronous; the future
 * fails only when the model cannot be reached, never because a tool failed — a tool's failure
 * is a result the model reads.
 */
public interface Agent {

    CompletableFuture<AgentAnswer> answer(AgentRequest request);
}
