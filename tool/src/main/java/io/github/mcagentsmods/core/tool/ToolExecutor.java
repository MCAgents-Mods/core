package io.github.mcagentsmods.core.tool;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

/**
 * Runs the calls a model asked for in one turn, on a {@link WorkerPool}.
 *
 * <p><b>Two kinds of work.</b> The calls are split into stages in the order they were asked.
 * {@link ExecutionMode#CONCURRENT} calls next to each other share a stage and run together, up
 * to the pool's limit. A {@link ExecutionMode#WAIT} call is a stage of its own: it starts only
 * once everything before it has finished, and nothing after it starts until it has. So
 * {@code [read, read, write, read]} runs both reads together, then the write alone, then the
 * last read.
 *
 * <p><b>Results come back in the order asked</b>, whatever order the work finished in, one per
 * call.
 *
 * <p><b>The future never fails.</b> An unknown tool, arguments that are not JSON, a tool that
 * throws, a stage that fails, and a call that runs past its tool's timeout each become an error
 * {@link ToolResult} the model reads, so one bad call never costs the player the answer.
 */
public final class ToolExecutor {

    private final WorkerPool pool;

    public ToolExecutor(WorkerPool pool) {
        this.pool = Objects.requireNonNull(pool, "pool");
    }

    /** An executor on the {@link WorkerPool#shared() shared pool}. */
    public static ToolExecutor shared() {
        return new ToolExecutor(WorkerPool.shared());
    }

    public WorkerPool pool() {
        return pool;
    }

    /**
     * Runs {@code calls} against the tools {@code tools} finds.
     *
     * @return one outcome per call, in the order of {@code calls}
     */
    public CompletableFuture<List<ToolOutcome>> run(List<ToolInvocation> calls, ToolLookup tools) {
        Objects.requireNonNull(calls, "calls");
        Objects.requireNonNull(tools, "tools");

        List<List<Prepared>> stages = stages(calls, tools);
        CompletableFuture<List<ToolOutcome>> chain = CompletableFuture.completedFuture(List.of());
        for (List<Prepared> stage : stages) {
            chain = chain.thenCompose(done -> runStage(stage).thenApply(next -> {
                List<ToolOutcome> all = new ArrayList<>(done);
                all.addAll(next);
                return all;
            }));
        }
        return chain.thenApply(List::copyOf);
    }

    /** One call to one tool, run alone. */
    public CompletableFuture<ToolOutcome> run(ToolInvocation call, ToolLookup tools) {
        return run(List.of(call), tools).thenApply(outcomes -> outcomes.get(0));
    }

    private static List<List<Prepared>> stages(List<ToolInvocation> calls, ToolLookup tools) {
        List<List<Prepared>> stages = new ArrayList<>();
        List<Prepared> together = new ArrayList<>();
        for (ToolInvocation call : calls) {
            Prepared prepared = new Prepared(call, tools.find(call.name()));
            if (prepared.waits()) {
                if (!together.isEmpty()) {
                    stages.add(together);
                    together = new ArrayList<>();
                }
                stages.add(List.of(prepared));
            } else {
                together.add(prepared);
            }
        }
        if (!together.isEmpty()) {
            stages.add(together);
        }
        return stages;
    }

    private CompletableFuture<List<ToolOutcome>> runStage(List<Prepared> stage) {
        List<CompletableFuture<ToolOutcome>> running = stage.stream().map(this::runOne).toList();
        return CompletableFuture.allOf(running.toArray(CompletableFuture[]::new))
                .thenApply(ignored -> running.stream().map(CompletableFuture::join).toList());
    }

    private CompletableFuture<ToolOutcome> runOne(Prepared prepared) {
        ToolInvocation call = prepared.call();
        if (prepared.tool().isEmpty()) {
            return CompletableFuture.completedFuture(outcome(call,
                    ToolResult.error("there is no tool called '" + call.name() + "'")));
        }
        RegisteredTool tool = prepared.tool().get();
        ToolArguments arguments;
        try {
            arguments = ToolArguments.parse(call.arguments());
        } catch (ToolArgumentException e) {
            return CompletableFuture.completedFuture(outcome(call, ToolResult.error(e.getMessage())));
        }
        CompletableFuture<ToolResult> result;
        try {
            result = pool.submitAsync(() -> tool.invoke(arguments), tool.spec().timeout());
        } catch (RuntimeException e) {
            result = CompletableFuture.failedFuture(e);
        }
        return result.handle((value, failure) -> {
            if (failure == null) {
                return outcome(call, value == null
                        ? ToolResult.error("the tool returned nothing")
                        : value);
            }
            return outcome(call, ToolResult.error(describe(failure, tool.spec())));
        });
    }

    private static ToolOutcome outcome(ToolInvocation call, ToolResult result) {
        return new ToolOutcome(call.id(), call.name(), result);
    }

    private static String describe(Throwable failure, ToolSpec spec) {
        Throwable cause = failure instanceof CompletionException && failure.getCause() != null
                ? failure.getCause()
                : failure;
        if (cause instanceof TimeoutException) {
            return "it did not finish within " + readable(spec.timeout());
        }
        if (cause instanceof ToolArgumentException) {
            return cause.getMessage();
        }
        String message = cause.getMessage();
        return message == null || message.isBlank() ? cause.getClass().getSimpleName() : message;
    }

    private static String readable(Duration timeout) {
        long seconds = timeout.toSeconds();
        if (seconds >= 1) {
            return seconds + (seconds == 1 ? " second" : " seconds");
        }
        return timeout.toMillis() + " milliseconds";
    }

    private record Prepared(ToolInvocation call, Optional<RegisteredTool> tool) {

        /** A call to a tool that is not there runs with the others: it is answered at once. */
        boolean waits() {
            return tool.isPresent() && tool.get().spec().mode() == ExecutionMode.WAIT;
        }
    }
}
