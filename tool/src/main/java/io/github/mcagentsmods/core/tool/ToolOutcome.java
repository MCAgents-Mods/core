package io.github.mcagentsmods.core.tool;

import java.util.Objects;

/**
 * One call and what it produced, on its way back to the model.
 *
 * @param callId   the {@link ToolInvocation#id()} this answers
 * @param toolName the tool that was asked for
 * @param result   what it produced, or why it could not
 */
public record ToolOutcome(String callId, String toolName, ToolResult result) {

    public ToolOutcome {
        Objects.requireNonNull(callId, "callId");
        Objects.requireNonNull(toolName, "toolName");
        Objects.requireNonNull(result, "result");
    }

    /** Whether the call failed. */
    public boolean isError() {
        return result.isError();
    }

    /** What the model reads. */
    public String text() {
        return result.text();
    }
}
