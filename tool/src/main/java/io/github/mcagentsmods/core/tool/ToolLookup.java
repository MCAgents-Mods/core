package io.github.mcagentsmods.core.tool;

import java.util.Optional;

/**
 * Finds a registered tool by name: the one step between a model's call and the code it runs.
 *
 * <p>Both {@link ToolRegistry} and its {@link ToolRegistry.Snapshot} answer it with one hash
 * lookup. Nothing tests the name against a list of known tools one by one.
 */
@FunctionalInterface
public interface ToolLookup {

    Optional<RegisteredTool> find(String name);
}
