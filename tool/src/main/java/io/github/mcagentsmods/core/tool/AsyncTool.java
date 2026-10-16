package io.github.mcagentsmods.core.tool;

import java.util.concurrent.CompletionStage;

/**
 * What a tool does, when the work finishes later: a request to a server, or work handed to the
 * game's own thread.
 *
 * <p>{@link #call} is invoked on a worker thread and should return quickly. The call keeps its
 * slot in the {@link WorkerPool} until the stage completes, so the pool's limit holds for slow
 * asynchronous work as well as for work that blocks. A stage that completes exceptionally, or
 * not within the tool's timeout, becomes an error result.
 */
@FunctionalInterface
public interface AsyncTool {

    CompletionStage<ToolResult> call(ToolArguments arguments) throws Exception;
}
