package io.github.mcagentsmods.core.tool;

import java.util.Objects;

/**
 * What a tool call produced, as the model will read it.
 *
 * <p><b>A failure is a result too.</b> A tool that was not found, arguments that would not
 * parse, a tool that threw or took too long: each comes back as text the model reads and can
 * act on — calling again differently, or telling the player what it could not do. Failing the
 * whole question instead would turn a recoverable mistake into a dead one.
 *
 * @param text    what to show the model
 * @param isError whether the call failed, so the model is not left to infer it
 */
public record ToolResult(String text, boolean isError) {

    public ToolResult {
        Objects.requireNonNull(text, "text");
    }

    /** A call that worked. */
    public static ToolResult ok(String text) {
        return new ToolResult(text, false);
    }

    /**
     * A call that failed.
     *
     * <p>Prefixed, because a model handed a bare sentence often repeats it to the player as
     * though it were the answer.
     */
    public static ToolResult error(String reason) {
        return new ToolResult("The tool call failed: " + reason, true);
    }
}
