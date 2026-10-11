package io.github.mcagentsmods.core.common.web;

import io.github.mcagentsmods.core.api.web.WebConsole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The console, driven the way a mod's pages drive it.
 *
 * <p>Most of this is the guard: a console that serves several mods' data to a browser has to
 * refuse everything that is not one of its own pages asking, and keep each mod inside its
 * own paths.
 */
class LocalWebHostTest {

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private CoreWebRegistry registry;
    private SampleModule sample;
    private WebConsole console;

    @BeforeEach
    void open() {
        registry = new CoreWebRegistry();
        sample = new SampleModule("sample");
        registry.register(sample);
        console = registry.open();
    }

    @AfterEach
    void close() {
        registry.close();
    }

    // ------------------------------------------------------------ the guard

    @Test
    void refusesEveryPageAndApiRequestWithoutTheToken() {
        assertEquals(403, get("/sample/home/").statusCode());
        assertEquals(403, get("/api/sample/echo").statusCode());
        assertEquals(403, get("/api/core/navigation").statusCode());
        assertEquals(403, get("/api/sample/echo?t=guessed").statusCode());
        assertEquals(403, get("/").statusCode());
    }

    @Test
    void acceptsTheTokenInAHeaderAsWellAsTheUrl() {
        assertEquals(200, get("/api/sample/echo?t=" + token()).statusCode());
        assertEquals(200, send(HttpRequest.newBuilder(base("/api/sample/echo"))
                .header(LocalWebHost.TOKEN_HEADER, token()).GET()).statusCode());
    }

    @Test
    void refusesARequestThatArrivedThroughSomeOtherHostname() {
        // A site pointing one of its own names at 127.0.0.1 reaches the console carrying
        // that name. Written onto a socket by hand because HttpClient forbids setting Host.
        String path = "/api/sample/echo?t=" + token();

        assertTrue(rawGet("console-stealer.example", path).startsWith("HTTP/1.1 403"));
        assertTrue(rawGet("localhost.console-stealer.example", path).startsWith("HTTP/1.1 403"));
        assertTrue(rawGet("127.0.0.1.nip.io:" + console.port(), path).startsWith("HTTP/1.1 403"));
        assertTrue(rawGet("127.0.0.1:" + console.port(), path).startsWith("HTTP/1.1 200"),
                "and the same request from the real origin still works");
    }

    @Test
    void readsOnlyTheLoopbackNamesAsLoopback() {
        assertTrue(LocalWebHost.isLoopbackHost("127.0.0.1:8080"));
        assertTrue(LocalWebHost.isLoopbackHost("LOCALHOST"));
        assertTrue(LocalWebHost.isLoopbackHost("[::1]:8080"));
        assertFalse(LocalWebHost.isLoopbackHost("[::1].evil.example"));
        assertFalse(LocalWebHost.isLoopbackHost("127.0.0.1:80:evil"));
        assertFalse(LocalWebHost.isLoopbackHost("localhost:"));
        assertFalse(LocalWebHost.isLoopbackHost(null));
    }

    @Test
    void givesEveryRunItsOwnToken() {
        String first = token();
        registry.close();
        console = registry.open();

        assertNotEquals(first, token());
    }

    @Test
    void letsNoOtherOriginReadAnAnswer() {
        HttpResponse<String> response = get("/api/sample/echo?t=" + token());

        assertTrue(response.headers().firstValue("Access-Control-Allow-Origin").isEmpty());
        String policy = response.headers().firstValue("Content-Security-Policy").orElseThrow();
        assertTrue(policy.contains("script-src 'self'"));
        assertFalse(policy.contains("unsafe-eval"));
        assertTrue(policy.contains("frame-ancestors 'none'"));
        assertEquals("nosniff", response.headers().firstValue("X-Content-Type-Options").orElseThrow());
        assertEquals("no-store", response.headers().firstValue("Cache-Control").orElseThrow());
    }

    @Test
    void refusesABodyLargerThanItAcceptsRatherThanCuttingItShort() {
        String large = "x".repeat(LocalWebHost.MAX_BODY_BYTES + 1);

        assertEquals(413, post("/api/sample/echo", large).statusCode());
        assertNull(sample.lastRequest.get(), "the module never sees a truncated body");
        assertEquals(200, post("/api/sample/echo", "x".repeat(1000)).statusCode());
    }

    @Test
    void neverReadsResourcesOutsideAModulesRoot() {
        assertEquals(404, get("/assets/sample/../../io/github/mcagentsmods/core/common/web/"
                + "CoreWebRegistry.class").statusCode());
        assertEquals(404, get("/assets/sample/%2e%2e/testweb/css/sample.css").statusCode());
        assertEquals(404, get("/assets/sample/css//sample.css").statusCode());
        assertEquals(404, get("/assets/sample/SampleModule.class").statusCode());
    }

    // ------------------------------------------------------------ routing

    @Test
    void servesAModulesPagesUnderItsOwnId() {
        HttpResponse<String> page = get("/sample/home/?t=" + token());

        assertEquals(200, page.statusCode());
        assertTrue(page.body().contains("sample page"));
        assertTrue(page.headers().firstValue("Content-Type").orElseThrow().startsWith("text/html"));
        assertEquals(404, get("/sample/other/?t=" + token()).statusCode(),
                "a link the module lists but does not serve is still a 404, not someone else's");
        assertEquals(404, get("/nobody/home/?t=" + token()).statusCode());
    }

    @Test
    void servesAssetsWithoutTheTokenBecauseATagCannotCarryOne() {
        assertEquals(200, get("/assets/sample/css/sample.css").statusCode());
        assertEquals(200, get("/assets/core/css/main.css").statusCode());
        assertEquals(200, get("/assets/core/js/site.js").statusCode());
        assertEquals(200, get("/assets/core/partials/header.html").statusCode());
        // Only reading is open. Anything else needs the token like any other request, and
        // even with it an asset cannot be written to.
        assertEquals(403, send(HttpRequest.newBuilder(base("/assets/core/css/main.css"))
                .POST(HttpRequest.BodyPublishers.noBody())).statusCode());
        assertEquals(405, post("/assets/core/css/main.css", "").statusCode());
    }

    @Test
    void passesTheModuleItsOwnPathAndNotTheToken() {
        HttpResponse<String> response = get("/api/sample/echo?q=a%20b&t=" + token());

        assertEquals("{\"q\":\"a b\"}", response.body());
        assertEquals("echo", sample.lastRequest.get().path());
        assertFalse(sample.lastRequest.get().query().containsKey("t"),
                "a module never needs the token, so it never gets it");
    }

    @Test
    void sendsAModulesRefusalBackAsItsOwnWords() {
        HttpResponse<String> refused = post("/api/sample/refuse", "{}");

        assertEquals(400, refused.statusCode());
        assertEquals("{\"error\":\"That name is already taken.\"}", refused.body());
    }

    @Test
    void neverSendsAnExceptionsOwnTextToTheBrowser() {
        HttpResponse<String> failed = get("/api/sample/explode?t=" + token());

        assertEquals(500, failed.statusCode());
        assertFalse(failed.body().contains("secret"));
    }

    @Test
    void sendsTheRootOnToTheFrontPageCarryingTheToken() {
        HttpResponse<String> root = get("/?t=" + token());

        assertEquals(303, root.statusCode());
        assertEquals("/core/?t=" + token(), root.headers().firstValue("Location").orElseThrow());
        assertEquals(200, get("/core/?t=" + token()).statusCode());
    }

    @Test
    void listsEveryModuleForTheHeaderWithItsTitleEscaped() {
        registry.register(new SampleModule("second"));

        String navigation = get("/api/core/navigation?t=" + token()).body();

        assertTrue(navigation.contains("\"id\":\"sample\""));
        assertTrue(navigation.contains("\"id\":\"second\""));
        assertTrue(navigation.contains("\"href\":\"/sample/home/\""));
        assertTrue(navigation.contains("\"section\":\"home\""));
        assertFalse(navigation.contains("<b>"), "a title cannot carry markup into the page");
    }

    @Test
    void stopsAnsweringAModuleThatUnregistered() {
        assertTrue(registry.unregister("sample"));

        assertEquals(404, get("/sample/home/?t=" + token()).statusCode());
        assertEquals(404, get("/api/sample/echo?t=" + token()).statusCode());
    }

    @Test
    void pointsTheBrowserAtAModulesFirstPage() {
        assertEquals("/sample/home/", console.url("sample").getPath());
        assertEquals("/core/", console.url().getPath());
        assertTrue(console.url().getQuery().startsWith("t="));
    }

    // ------------------------------------------------------------ lifecycle

    @Test
    void keepsTheSameConsoleWhileItRunsAndStartsANewOneAfterItCloses() {
        assertEquals(console, registry.open());

        console.close();
        assertTrue(registry.current().isEmpty());
        assertDoesNotThrow(() -> console.stopped().get(5, TimeUnit.SECONDS));

        WebConsole next = registry.open();
        assertNotEquals(console.port(), -1);
        assertTrue(next.isRunning());
    }

    @Test
    void closingTwiceIsHarmless() {
        console.close();
        console.close();
        registry.close();

        assertFalse(console.isRunning());
    }

    // ------------------------------------------------------------ plumbing

    private String token() {
        return registry.current().orElseThrow().url().getQuery().substring("t=".length());
    }

    private URI base(String path) {
        return URI.create("http://127.0.0.1:" + registry.current().orElseThrow().port() + path);
    }

    private HttpResponse<String> get(String path) {
        return send(HttpRequest.newBuilder(base(path)).GET());
    }

    private HttpResponse<String> post(String path, String body) {
        return send(HttpRequest.newBuilder(base(path))
                .header(LocalWebHost.TOKEN_HEADER, token())
                .POST(HttpRequest.BodyPublishers.ofString(body)));
    }

    private HttpResponse<String> send(HttpRequest.Builder request) {
        return assertDoesNotThrow(() -> client.send(request.timeout(Duration.ofSeconds(10)).build(),
                HttpResponse.BodyHandlers.ofString()));
    }

    private String rawGet(String host, String path) {
        try (Socket socket = new Socket("127.0.0.1", console.port())) {
            socket.setSoTimeout(5000);
            OutputStream out = socket.getOutputStream();
            out.write(("GET " + path + " HTTP/1.1\r\nHost: " + host
                    + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            out.flush();
            InputStream in = socket.getInputStream();
            return new String(in.readAllBytes(), StandardCharsets.US_ASCII);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
