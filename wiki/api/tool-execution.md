# Tool Execution

A tool's body is a `Tool`, whose work is done when it returns, or an `AsyncTool`, whose work is
done when the stage it returns completes. Every call runs on a `WorkerPool`, never on the
game's thread, and at most the pool's limit run at once. `ToolExecutor` runs the calls of one
model turn in stages and turns every failure into an error result. The agent does all of this
for you; call the executor yourself only to run tools outside the agent.

```java
ToolRegistry tools = new ToolRegistry();
tools.register(
        ToolSpec.builder("echo")
                .description("Repeats its text")
                .category("demo")
                .parameters(ToolSchema.builder().string("text", "What to repeat", true).build())
                .mode(ExecutionMode.CONCURRENT)
                .build(),
        arguments -> ToolResult.ok(arguments.string("text")));

try (WorkerPool pool = new WorkerPool(2)) {
    List<ToolOutcome> outcomes = new ToolExecutor(pool).run(List.of(
            new ToolInvocation("call-1", "echo", "{\"text\":\"hi\"}"),
            new ToolInvocation("call-2", "echo", "{}"),
            new ToolInvocation("call-3", "missing", "{}")), tools).join();

    outcomes.forEach(outcome -> System.out.println(outcome.callId() + " " + outcome.text()));
}
```

```
call-1 hi
call-2 The tool call failed: The argument 'text' is required
call-3 The tool call failed: there is no tool called 'missing'
```

Every type on this page is in `io.github.mcagentsmods.core.tool`, in `core-tool`,
except `ClientThreadTools`. What the two kinds of work and the worker limit mean is on
[`../information/tools.md`](../information/tools.md).

## Tool and AsyncTool

Both are functional interfaces; register the first with `register` and the second with
`registerAsync` ([`tool-registry.md`](tool-registry.md)).

| Interface | Method | Use it when |
|---|---|---|
| `Tool` | `ToolResult call(ToolArguments arguments) throws Exception` | The work is done when the method returns. It runs on a worker thread and holds its slot until it returns. |
| `AsyncTool` | `CompletionStage<ToolResult> call(ToolArguments arguments) throws Exception` | The work finishes later: a request to a server, or work handed to the game's thread. `call` runs on a worker thread and should return quickly; the call keeps its slot until the stage completes. |

Neither runs on the game's thread. Anything either throws, a stage that fails, and a call past
its timeout become error results, listed under [`ToolExecutor`](#toolexecutor).

## ClientThreadTools

Most of Minecraft may only be touched from the game's thread. In `core-client`,
`io.github.mcagentsmods.core.client.agent.ClientThreadTools` turns a `Tool` into an
`AsyncTool` whose body runs there, between frames:

```java
Agents.tools().registerAsync(spec, ClientThreadTools.onClientThread(arguments -> {
    LocalPlayer player = Minecraft.getInstance().player;
    return player == null
            ? ToolResult.error("the player is not in a world")
            : ToolResult.ok(player.blockPosition().toShortString());
}));
```

| Method | Returns | What it does |
|---|---|---|
| `onClientThread(Tool body)` | `AsyncTool` | Static. Hands `body` to `Minecraft.getInstance().execute` when the tool is called, and completes the stage with what it returns or throws. |

The call keeps its worker slot and its timeout while the body waits for the game's thread.
Keep the body short: the game draws no frame while it runs.

## ToolExecutor

| Member | Returns | What it does |
|---|---|---|
| `ToolExecutor(WorkerPool pool)` | — | An executor on that pool. |
| `ToolExecutor.shared()` | `ToolExecutor` | Static. A new executor on `WorkerPool.shared()`. |
| `pool()` | `WorkerPool` | The pool it runs on. |
| `run(List<ToolInvocation> calls, ToolLookup tools)` | `CompletableFuture<List<ToolOutcome>>` | Runs one turn's calls against the tools `tools` finds, and completes with one outcome per call, in the order of `calls`. |
| `run(ToolInvocation call, ToolLookup tools)` | `CompletableFuture<ToolOutcome>` | One call, run alone. |

The calls run in stages, as [`../information/tools.md`](../information/tools.md#two-kinds-of-work)
describes. A call to a tool `tools` cannot find is treated as `CONCURRENT` and answered at
once.

The future never fails. Each way a call can go wrong becomes an error result instead:

| What went wrong | What the model reads |
|---|---|
| No tool by that name | `The tool call failed: there is no tool called 'missing'` |
| Arguments that are not JSON, or not a JSON object | `The tool call failed: The arguments are not valid JSON.` and where it went wrong, or `The tool call failed: The arguments must be a JSON object` |
| A `ToolArgumentException` | `The tool call failed: ` and its message; see [`tool-arguments.md`](tool-arguments.md) |
| Any other exception, or a failed stage | `The tool call failed: ` and its message, or the exception's class name when it has none |
| A `Tool` that returned `null` | `The tool call failed: the tool returned nothing` |
| An `AsyncTool` that returned no stage | `The tool call failed: The tool 'name' returned no result` |
| Past the spec's timeout | `The tool call failed: it did not finish within 60 seconds`, or `… within 200 milliseconds` for a timeout under a second |

## ToolInvocation

`record ToolInvocation(String id, String name, String arguments)`: one call the model asked
for.

| Component | Meaning |
|---|---|
| `id` | The provider's id for the call. The outcome carries it, so the result is matched back. |
| `name` | The tool asked for. |
| `arguments` | The arguments as JSON text, verbatim; `null` or blank becomes `{}`. Parsed only when the call runs, so malformed JSON is answered rather than failing the turn. |

## ToolOutcome

`record ToolOutcome(String callId, String toolName, ToolResult result)`: one call and what it
produced.

| Member | Meaning |
|---|---|
| `callId` | The `ToolInvocation.id` it answers. |
| `toolName` | The tool that was asked for. |
| `result` | The `ToolResult`. |
| `isError()` | `result.isError()`. |
| `text()` | `result.text()`, what the model reads. |

## ToolLookup

`Optional<RegisteredTool> find(String name)`, a functional interface: the one step between a
model's call and the code it runs. `ToolRegistry` and its `Snapshot` implement it with one hash
lookup. A lambda can narrow it; the agent passes one that finds only the tools it offered the
model, so a call to any other tool is answered as unknown.

## RegisteredTool

A tool as the registry holds it. Only the registry makes one.

| Method | Returns | What it does |
|---|---|---|
| `spec()` | `ToolSpec` | Its spec. |
| `name()` | `String` | `spec().name()`. |
| `invoke(ToolArguments arguments)` | `CompletionStage<ToolResult>` | Runs the body on the calling thread, with no pool, no timeout and no error results: failures arrive as exceptions. Throws `Exception`. |

## WorkerPool

Runs work off the caller's thread, never more than `limit()` pieces at once. The limit counts
work, not threads: a piece holds a slot from when it starts until its result is complete, so a
tool waiting on a server counts exactly as one computing something. Work beyond the limit
waits in a queue, first come first served. Threads are daemons, so an open pool never keeps
the game from exiting.

| Member | Returns | What it does |
|---|---|---|
| `DEFAULT_LIMIT`, `MIN_LIMIT`, `MAX_LIMIT` | `int` | 2, 1 and 32. |
| `WorkerPool.shared()` | `WorkerPool` | Static. The pool core's agent runs every tool on, and whose limit the player sets. `Agents.workers()` returns it. |
| `WorkerPool()` | — | A pool of your own with the default limit. |
| `WorkerPool(int limit)` | — | A pool of your own with that limit. |
| `WorkerPool.clampLimit(int limit)` | `int` | Static. `limit` brought into 1 to 32. |
| `limit()` | `int` | How many may run at once. |
| `setLimit(int limit)` | `void` | Changes it now. Raising it starts queued work at once; lowering it stops nothing, and starts nothing new until fewer than the new limit run. |
| `running()` | `int` | How many hold a slot now. |
| `queued()` | `int` | How many wait for one. |
| `submit(Callable<? extends T> work)` | `CompletableFuture<T>` | Runs a blocking body; the slot is held until it returns. |
| `submit(Callable<? extends T> work, Duration timeout)` | `CompletableFuture<T>` | The same, until it returns or `timeout` passes. |
| `submitAsync(Callable<? extends CompletionStage<T>> work)` | `CompletableFuture<T>` | Starts a body that returns a stage; the slot is held until the stage completes. |
| `submitAsync(Callable<? extends CompletionStage<T>> work, Duration timeout)` | `CompletableFuture<T>` | The same, until the stage completes or `timeout` passes. |
| `close()` | `void` | Queued work fails with `RejectedExecutionException`, running work is interrupted, and new work fails the same way. Safe to call twice. |

A limit outside 1 to 32, in the constructor or in `setLimit`, throws
`IllegalArgumentException`. Closing the shared pool throws `UnsupportedOperationException`: it
lives as long as the game.

A timeout counts from when the work starts, not while it waits in the queue; `null`, zero or a
negative one means no limit. Work past its timeout fails with `TimeoutException` and gives its
slot back, and a blocking body is interrupted. One that ignores the interrupt keeps its thread
until it returns, but no longer its slot. Cancelling the future interrupts it the same way.

The shared pool's limit is the player's setting. Change it through `Agents.update`
([`settings.md`](settings.md)), which saves it and shows it on the settings screen; a
`setLimit` on the shared pool directly is neither, and the player's next save replaces it.
