package io.github.mcagentsmods.core.tool;

import java.util.Objects;

/**
 * One call the model asked for: which tool, with what, and the id its result must carry.
 *
 * @param id        the provider's id for the call; the result is matched back to it
 * @param name      the tool asked for
 * @param arguments the arguments, as JSON text, verbatim; parsed only when the call runs, so a
 *                  model that sent malformed JSON is told so rather than failing the question
 */
public record ToolInvocation(String id, String name, String arguments) {

    public ToolInvocation {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        arguments = arguments == null || arguments.isBlank() ? "{}" : arguments;
    }
}
