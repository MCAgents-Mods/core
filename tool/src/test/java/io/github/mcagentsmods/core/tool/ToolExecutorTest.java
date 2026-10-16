package io.github.mcagentsmods.core.tool;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolExecutorTest {

    private final WorkerPool pool = new WorkerPool(4);
    private final ToolExecutor executor = new ToolExecutor(pool);
    private final ToolRegistry registry = new ToolRegistry();
    private final List<String> events = Collections.synchronizedList(new ArrayList<>());

    @AfterEach
    void closePool() {
        pool.close();
    }

    private void tool(String name, ExecutionMode mode, long millis) {
        registry.register(ToolSpec.builder(name).description("Test tool " + name).category("test").mode(mode).build(),
                arguments -> {
                    events.add("start " + name);
                    Thread.sleep(millis);
                    events.add("end " + name);
                    return ToolResult.ok(name + " " + arguments.string("tag", ""));
                });
    }

    private static ToolInvocation call(String id, String name) {
        return new ToolInvocation(id, name, "{\"tag\":\"" + id + "\"}");
    }

    private List<ToolOutcome> run(ToolInvocation... calls) throws Exception {
        return executor.run(List.of(calls), registry).get(10, TimeUnit.SECONDS);
    }

    @Test
    void concurrentCallsRunTogether() throws Exception {
        CountDownLatch bothRunning = new CountDownLatch(2);
        for (String name : List.of("a", "b")) {
            registry.register(ToolSpec.builder(name).description("Waits for its twin").category("test")
                    .mode(ExecutionMode.CONCURRENT).build(), arguments -> {
                bothRunning.countDown();
                // Only returns if the other call is running at the same time.
                return ToolResult.ok(String.valueOf(bothRunning.await(5, TimeUnit.SECONDS)));
            });
        }

        List<ToolOutcome> outcomes = run(call("1", "a"), call("2", "b"));

        assertEquals(List.of("true", "true"), outcomes.stream().map(ToolOutcome::text).toList());
    }

    @Test
    void aWaitingCallRunsAloneBetweenTheOthers() throws Exception {
        tool("read1", ExecutionMode.CONCURRENT, 40);
        tool("read2", ExecutionMode.CONCURRENT, 40);
        tool("write", ExecutionMode.WAIT, 10);
        tool("read3", ExecutionMode.CONCURRENT, 10);

        List<ToolOutcome> outcomes = run(call("1", "read1"), call("2", "read2"), call("3", "write"), call("4", "read3"));

        assertEquals(List.of("1", "2", "3", "4"), outcomes.stream().map(ToolOutcome::callId).toList());
        assertEquals(List.of("read1 1", "read2 2", "write 3", "read3 4"),
                outcomes.stream().map(ToolOutcome::text).toList());
        int writeStart = events.indexOf("start write");
        assertTrue(events.indexOf("end read1") < writeStart);
        assertTrue(events.indexOf("end read2") < writeStart);
        assertTrue(events.indexOf("end write") < events.indexOf("start read3"));
    }

    @Test
    void waitingCallsRunOneAtATimeInTheOrderAsked() throws Exception {
        tool("first", ExecutionMode.WAIT, 30);
        tool("second", ExecutionMode.WAIT, 1);

        run(call("1", "first"), call("2", "second"));

        assertEquals(List.of("start first", "end first", "start second", "end second"), events);
    }

    @Test
    void resultsComeBackInTheOrderAskedNotTheOrderFinished() throws Exception {
        tool("slow", ExecutionMode.CONCURRENT, 80);
        tool("fast", ExecutionMode.CONCURRENT, 1);

        List<ToolOutcome> outcomes = run(call("1", "slow"), call("2", "fast"));

        assertEquals(List.of("slow 1", "fast 2"), outcomes.stream().map(ToolOutcome::text).toList());
        assertTrue(events.indexOf("end fast") < events.indexOf("end slow"));
    }

    @Test
    void everyFailureIsAResultTheModelCanRead() throws Exception {
        registry.register(ToolSpec.builder("throws").description("Always fails").category("test").build(),
                arguments -> {
                    throw new IllegalStateException("the chest is locked");
                });
        registry.register(ToolSpec.builder("needs").description("Needs an item").category("test").build(),
                arguments -> ToolResult.ok(arguments.string("item")));
        registry.register(ToolSpec.builder("hangs").description("Never finishes").category("test")
                .timeout(Duration.ofMillis(100)).build(), arguments -> {
                    Thread.sleep(10_000);
                    return ToolResult.ok("late");
                });
        registry.registerAsync(ToolSpec.builder("rejects").description("Fails later").category("test").build(),
                arguments -> CompletableFuture.failedFuture(new IllegalStateException("server gone")));

        List<ToolOutcome> outcomes = run(
                new ToolInvocation("1", "missing", "{}"),
                new ToolInvocation("2", "throws", "{}"),
                new ToolInvocation("3", "needs", "{}"),
                new ToolInvocation("4", "needs", "{not json"),
                new ToolInvocation("5", "hangs", "{}"),
                new ToolInvocation("6", "rejects", "{}"));

        assertTrue(outcomes.stream().allMatch(ToolOutcome::isError));
        assertEquals("The tool call failed: there is no tool called 'missing'", outcomes.get(0).text());
        assertEquals("The tool call failed: the chest is locked", outcomes.get(1).text());
        assertEquals("The tool call failed: The argument 'item' is required", outcomes.get(2).text());
        assertTrue(outcomes.get(3).text().contains("not valid JSON"));
        assertEquals("The tool call failed: it did not finish within 100 milliseconds", outcomes.get(4).text());
        assertEquals("The tool call failed: server gone", outcomes.get(5).text());
    }

    @Test
    void anAsyncToolIsAnsweredWhenItsStageCompletes() throws Exception {
        CompletableFuture<ToolResult> later = new CompletableFuture<>();
        registry.registerAsync(ToolSpec.builder("later").description("Answers later").category("test").build(),
                arguments -> later);

        CompletableFuture<List<ToolOutcome>> running = executor.run(List.of(call("1", "later")), registry);
        Thread.sleep(50);
        assertFalse(running.isDone());
        assertEquals(1, pool.running());

        later.complete(ToolResult.ok("here"));
        assertEquals("here", running.get(5, TimeUnit.SECONDS).get(0).text());
    }

    @Test
    void theLimitHoldsAcrossConcurrentCalls() throws Exception {
        pool.setLimit(2);
        AtomicInteger now = new AtomicInteger();
        AtomicInteger most = new AtomicInteger();
        registry.register(ToolSpec.builder("busy").description("Takes a while").category("test")
                .mode(ExecutionMode.CONCURRENT).build(), arguments -> {
            most.accumulateAndGet(now.incrementAndGet(), Math::max);
            Thread.sleep(20);
            now.decrementAndGet();
            return ToolResult.ok("done");
        });
        List<ToolInvocation> calls = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            calls.add(call(String.valueOf(i), "busy"));
        }

        List<ToolOutcome> outcomes = executor.run(calls, registry).get(10, TimeUnit.SECONDS);

        assertEquals(8, outcomes.size());
        assertEquals(2, most.get());
    }

    @Test
    void anEmptyTurnAnswersAtOnce() throws Exception {
        assertTrue(executor.run(List.of(), registry).get(1, TimeUnit.SECONDS).isEmpty());
    }
}
