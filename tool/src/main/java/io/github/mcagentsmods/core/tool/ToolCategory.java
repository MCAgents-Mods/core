package io.github.mcagentsmods.core.tool;

import java.util.Objects;

/**
 * A group of tools the agent can pick as a whole.
 *
 * <p>The first thing the agent does with a prompt is choose the categories that fit it, from
 * their ids and descriptions alone, so only the tools in those categories are described to the
 * model afterwards. A tool belongs to one or more categories.
 *
 * @param id          what the model picks it by: 1 to 64 letters, digits, dashes or
 *                    underscores
 * @param description what the tools in it are for, as the model reads it; may be blank for a
 *                    category a tool named before anyone described it
 */
public record ToolCategory(String id, String description) {

    public ToolCategory {
        Objects.requireNonNull(id, "id");
        description = description == null ? "" : description.strip();
        ToolNames.requireValid(id, "category");
    }

    /** Whether the category carries a description yet. */
    public boolean isDescribed() {
        return !description.isEmpty();
    }
}
