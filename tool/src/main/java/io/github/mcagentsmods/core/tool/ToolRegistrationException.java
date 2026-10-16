package io.github.mcagentsmods.core.tool;

/**
 * A tool, or a category, could not be registered.
 *
 * <p>Thrown at registration, never later: a name another tool already holds, a name a model
 * provider would refuse, or a tool with no description or no category. Failing here names the
 * one tool at fault, in the mod that registered it, before any player question depends on it.
 */
public class ToolRegistrationException extends IllegalArgumentException {

    public ToolRegistrationException(String message) {
        super(message);
    }
}
