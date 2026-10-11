package io.github.mcagentsmods.core.common.testing;

import io.github.mcagentsmods.core.api.http.HttpCall;
import io.github.mcagentsmods.core.api.http.HttpReply;
import io.github.mcagentsmods.core.api.http.HttpTransport;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A transport that answers from a script and records what it was asked.
 *
 * <p>This is what makes retry, key rotation, and JSON handling testable at all: none of
 * them needs a network, and none of them needs an API key.
 */
public final class StubTransport implements HttpTransport {

    private final Deque<Object> scripted = new ArrayDeque<>();
    private final List<HttpCall> calls = new ArrayList<>();
    private boolean closed;

    /** Queues a reply. */
    public StubTransport reply(int status, String body) {
        scripted.add(new HttpReply(status, body));
        return this;
    }

    /** Queues a transport failure — a dropped connection, a DNS failure, a timeout. */
    public StubTransport fail(Throwable cause) {
        scripted.add(cause);
        return this;
    }

    /**
     * Queues a refusal thrown by {@code send} itself, before anything is sent — what the JDK
     * client does with a header value it will not write.
     */
    public StubTransport refuse(RuntimeException refusal) {
        scripted.add(new Refusal(refusal));
        return this;
    }

    private record Refusal(RuntimeException exception) {
    }

    @Override
    public CompletableFuture<HttpReply> send(HttpCall call) {
        calls.add(call);
        if (scripted.isEmpty()) {
            return CompletableFuture.failedFuture(
                    new AssertionError("The client sent more requests than the test scripted"));
        }
        Object next = scripted.poll();
        if (next instanceof Refusal refusal) {
            throw refusal.exception();
        }
        if (next instanceof Throwable failure) {
            return CompletableFuture.failedFuture(failure);
        }
        return CompletableFuture.completedFuture((HttpReply) next);
    }

    public List<HttpCall> calls() {
        return List.copyOf(calls);
    }

    public HttpCall lastCall() {
        return calls.getLast();
    }

    public int callCount() {
        return calls.size();
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        closed = true;
    }
}
