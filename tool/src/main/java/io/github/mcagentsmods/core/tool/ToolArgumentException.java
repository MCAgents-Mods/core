package io.github.mcagentsmods.core.tool;

/**
 * The model called a tool with arguments the tool cannot use.
 *
 * <p>A tool may throw it, and {@link ToolArguments} throws it for a missing or mistyped
 * argument. Either way the executor answers the call with the message as an error result, so
 * the model reads what was wrong and can call again.
 */
public class ToolArgumentException extends IllegalArgumentException {

    public ToolArgumentException(String message) {
        super(message);
    }
}
