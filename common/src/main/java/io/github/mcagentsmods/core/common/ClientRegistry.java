package io.github.mcagentsmods.core.common;

import io.github.mcagentsmods.core.api.AgentException;
import io.github.mcagentsmods.core.api.llm.LlmClient;
import io.github.mcagentsmods.core.api.llm.LlmCredentials;
import io.github.mcagentsmods.core.api.llm.LlmVendor;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Which vendors are configured, and the client serving each.
 *
 * <p>This is the whole of the provider's state — and, deliberately, the whole
 * of the core's. No conversation, no cache, no per player anything is kept
 * anywhere in this module.</p>
 *
 * <p>Guarded by an intrinsic lock rather than a concurrent map. The operations
 * are not just reads and writes: registering has to close whatever it replaced,
 * and closing has to close every client exactly once. Those are compound
 * actions, and a concurrent map would make them look safe without making them
 * so. Contention is a non issue — registration happens at startup, and lookups
 * are a map read.</p>
 *
 * <p>Package-private on purpose: the registry is an implementation detail of
 * {@link MCAgentsProvider}.</p>
 */
final class ClientRegistry implements AutoCloseable {

    /**
     * The client serving each registered vendor.
     *
     * <p>An {@link EnumMap} because the key space is a small enum — it is
     * faster and smaller than a hash map, and it iterates in constant order,
     * which keeps {@link #vendors()} stable between calls.</p>
     */
    private final Map<LlmVendor, LlmClient> clients = new EnumMap<>(LlmVendor.class);

    /**
     * Whether {@link #close()} has run.
     *
     * <p>Atomic so a caller can check it without taking the lock, which is what
     * makes the closed check on the read path free.</p>
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Registers a vendor, replacing and closing any client already serving it.
     *
     * @param credentials The key, endpoint, and timeout to reach the vendor
     *                    with.
     * @throws NullPointerException When {@code credentials} is {@code null}.
     * @throws AgentException When the registry has been closed.
     */
    void register(LlmCredentials credentials) {
        Objects.requireNonNull(credentials, "credentials cannot be null");
        LlmVendor vendor = credentials.vendor();

        LlmClient replaced;
        synchronized (this) {
            ensureOpen(vendor);
            replaced = clients.put(vendor, LlmClientFactory.create(credentials));
        }

        // Closed outside the lock: shutting a client down is the slowest thing
        // here, and nothing else needs to wait behind it.
        if (replaced != null) {
            replaced.close();
        }
    }

    /**
     * Removes a vendor and closes its client.
     *
     * @param vendor The vendor to remove.
     * @return {@code true} when a client was registered and has now been
     *         closed.
     */
    boolean unregister(LlmVendor vendor) {
        LlmClient removed;
        synchronized (this) {
            removed = clients.remove(vendor);
        }
        if (removed == null) {
            return false;
        }
        removed.close();
        return true;
    }

    /**
     * Reports whether a vendor can currently be called.
     *
     * @param vendor The vendor to check.
     * @return {@code true} when a client is registered for it.
     */
    synchronized boolean isRegistered(LlmVendor vendor) {
        return clients.containsKey(vendor);
    }

    /**
     * Lists the registered vendors.
     *
     * @return An unmodifiable snapshot taken under the lock, so it is never a
     *         half updated view. Later registrations do not appear in it.
     */
    synchronized Set<LlmVendor> vendors() {
        return Set.copyOf(clients.keySet());
    }

    /**
     * Returns the client serving a vendor, or explains why there is none.
     *
     * @param vendor The vendor to look up.
     * @return The client serving it.
     * @throws NullPointerException When {@code vendor} is {@code null}.
     * @throws AgentException When the registry is closed, or nothing is
     *                        registered for that vendor.
     */
    synchronized LlmClient require(LlmVendor vendor) {
        Objects.requireNonNull(vendor, "vendor cannot be null");
        ensureOpen(vendor);

        LlmClient client = clients.get(vendor);
        if (client == null) {
            throw new AgentException(vendor, "No credentials registered for " + vendor.code()
                    + ". Register it before calling it.");
        }
        return client;
    }

    /**
     * Fails when the registry has been closed.
     *
     * @param vendor The vendor the failing call was aimed at, for the
     *               exception.
     * @throws AgentException When {@link #close()} has already run.
     */
    private void ensureOpen(LlmVendor vendor) {
        if (closed.get()) {
            throw new AgentException(vendor, "This provider has been closed");
        }
    }

    /**
     * Closes every registered client and clears the registry.
     *
     * <p>Idempotent. A registry is not reusable afterwards: every later call
     * fails with an {@link AgentException}.</p>
     */
    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        Map<LlmVendor, LlmClient> closing;
        synchronized (this) {
            closing = new EnumMap<>(clients);
            clients.clear();
        }
        // Outside the lock, and each independently: one client failing to shut
        // down must not leave the others open.
        closing.values().forEach(LlmClient::close);
    }
}
