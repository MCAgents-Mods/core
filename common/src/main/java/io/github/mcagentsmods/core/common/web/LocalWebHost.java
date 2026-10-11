package io.github.mcagentsmods.core.common.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.mcagentsmods.core.api.web.WebConsole;
import io.github.mcagentsmods.core.api.web.WebConsoleException;
import io.github.mcagentsmods.core.api.web.WebModule;
import io.github.mcagentsmods.core.api.web.WebRequest;
import io.github.mcagentsmods.core.api.web.WebResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * One run of the console: a loopback HTTP server routing to every registered module.
 *
 * <h2>Routing</h2>
 *
 * <table border="1">
 *   <caption>Every path the console answers</caption>
 *   <tr><th>Path</th><th>Answer</th><th>Token</th></tr>
 *   <tr><td>{@code /}</td><td>a redirect to {@code /core/}, token attached</td><td>yes</td></tr>
 *   <tr><td>{@code /{id}/…}</td><td>the module's page, {@code GET} only</td><td>yes</td></tr>
 *   <tr><td>{@code /api/{id}/…}</td><td>the module's handler</td><td>yes</td></tr>
 *   <tr><td>{@code /assets/{id}/…}</td><td>a resource of that module's, {@code GET}
 *       only</td><td>no</td></tr>
 * </table>
 *
 * <p>Assets need no token because a {@code <link>} or {@code <script>} tag cannot carry
 * one. Nothing is given away by it: an asset is a file shipped inside a mod jar, identical
 * for every player, and {@link WebModule} forbids putting player data there.
 *
 * <h2>Threads</h2>
 *
 * <p>Requests are served on a small bounded pool of daemon threads rather than one, because
 * a module may take a minute to answer — asking a model a question, say — and the player
 * will keep browsing while they wait. Bounded, because this runs inside a game and an
 * unbounded pool answering a page that opened twenty requests is memory the game does not
 * have. The queue in front of the pool is bounded too: anything on the machine can reach the
 * port, and a fixed pool's unbounded queue would hold every connection it opened. A request
 * that finds the queue full has its connection closed. Daemon, so the game can exit with a
 * page half loaded.
 */
final class LocalWebHost implements WebConsole {

    private static final Logger LOGGER = System.getLogger(LocalWebHost.class.getName());

    private static final int TOKEN_BYTES = 32;

    /**
     * The largest request body accepted, in bytes.
     *
     * <p>Bounded because this listens on a socket anything on the machine can open, and an
     * unbounded read there is a way to make the game run out of memory. A larger body is
     * refused with a 413 rather than cut short: a save that was silently truncated would be
     * the worst possible failure for a file somebody wrote by hand.
     */
    static final int MAX_BODY_BYTES = 256 * 1024;

    /** Enough to answer a page's parallel fetches without letting the pool grow unbounded. */
    private static final int WORKER_THREADS = 4;

    /** Requests that may wait for a worker before more are refused. */
    private static final int QUEUED_REQUESTS = 64;

    /** Seconds {@link #close()} allows a request already in flight to finish. */
    private static final int STOP_GRACE_SECONDS = 2;

    static final String TOKEN_HEADER = "X-MCAgents-Token";

    /**
     * Scripts, styles, images and requests from the console itself, and nothing else.
     *
     * <p>{@code script-src 'self'} with no {@code 'unsafe-inline'} is the line that matters:
     * a page that ever renders stored text as markup by mistake still cannot run it. Inline
     * {@code style} attributes are allowed, because they cannot run code. Framing is refused,
     * so no other page can lay the console under a click.
     */
    static final String CONTENT_SECURITY_POLICY = "default-src 'none'; script-src 'self'; "
            + "style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; "
            + "connect-src 'self'; form-action 'self'; base-uri 'none'; frame-ancestors 'none'";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final HttpServer server;
    private final ExecutorService workers;
    private final CoreWebRegistry registry;
    private final String token;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final CompletableFuture<Void> stopped = new CompletableFuture<>();

    private LocalWebHost(HttpServer server, ExecutorService workers, CoreWebRegistry registry,
                         String token) {
        this.server = server;
        this.workers = workers;
        this.registry = registry;
        this.token = token;
    }

    /**
     * Opens a port and starts serving.
     *
     * @throws WebConsoleException if no loopback port can be opened
     */
    static LocalWebHost start(CoreWebRegistry registry) {
        Objects.requireNonNull(registry, "registry");
        HttpServer server;
        try {
            server = HttpServer.create(
                    new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        } catch (IOException e) {
            throw new WebConsoleException("Could not open a local port for the web console", e);
        }

        // A full queue refuses the request; the server closes that connection and keeps
        // serving the rest.
        ExecutorService workers = new ThreadPoolExecutor(WORKER_THREADS, WORKER_THREADS,
                0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(QUEUED_REQUESTS), runnable -> {
                    Thread thread = new Thread(runnable, "MCAgents-web-console");
                    thread.setDaemon(true);
                    return thread;
                }, new ThreadPoolExecutor.AbortPolicy());
        server.setExecutor(workers);

        LocalWebHost host = new LocalWebHost(server, workers, registry, randomHex());
        server.createContext("/", host::route);
        server.start();

        LOGGER.log(Level.INFO, "Web console listening on 127.0.0.1:{0}",
                String.valueOf(host.port()));
        return host;
    }

    @Override
    public URI url() {
        return address("/core/");
    }

    @Override
    public URI url(String moduleId) {
        WebModule module = registry.module(moduleId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No web module '" + moduleId + "' is registered"));
        return address(ShellModule.home(module));
    }

    private URI address(String path) {
        return URI.create("http://127.0.0.1:" + port() + path + "?t=" + token);
    }

    @Override
    public int port() {
        return server.getAddress().getPort();
    }

    @Override
    public boolean isRunning() {
        return !closed.get();
    }

    @Override
    public CompletableFuture<Void> stopped() {
        return stopped;
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        // A short grace period rather than zero: a page mid-load should finish rather than
        // show the player a half-rendered console as its parting image.
        server.stop(STOP_GRACE_SECONDS);
        workers.shutdownNow();
        stopped.complete(null);
        LOGGER.log(Level.INFO, "Web console stopped");
    }

    // ------------------------------------------------------------- routing

    private void route(HttpExchange exchange) throws IOException {
        try {
            if (!guard(exchange)) {
                return;
            }
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);

            if (path.equals("/") || path.isEmpty()) {
                redirect(exchange, "/core/?t=" + token);
                return;
            }
            if (path.startsWith("/api/")) {
                handleApi(exchange, method, path.substring("/api/".length()));
                return;
            }
            if (!method.equals("GET")) {
                sendError(exchange, 405, "Method not allowed.");
                return;
            }
            if (path.startsWith("/assets/")) {
                handleAsset(exchange, path.substring("/assets/".length()));
                return;
            }
            handlePage(exchange, path.substring(1));
        } catch (BodyTooLargeException e) {
            sendError(exchange, 413, "That is more than the console accepts in one request.");
        } catch (IllegalArgumentException e) {
            // A module refusing input. Its message is written for the player, so it goes
            // back as it stands.
            sendError(exchange, 400, e.getMessage());
        } catch (RuntimeException e) {
            // Never the exception's own text: it can name a file path or a query. The detail
            // belongs in the log, where the player can find it and nobody else can.
            LOGGER.log(Level.ERROR, "The web console failed to answer a request", e);
            sendError(exchange, 500, "Something went wrong. The reason is in the game's log.");
        }
    }

    private void handleApi(HttpExchange exchange, String method, String rest) throws IOException {
        int slash = rest.indexOf('/');
        String id = slash < 0 ? rest : rest.substring(0, slash);
        Optional<WebModule> module = registry.module(id);
        if (module.isEmpty()) {
            sendError(exchange, 404, "No such endpoint.");
            return;
        }
        Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
        query.remove("t");
        WebRequest request = new WebRequest(method, slash < 0 ? "" : rest.substring(slash + 1),
                query, readBody(exchange));
        WebResponse response = module.get().handle(request);
        if (response == null) {
            throw new IllegalStateException("Web module '" + id + "' answered with nothing");
        }
        send(exchange, response.status(), response.contentType(), response.body());
    }

    private void handleAsset(HttpExchange exchange, String rest) throws IOException {
        int slash = rest.indexOf('/');
        Optional<WebModule> module = slash < 0 ? Optional.empty()
                : registry.module(rest.substring(0, slash));
        if (module.isEmpty()) {
            sendError(exchange, 404, "No such file.");
            return;
        }
        serveResource(exchange, module.get(), rest.substring(slash + 1), "No such file.");
    }

    private void handlePage(HttpExchange exchange, String rest) throws IOException {
        int slash = rest.indexOf('/');
        String id = slash < 0 ? rest : rest.substring(0, slash);
        Optional<WebModule> module = registry.module(id);
        if (module.isEmpty()) {
            sendError(exchange, 404, "No such page.");
            return;
        }
        if (slash < 0) {
            // /chat → /chat/, so the page's own relative links resolve the same either way.
            redirect(exchange, "/" + id + "/?t=" + token);
            return;
        }
        Optional<String> resource = module.get().page(rest.substring(slash + 1));
        if (resource.isEmpty()) {
            sendError(exchange, 404, "No such page.");
            return;
        }
        serveResource(exchange, module.get(), resource.get(), "No such page.");
    }

    private static void serveResource(HttpExchange exchange, WebModule module, String path,
                                      String missing) throws IOException {
        Optional<byte[]> body = WebResources.read(module.resourceLoader(),
                module.resourceRoot(), path);
        if (body.isEmpty()) {
            sendError(exchange, 404, missing);
            return;
        }
        send(exchange, 200, WebResources.contentType(path), body.get());
    }

    // ------------------------------------------------------------ security

    /**
     * Rejects anything that is not one of this console's own pages asking.
     *
     * <p>The {@code Host} check is what closes DNS rebinding: a hostile site that resolves
     * one of its own names to {@code 127.0.0.1} arrives carrying that name, and the token it
     * cannot read is not the only thing standing in its way.
     */
    private boolean guard(HttpExchange exchange) throws IOException {
        if (!isLoopbackHost(exchange.getRequestHeaders().getFirst("Host"))) {
            sendError(exchange, 403, "Forbidden.");
            return false;
        }
        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())
                && exchange.getRequestURI().getPath().startsWith("/assets/")) {
            return true;
        }
        String presented = exchange.getRequestHeaders().getFirst(TOKEN_HEADER);
        if (presented == null) {
            presented = parseQuery(exchange.getRequestURI().getRawQuery()).get("t");
        }
        if (!tokenMatches(presented)) {
            sendError(exchange, 403, "Forbidden.");
            return false;
        }
        return true;
    }

    static boolean isLoopbackHost(String host) {
        if (host == null) {
            return false;
        }
        String name = host.trim().toLowerCase(Locale.ROOT);
        if (name.startsWith("[")) {
            int close = name.indexOf(']');
            if (close < 0) {
                return false;
            }
            String rest = name.substring(close + 1);
            if (!rest.isEmpty() && !isPortSuffix(rest)) {
                return false;
            }
            return name.substring(0, close + 1).equals("[::1]");
        }
        int colon = name.indexOf(':');
        if (colon >= 0) {
            if (!isPortSuffix(name.substring(colon))) {
                return false;
            }
            name = name.substring(0, colon);
        }
        return name.equals("127.0.0.1") || name.equals("localhost");
    }

    private static boolean isPortSuffix(String suffix) {
        if (suffix.length() < 2 || suffix.length() > 6 || suffix.charAt(0) != ':') {
            return false;
        }
        for (int i = 1; i < suffix.length(); i++) {
            if (!Character.isDigit(suffix.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /** Constant time, so the token cannot be recovered a byte at a time. */
    private boolean tokenMatches(String candidate) {
        return candidate != null && MessageDigest.isEqual(
                candidate.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------ plumbing

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> values = new HashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return values;
        }
        for (String pair : rawQuery.split("&")) {
            int equals = pair.indexOf('=');
            if (equals > 0) {
                values.put(decode(pair.substring(0, equals)), decode(pair.substring(equals + 1)));
            } else if (!pair.isEmpty()) {
                values.put(decode(pair), "");
            }
        }
        return values;
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            // A malformed escape is not worth a 500; it simply matches nothing.
            return value;
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream stream = exchange.getRequestBody()) {
            byte[] body = stream.readNBytes(MAX_BODY_BYTES + 1);
            if (body.length > MAX_BODY_BYTES) {
                throw new BodyTooLargeException();
            }
            return new String(body, StandardCharsets.UTF_8);
        }
    }

    private static void redirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().add("Location", location);
        send(exchange, 303, "text/plain; charset=utf-8", new byte[0]);
    }

    private static void sendError(HttpExchange exchange, int status, String reason)
            throws IOException {
        WebResponse error = WebResponse.error(status, reason);
        send(exchange, status, error.contentType(), error.body());
    }

    private static void send(HttpExchange exchange, int status, String contentType, byte[] body)
            throws IOException {
        var headers = exchange.getResponseHeaders();
        headers.set("Content-Type", contentType);
        // The console shows live data and its address carries a token; neither should be
        // kept by a cache or handed to anything a page links to.
        headers.set("Cache-Control", "no-store");
        headers.set("Referrer-Policy", "no-referrer");
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("X-Frame-Options", "DENY");
        headers.set("Cross-Origin-Resource-Policy", "same-origin");
        headers.set("Content-Security-Policy", CONTENT_SECURITY_POLICY);
        // No Access-Control-Allow-Origin, deliberately: without it a browser refuses to let
        // another origin read anything this console says.
        exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
        if (body.length > 0) {
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        }
        exchange.close();
    }

    private static String randomHex() {
        byte[] value = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(value);
        return HexFormat.of().formatHex(value);
    }

    /** A request body over {@link #MAX_BODY_BYTES}. */
    private static final class BodyTooLargeException extends RuntimeException {
        BodyTooLargeException() {
            super(null, null, false, false);
        }
    }
}
