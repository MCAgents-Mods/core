package io.github.mcagentsmods.core.api.agent;

import io.github.mcagentsmods.core.tool.ToolOutcome;
import io.github.mcagentsmods.core.api.model.TokenUsage;

import java.util.List;
import java.util.Objects;

/**
 * What the agent answered, and how it got there.
 *
 * @param text         the answer for the player
 * @param model        the model that wrote the answer, as the provider reported it
 * @param usage        the tokens of every request the question took, added together
 * @param categories   the categories the agent picked, in the order picked
 * @param tools        the tools the agent offered the model for the answer
 * @param toolResults  every tool call the model made and what it produced, in order
 * @param toolRounds   how many rounds of tool calls ran
 * @param stoppedShort whether the model still wanted tools when its rounds ran out, so the
 *                     answer is what it had worked out by then
 */
public record AgentAnswer(
        String text,
        String model,
        TokenUsage usage,
        List<String> categories,
        List<String> tools,
        List<ToolOutcome> toolResults,
        int toolRounds,
        boolean stoppedShort) {

    public AgentAnswer {
        Objects.requireNonNull(text, "text");
        model = model == null ? "" : model;
        usage = usage == null ? TokenUsage.UNKNOWN : usage;
        categories = categories == null ? List.of() : List.copyOf(categories);
        tools = tools == null ? List.of() : List.copyOf(tools);
        toolResults = toolResults == null ? List.of() : List.copyOf(toolResults);
    }

    /** Whether any tool ran. */
    public boolean usedTools() {
        return !toolResults.isEmpty();
    }
}
