package io.github.mcagentsmods.core.api.web;

import java.util.List;
import java.util.Optional;

/**
 * Where mods put their pages, and how the console that serves them is opened.
 *
 * <p>Registration and the console are independent. A mod may register at any point —
 * before the console has ever been opened, or while it is running, in which case its pages
 * appear on the next request. Nothing is listening until something calls {@link #open()}.
 *
 * <p>Every method may be called from any thread.
 */
public interface WebRegistry {

    /**
     * Adds a module.
     *
     * @throws IllegalArgumentException if its id is invalid or reserved — see
     *                                  {@link WebModule#isValidId(String)}
     * @throws IllegalStateException    if a module with that id is already registered; one
     *                                  mod cannot replace another's pages
     */
    void register(WebModule module);

    /**
     * Removes a module. Its pages answer 404 from the next request.
     *
     * @return whether a module with that id was registered
     */
    boolean unregister(String id);

    /** The ids of every registered module, in registration order. */
    List<String> moduleIds();

    /**
     * The running console, starting one if none is running.
     *
     * @throws WebConsoleException if no loopback port can be opened
     */
    WebConsole open();

    /** The running console, if one is. */
    Optional<WebConsole> current();
}
