package io.github.mcagentsmods.core.tool;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * A tool as the registry holds it: its spec and the code it runs.
 *
 * <p>Both kinds of tool body are held the same way, as a call that produces a stage, so the
 * executor runs every tool through one path and never asks which kind it has.
 */
public final class RegisteredTool {

    private final ToolSpec spec;
    private final AsyncTool body;

    private RegisteredTool(ToolSpec spec, AsyncTool body) {
        this.spec = Objects.requireNonNull(spec, "spec");
        this.body = Objects.requireNonNull(body, "body");
    }

    static RegisteredTool of(ToolSpec spec, Tool body) {
        Objects.requireNonNull(body, "body");
        return new RegisteredTool(spec, arguments -> CompletableFuture.completedFuture(body.call(arguments)));
    }

    static RegisteredTool ofAsync(ToolSpec spec, AsyncTool body) {
        return new RegisteredTool(spec, body);
    }

    public ToolSpec spec() {
        return spec;
    }

    public String name() {
        return spec.name();
    }

    /**
     * Runs the tool on the calling thread.
     *
     * <p>{@link ToolExecutor} calls this on a worker thread and turns every way it can go wrong
     * into a {@link ToolResult}. Call it directly only when you want the failures as
     * exceptions.
     */
    public CompletionStage<ToolResult> invoke(ToolArguments arguments) throws Exception {
        CompletionStage<ToolResult> stage = body.call(arguments);
        if (stage == null) {
            throw new IllegalStateException("The tool '" + spec.name() + "' returned no result");
        }
        return stage;
    }

    @Override
    public String toString() {
        return "RegisteredTool[" + spec.name() + "]";
    }
}
