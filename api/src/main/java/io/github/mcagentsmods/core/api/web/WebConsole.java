package io.github.mcagentsmods.core.api.web;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

/**
 * One run of the local web console: every registered mod's pages, in the player's own
 * browser, served from their own machine.
 *
 * <p>The console is a server the player opens on purpose and closes on purpose, and it stays
 * up in between, because a browser tab they are reading is worthless if the port closes
 * under it. A closed console is not reopened; {@link WebRegistry#open()} starts a new one,
 * on a new port, with a new token.
 *
 * <h2>What keeps it safe</h2>
 *
 * <ul>
 *   <li><strong>Loopback only</strong>, on a port the operating system picks. Nothing off
 *       the machine can reach it.</li>
 *   <li><strong>A one-time token</strong> of 256 bits, minted per run and required on every
 *       page and API request. Another program on the machine cannot guess it; another web
 *       page cannot read it.</li>
 *   <li><strong>A loopback {@code Host} header</strong>, which is what closes DNS
 *       rebinding.</li>
 *   <li><strong>No CORS headers</strong>, so another origin cannot read an answer even if
 *       it could send the request.</li>
 *   <li><strong>A content security policy</strong> that allows scripts, styles and requests
 *       from the console itself and nothing else, and forbids framing.</li>
 * </ul>
 */
public interface WebConsole extends AutoCloseable {

    /**
     * Where to send the browser: the console's front page, including the token.
     *
     * <p>Worth showing on screen as well as opening, because the browser that opens by
     * default is not always the one the player wants to read in.
     */
    URI url();

    /**
     * Where to send the browser for one module: the first page in its navigation, including
     * the token.
     *
     * @throws IllegalArgumentException if no module with that id is registered
     */
    URI url(String moduleId);

    /** The loopback port it is listening on. */
    int port();

    /** Whether it is still listening. */
    boolean isRunning();

    /**
     * Stops the server. Safe to call twice.
     *
     * <p>Requests already in flight are given a moment to finish, so a page that is
     * mid-load does not simply hang.
     */
    @Override
    void close();

    /**
     * Completes when the server has stopped, however it stopped.
     *
     * <p>Lets a screen show "running" or "stopped" without polling for it.
     */
    CompletableFuture<Void> stopped();
}
