package io.github.mcagentsmods.core.common.web;

import io.github.mcagentsmods.core.api.web.WebConsole;
import io.github.mcagentsmods.core.api.web.WebModule;
import io.github.mcagentsmods.core.api.web.WebRegistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The {@link WebRegistry} every MCAgents mod registers into.
 *
 * <p>{@link #shared()} is the one instance the game uses: core's jar is loaded once, so every
 * mod that calls it reaches the same registry and the same console. It holds the modules
 * mods registered and, while the player has it open, one running console — nothing about the
 * player, and nothing that grows with use.
 *
 * <p>Every method is safe to call from any thread. A module's own methods are never called
 * while this registry's lock is held, so a slow module cannot stall a registration.
 */
public final class CoreWebRegistry implements WebRegistry, AutoCloseable {

    private static final CoreWebRegistry SHARED = new CoreWebRegistry();

    private final ShellModule shell = new ShellModule(this);

    /** Guarded by {@code this}. Insertion order is the header's order. */
    private final Map<String, WebModule> modules = new LinkedHashMap<>();

    /** Guarded by {@code this}. */
    private LocalWebHost running;

    /** A registry of its own, for a test or a tool. The game uses {@link #shared()}. */
    public CoreWebRegistry() {
    }

    /** The registry every mod in this game registers into. */
    public static CoreWebRegistry shared() {
        return SHARED;
    }

    @Override
    public void register(WebModule module) {
        Objects.requireNonNull(module, "module");
        String id = module.id();
        if (!WebModule.isValidId(id)) {
            throw new IllegalArgumentException("'" + id + "' is not a usable web module id: it "
                    + "must be 1 to 32 of a-z, 0-9 and _, and not one of "
                    + WebModule.RESERVED_IDS);
        }
        String root = module.resourceRoot();
        if (root == null || root.startsWith("/") || !root.endsWith("/") || root.contains("..")) {
            throw new IllegalArgumentException(
                    "A resource root has no leading slash and ends with one, such as 'mod/web/'");
        }
        Objects.requireNonNull(module.resourceLoader(), "resourceLoader");
        synchronized (this) {
            if (modules.containsKey(id)) {
                throw new IllegalStateException("A web module '" + id + "' is already registered");
            }
            modules.put(id, module);
        }
    }

    @Override
    public synchronized boolean unregister(String id) {
        return modules.remove(id) != null;
    }

    @Override
    public synchronized List<String> moduleIds() {
        return List.copyOf(modules.keySet());
    }

    @Override
    public synchronized WebConsole open() {
        if (running != null && running.isRunning()) {
            return running;
        }
        running = LocalWebHost.start(this);
        return running;
    }

    @Override
    public synchronized Optional<WebConsole> current() {
        return running != null && running.isRunning() ? Optional.of(running) : Optional.empty();
    }

    /** Stops the running console, if any. Registrations are kept. Safe to call twice. */
    @Override
    public void close() {
        LocalWebHost stopping;
        synchronized (this) {
            stopping = running;
            running = null;
        }
        if (stopping != null) {
            stopping.close();
        }
    }

    /** The module answering {@code id}, core's own shell included. */
    synchronized Optional<WebModule> module(String id) {
        if (ShellModule.ID.equals(id)) {
            return Optional.of(shell);
        }
        return Optional.ofNullable(modules.get(id));
    }

    /** Every registered module, without core's shell, in registration order. */
    synchronized List<WebModule> registeredModules() {
        return new ArrayList<>(modules.values());
    }
}
