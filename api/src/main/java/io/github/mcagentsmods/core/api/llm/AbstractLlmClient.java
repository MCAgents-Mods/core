package io.github.mcagentsmods.core.api.llm;

import io.github.mcagentsmods.core.api.AgentException;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The bookkeeping every {@link LlmClient} would otherwise repeat: holding the
 * credentials, answering {@link #vendor()}, and tracking whether the client has
 * been closed.
 *
 * <p>Subclasses implement only the parts that actually differ between vendors —
 * building a request, reading a response — and call {@link #ensureOpen()} at
 * the top of each of those.</p>
 *
 * <p>This class performs no I/O and names no HTTP type, which is why it belongs
 * in the API layer: it constrains the shape of an implementation without
 * choosing one.</p>
 */
public abstract class AbstractLlmClient implements LlmClient {

    /**
     * The credentials this client authenticates with, fixed at construction.
     *
     * <p>Protected so a subclass can read the key, endpoint, timeout, and extra
     * headers when it builds a request. Immutable, so exposing it this way
     * cannot let a subclass reconfigure the client mid flight.</p>
     */
    protected final LlmCredentials credentials;

    /**
     * Whether {@link #close()} has run.
     *
     * <p>Atomic because a client is shared across threads by design — a mod
     * may well close it on the main thread while a request started from an
     * async task is still in flight.</p>
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Binds a client to its credentials.
     *
     * @param credentials The credentials to authenticate with.
     * @throws NullPointerException When {@code credentials} is {@code null}.
     */
    protected AbstractLlmClient(LlmCredentials credentials) {
        this.credentials = Objects.requireNonNull(credentials, "credentials cannot be null");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Taken from the credentials this client was built with.</p>
     */
    @Override
    public LlmVendor vendor() {
        return credentials.vendor();
    }

    /**
     * Returns the credentials this client authenticates with.
     *
     * @return The bound credentials, never {@code null}.
     */
    public LlmCredentials credentials() {
        return credentials;
    }

    /**
     * Reports whether this client has been closed.
     *
     * @return {@code true} once {@link #close()} has run.
     */
    public boolean isClosed() {
        return closed.get();
    }

    /**
     * Marks this client closed and reports whether this call is the one that
     * did it.
     *
     * <p>A subclass overriding {@link #close()} calls this first and releases
     * its resources only when it returns {@code true}, which is what makes
     * closing idempotent.</p>
     *
     * @return {@code true} when this call closed the client, {@code false} when
     *         it was already closed.
     */
    protected boolean markClosed() {
        return closed.compareAndSet(false, true);
    }

    /**
     * Fails when this client has already been closed.
     *
     * <p>Called at the top of every operation that would otherwise touch a
     * released resource. Subclasses translate the thrown exception into a
     * failed future rather than letting it escape, per the
     * {@link LlmClient} contract.</p>
     *
     * @throws AgentException When the client is closed.
     */
    protected void ensureOpen() {
        if (closed.get()) {
            throw new AgentException(
                    credentials.vendor(), "Client for " + credentials.vendor().code() + " is closed");
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Marks the client closed and releases nothing else. A subclass holding
     * a resource overrides this, calls {@code super.close()}, and releases it
     * only when {@link #markClosed()} reported that this call did the
     * closing.</p>
     */
    @Override
    public void close() {
        markClosed();
    }
}
