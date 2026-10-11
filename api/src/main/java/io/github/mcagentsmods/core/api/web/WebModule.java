package io.github.mcagentsmods.core.api.web;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * One mod's pages in the local web console.
 *
 * <p>A mod registers a module with the {@link WebRegistry} and gets three namespaces that no
 * other module can reach into:
 *
 * <table border="1">
 *   <caption>What a module with id {@code chat} owns</caption>
 *   <tr><th>Path</th><th>Answered by</th><th>Token</th></tr>
 *   <tr><td>{@code /chat/…}</td><td>{@link #page(String)}</td><td>required</td></tr>
 *   <tr><td>{@code /assets/chat/…}</td><td>a resource under {@link #resourceRoot()}</td>
 *       <td>not required: a {@code <link>} or {@code <script>} tag cannot carry one</td></tr>
 *   <tr><td>{@code /api/chat/…}</td><td>{@link #handle(WebRequest)}</td><td>required</td></tr>
 * </table>
 *
 * <p>Core serves the shell every page shares — {@code /assets/core/css/main.css},
 * {@code layout.css}, {@code components.css} and {@code /assets/core/js/site.js} — and
 * builds the header's navigation from every module's {@link #navigation()}.
 *
 * <p><strong>Nothing under {@link #resourceRoot()} may hold player data.</strong> Assets
 * are served without the token, so they are readable by anything on the machine that can
 * open a socket. Player data belongs behind {@code /api/}.
 *
 * <p>{@link #handle(WebRequest)} runs on one of the console's worker threads, never on the
 * game thread, and may block for as long as it needs to.
 */
public interface WebModule {

    /** What a module id may be. */
    Pattern ID = Pattern.compile("[a-z0-9_]{1,32}");

    /** Ids core keeps for itself, so no module can shadow the shell or the routing. */
    List<String> RESERVED_IDS = List.of("core", "api", "assets");

    /**
     * The module's id: 1 to 32 characters of lowercase letters, digits and underscores, and
     * not one of {@link #RESERVED_IDS}. Usually the mod id.
     */
    String id();

    /** The name a player reads, on the console's front page. */
    String title();

    /** The header's links to this module's pages, in the order they are shown. */
    List<NavLink> navigation();

    /**
     * Which page of this module a request for {@code path} is.
     *
     * @param path everything after {@code /{id}/}, without a leading slash and without the
     *             query string; empty for the module's root
     * @return a resource name relative to {@link #resourceRoot()}, such as
     *         {@code pages/chat.html}, or empty for a 404
     */
    Optional<String> page(String path);

    /**
     * Where this module's pages and assets live on the classpath, such as
     * {@code mcagents/web/}. No leading slash; a trailing one.
     */
    String resourceRoot();

    /**
     * The class loader the module's resources are read through.
     *
     * <p>Each mod's own, by default, so a loader that gives every mod its own module layer —
     * NeoForge does — still finds them. Override only to read from somewhere else.
     */
    default ClassLoader resourceLoader() {
        return getClass().getClassLoader();
    }

    /**
     * Answers a request under {@code /api/{id}/}.
     *
     * <p>Throw {@link IllegalArgumentException} to refuse input: its message is sent back
     * as a 400, so write it for the player. Any other exception is logged and answered with
     * a 500 that names nothing.
     *
     * @param request the request, with its path relative to the module's API root
     * @return the answer; never {@code null}
     */
    default WebResponse handle(WebRequest request) {
        return WebResponse.error(404, "No such endpoint.");
    }

    /**
     * Whether {@code id} could be registered at all.
     *
     * @param id the candidate id
     * @return {@code true} when it matches {@link #ID} and is not reserved
     */
    static boolean isValidId(String id) {
        return id != null && ID.matcher(id).matches() && !RESERVED_IDS.contains(id);
    }
}
