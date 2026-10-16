package io.github.mcagentsmods.core.tool;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runs work off the caller's thread, never more than {@link #limit()} pieces at once.
 *
 * <p><b>The limit counts work, not threads.</b> A piece of work holds one slot from the moment
 * it starts until its result is complete — for a blocking body that is until it returns, and
 * for an asynchronous one until the stage it returned completes. So a tool waiting on a server
 * counts against the limit exactly as a tool computing something does. Work beyond the limit
 * waits in a queue, first come first served.
 *
 * <p><b>The limit changes while the pool runs.</b> Raising it starts queued work at once.
 * Lowering it stops nothing that is running: the pool simply starts nothing new until fewer
 * than the new limit are running. The player sets it in game; it defaults to
 * {@link #DEFAULT_LIMIT}.
 *
 * <p><b>Every piece of work may have a timeout.</b> Work that is not done in time completes
 * with a {@link TimeoutException} and gives its slot back. A blocking body is interrupted; one
 * that ignores the interrupt keeps its thread until it returns, but no longer its slot.
 *
 * <p>Threads are daemons, so a pool left open never keeps the game from exiting.
 */
public final class WorkerPool implements AutoCloseable {

    /** How many pieces of work run at once unless the player says otherwise. */
    public static final int DEFAULT_LIMIT = 2;

    /** The fewest the limit may be. */
    public static final int MIN_LIMIT = 1;

    /** The most the limit may be. Beyond this a game client gains nothing but contention. */
    public static final int MAX_LIMIT = 32;

    private static final WorkerPool SHARED = new WorkerPool(DEFAULT_LIMIT, "mcagents-tool-worker");

    private final Object lock = new Object();
    private final Queue<Job<?>> queued = new ArrayDeque<>();
    private final ExecutorService threads;
    private int limit;
    private int running;
    private boolean closed;

    /** A pool with the {@link #DEFAULT_LIMIT}. */
    public WorkerPool() {
        this(DEFAULT_LIMIT);
    }

    public WorkerPool(int limit) {
        this(limit, "mcagents-tool-pool");
    }

    private WorkerPool(int limit, String threadName) {
        this.limit = checkLimit(limit);
        this.threads = Executors.newCachedThreadPool(daemonThreads(threadName));
    }

    /** The pool core's agent runs every tool on, and whose limit the player sets. */
    public static WorkerPool shared() {
        return SHARED;
    }

    /** {@code limit} clamped to what the pool accepts. */
    public static int clampLimit(int limit) {
        return Math.max(MIN_LIMIT, Math.min(MAX_LIMIT, limit));
    }

    /** How many pieces of work may run at once. */
    public int limit() {
        synchronized (lock) {
            return limit;
        }
    }

    /**
     * Changes how many pieces of work may run at once, now.
     *
     * @throws IllegalArgumentException if {@code limit} is outside {@link #MIN_LIMIT} to
     *                                  {@link #MAX_LIMIT}
     */
    public void setLimit(int limit) {
        checkLimit(limit);
        synchronized (lock) {
            this.limit = limit;
            startWhatFits();
        }
    }

    /** How many pieces of work hold a slot right now. */
    public int running() {
        synchronized (lock) {
            return running;
        }
    }

    /** How many pieces of work wait for a slot. */
    public int queued() {
        synchronized (lock) {
            return queued.size();
        }
    }

    /** Runs {@code work} on a worker thread; its slot is held until it returns. */
    public <T> CompletableFuture<T> submit(Callable<? extends T> work) {
        return submit(work, null);
    }

    /**
     * Runs {@code work} on a worker thread; its slot is held until it returns or
     * {@code timeout} passes.
     *
     * @param timeout how long it may take, or {@code null} for no limit
     */
    public <T> CompletableFuture<T> submit(Callable<? extends T> work, Duration timeout) {
        Objects.requireNonNull(work, "work");
        return submitAsync(() -> CompletableFuture.completedFuture(work.call()), timeout);
    }

    /**
     * Starts {@code work} on a worker thread; its slot is held until the stage it returns
     * completes.
     */
    public <T> CompletableFuture<T> submitAsync(Callable<? extends CompletionStage<T>> work) {
        return submitAsync(work, null);
    }

    /**
     * Starts {@code work} on a worker thread; its slot is held until the stage it returns
     * completes or {@code timeout} passes.
     *
     * @param timeout how long it may take, or {@code null} for no limit
     */
    public <T> CompletableFuture<T> submitAsync(Callable<? extends CompletionStage<T>> work,
                                                Duration timeout) {
        Objects.requireNonNull(work, "work");
        Job<T> job = new Job<>(work, timeout);
        synchronized (lock) {
            if (closed) {
                return CompletableFuture.failedFuture(
                        new RejectedExecutionException("The worker pool is closed"));
            }
            queued.add(job);
            startWhatFits();
        }
        return job.result;
    }

    /**
     * Stops the pool: queued work fails, running work is interrupted, and nothing new is
     * accepted.
     */
    @Override
    public void close() {
        if (this == SHARED) {
            throw new UnsupportedOperationException("The shared worker pool lives as long as the game");
        }
        Queue<Job<?>> dropped;
        synchronized (lock) {
            if (closed) {
                return;
            }
            closed = true;
            dropped = new ArrayDeque<>(queued);
            queued.clear();
        }
        dropped.forEach(job -> job.result.completeExceptionally(
                new RejectedExecutionException("The worker pool closed before this work started")));
        threads.shutdownNow();
    }

    /** Starts queued work while there are free slots. Called with {@link #lock} held. */
    private void startWhatFits() {
        while (!closed && running < limit && !queued.isEmpty()) {
            Job<?> job = queued.poll();
            running++;
            start(job);
        }
    }

    private <T> void start(Job<T> job) {
        // The slot is given back exactly once, whichever way the result completes: the work
        // finishing, failing, or running out of time.
        job.result.whenComplete((value, failure) -> release());
        if (job.timeout != null) {
            job.result.orTimeout(job.timeout.toNanos(), TimeUnit.NANOSECONDS);
        }
        Future<?> task;
        try {
            task = threads.submit(() -> run(job));
        } catch (RejectedExecutionException e) {
            job.result.completeExceptionally(e);
            return;
        }
        // A blocking body still running when its time is up, or when the caller cancels it, is
        // interrupted, so a tool that honours interrupts gives its thread back as well as its slot.
        job.result.whenComplete((value, failure) -> {
            if (failure instanceof TimeoutException || failure instanceof CancellationException) {
                task.cancel(true);
            }
        });
    }

    private <T> void run(Job<T> job) {
        if (job.result.isDone()) {
            return;
        }
        CompletionStage<T> stage;
        try {
            stage = job.work.call();
            if (stage == null) {
                throw new IllegalStateException("The work returned no stage");
            }
        } catch (Throwable failure) {
            job.result.completeExceptionally(failure);
            return;
        }
        stage.whenComplete((value, failure) -> {
            if (failure == null) {
                job.result.complete(value);
            } else {
                job.result.completeExceptionally(unwrap(failure));
            }
        });
    }

    private void release() {
        synchronized (lock) {
            running--;
            startWhatFits();
        }
    }

    private static Throwable unwrap(Throwable failure) {
        return failure instanceof CompletionException && failure.getCause() != null
                ? failure.getCause()
                : failure;
    }

    private static int checkLimit(int limit) {
        if (limit < MIN_LIMIT || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("A worker limit must be between " + MIN_LIMIT
                    + " and " + MAX_LIMIT + ", not " + limit);
        }
        return limit;
    }

    private static ThreadFactory daemonThreads(String name) {
        AtomicInteger count = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, name + "-" + count.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    private static final class Job<T> {

        private final Callable<? extends CompletionStage<T>> work;
        private final Duration timeout;
        private final CompletableFuture<T> result = new CompletableFuture<>();

        private Job(Callable<? extends CompletionStage<T>> work, Duration timeout) {
            this.work = work;
            this.timeout = timeout == null || timeout.isNegative() || timeout.isZero() ? null : timeout;
        }
    }
}
