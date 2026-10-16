package io.github.mcagentsmods.core.tool;

/**
 * What a tool does, when the work finishes before it returns.
 *
 * <p>It runs on a worker thread of the {@link WorkerPool}, never on the game's thread, and
 * holds one of the pool's slots until it returns. Code that must touch the game runs it there
 * from an {@link AsyncTool} instead. Anything it throws becomes an error result; throw
 * {@link ToolArgumentException} to tell the model its arguments were wrong.
 */
@FunctionalInterface
public interface Tool {

    ToolResult call(ToolArguments arguments) throws Exception;
}
