package io.github.mcagentsmods.core.tool;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkerPoolTest {

    private final List<WorkerPool> pools = new ArrayList<>();

    private WorkerPool pool(int limit) {
        WorkerPool pool = new WorkerPool(limit);
        pools.add(pool);
        return pool;
    }

    @AfterEach
    void closePools() {
        pools.forEach(WorkerPool::close);
    }

    @Test
    void defaultsToTwo() {
        WorkerPool pool = new WorkerPool();
        pools.add(pool);
        assertEquals(2, pool.limit());
        assertEquals(WorkerPool.DEFAULT_LIMIT, WorkerPool.shared().limit());
    }

    @Test
    void neverRunsMoreThanTheLimitAtOnce() throws Exception {
        WorkerPool pool = pool(2);
        AtomicInteger now = new AtomicInteger();
        AtomicInteger most = new AtomicInteger();
        List<CompletableFuture<Integer>> work = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            int index = i;
            work.add(pool.submit(() -> {
                most.accumulateAndGet(now.incrementAndGet(), Math::max);
                Thread.sleep(20);
                now.decrementAndGet();
                return index;
            }));
        }
        for (int i = 0; i < work.size(); i++) {
            assertEquals(i, work.get(i).get(5, TimeUnit.SECONDS));
        }
        assertEquals(2, most.get());
        assertEquals(0, pool.running());
    }

    @Test
    void anAsyncPieceOfWorkHoldsItsSlotUntilItsStageCompletes() throws Exception {
        WorkerPool pool = pool(1);
        CompletableFuture<String> slow = new CompletableFuture<>();
        CompletableFuture<String> first = pool.submitAsync(() -> slow);
        CompletableFuture<String> second = pool.submit(() -> "second");

        waitUntil(() -> pool.running() == 1);
        Thread.sleep(50);
        assertEquals(1, pool.queued());
        assertTrue(!second.isDone());

        slow.complete("first");
        assertEquals("first", first.get(5, TimeUnit.SECONDS));
        assertEquals("second", second.get(5, TimeUnit.SECONDS));
    }

    @Test
    void raisingTheLimitStartsQueuedWorkAtOnce() throws Exception {
        WorkerPool pool = pool(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch started = new CountDownLatch(3);
        List<CompletableFuture<Object>> work = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            work.add(pool.submit(() -> {
                started.countDown();
                release.await();
                return null;
            }));
        }
        waitUntil(() -> pool.running() == 1 && pool.queued() == 2);

        pool.setLimit(3);

        assertTrue(started.await(5, TimeUnit.SECONDS));
        assertEquals(3, pool.running());
        release.countDown();
        CompletableFuture.allOf(work.toArray(CompletableFuture[]::new)).get(5, TimeUnit.SECONDS);
    }

    @Test
    void loweringTheLimitStopsNothingButStartsNothingNew() throws Exception {
        WorkerPool pool = pool(3);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger now = new AtomicInteger();
        AtomicInteger mostAfterLowering = new AtomicInteger();
        CountDownLatch firstThree = new CountDownLatch(3);
        List<CompletableFuture<Object>> work = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            work.add(pool.submit(() -> {
                now.incrementAndGet();
                firstThree.countDown();
                release.await();
                now.decrementAndGet();
                return null;
            }));
        }
        assertTrue(firstThree.await(5, TimeUnit.SECONDS));

        pool.setLimit(1);
        for (int i = 0; i < 4; i++) {
            work.add(pool.submit(() -> {
                mostAfterLowering.accumulateAndGet(now.incrementAndGet(), Math::max);
                Thread.sleep(10);
                now.decrementAndGet();
                return null;
            }));
        }
        assertEquals(3, pool.running());
        assertEquals(4, pool.queued());

        release.countDown();
        CompletableFuture.allOf(work.toArray(CompletableFuture[]::new)).get(5, TimeUnit.SECONDS);
        assertEquals(1, mostAfterLowering.get());
    }

    @Test
    void workPastItsTimeoutFailsAndGivesItsSlotBack() throws Exception {
        WorkerPool pool = pool(1);
        CompletableFuture<Object> stuck = pool.submit(() -> {
            Thread.sleep(10_000);
            return null;
        }, Duration.ofMillis(100));
        CompletableFuture<String> next = pool.submit(() -> "next");

        ExecutionException failure = assertThrows(ExecutionException.class, () -> stuck.get(5, TimeUnit.SECONDS));
        assertInstanceOf(TimeoutException.class, failure.getCause());
        assertEquals("next", next.get(5, TimeUnit.SECONDS));
    }

    @Test
    void aFailureCompletesTheFutureAndFreesTheSlot() throws Exception {
        WorkerPool pool = pool(1);
        CompletableFuture<Object> failed = pool.submit(() -> {
            throw new IllegalStateException("broken");
        });
        ExecutionException failure = assertThrows(ExecutionException.class, () -> failed.get(5, TimeUnit.SECONDS));
        assertEquals("broken", failure.getCause().getMessage());
        assertEquals("ok", pool.submit(() -> "ok").get(5, TimeUnit.SECONDS));
    }

    @Test
    void refusesALimitOutOfRange() {
        WorkerPool pool = pool(2);
        assertThrows(IllegalArgumentException.class, () -> pool.setLimit(0));
        assertThrows(IllegalArgumentException.class, () -> pool.setLimit(WorkerPool.MAX_LIMIT + 1));
        assertEquals(1, WorkerPool.clampLimit(-5));
        assertEquals(WorkerPool.MAX_LIMIT, WorkerPool.clampLimit(1000));
    }

    @Test
    void closingFailsQueuedWorkAndRefusesNewWork() throws Exception {
        WorkerPool pool = new WorkerPool(1);
        CountDownLatch release = new CountDownLatch(1);
        pool.submit(() -> {
            release.await();
            return null;
        });
        CompletableFuture<String> queued = pool.submit(() -> "never");
        waitUntil(() -> pool.queued() == 1);

        pool.close();

        ExecutionException failure = assertThrows(ExecutionException.class, () -> queued.get(5, TimeUnit.SECONDS));
        assertInstanceOf(RejectedExecutionException.class, failure.getCause());
        assertTrue(pool.submit(() -> "late").isCompletedExceptionally());
        assertThrows(UnsupportedOperationException.class, () -> WorkerPool.shared().close());
    }

    private static void waitUntil(java.util.function.BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("The condition never held");
            }
            Thread.sleep(5);
        }
    }
}
