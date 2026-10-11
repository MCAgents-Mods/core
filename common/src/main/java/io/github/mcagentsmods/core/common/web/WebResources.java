package io.github.mcagentsmods.core.common.web;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Optional;

/**
 * Pages, stylesheets and scripts, read off the classpath.
 *
 * <p>Everything the console serves ships inside a mod jar. There is no directory on disk to
 * point at and nothing is fetched from anywhere, so a player with no internet gets the same
 * console as a player with one.
 */
final class WebResources {

    /** Longer than any real asset path, and short enough that a hostile one is cheap to refuse. */
    private static final int MAX_PATH_LENGTH = 200;

    private WebResources() {
        throw new AssertionError("No instances.");
    }

    /**
     * Reads one resource.
     *
     * @param loader the class loader of the mod that owns it
     * @param root   the mod's resource root, such as {@code mcagents/web/}
     * @param path   the path below the root, as it arrived in the URL
     * @return the bytes, or empty if there is no such resource or the path is not one this
     *         may serve
     */
    static Optional<byte[]> read(ClassLoader loader, String root, String path) {
        if (!isServable(path)) {
            return Optional.empty();
        }
        try (InputStream stream = loader.getResourceAsStream(root + path)) {
            return stream == null ? Optional.empty() : Optional.of(stream.readAllBytes());
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /**
     * Whether {@code path} is one a resource may be read by.
     *
     * <p>A whitelist of characters rather than a search for {@code ..}, because the ways of
     * writing {@code ..} are more numerous than the ways of listing what a file name may
     * contain. Nothing here is served from disk, so a traversal could at worst read another
     * resource out of a jar — but "at worst" is not a reason to allow it.
     */
    static boolean isServable(String path) {
        if (path == null || path.isEmpty() || path.length() > MAX_PATH_LENGTH) {
            return false;
        }
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            boolean allowed = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '/' || c == '-' || c == '_' || c == '.';
            if (!allowed) {
                return false;
            }
        }
        return !path.contains("..") && !path.contains("//") && !path.startsWith("/")
                && !path.endsWith("/") && !path.endsWith(".class");
    }

    /** The content type for a resource, by extension. */
    static String contentType(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (lower.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (lower.endsWith(".js")) {
            return "text/javascript; charset=utf-8";
        }
        if (lower.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (lower.endsWith(".json")) {
            return "application/json; charset=utf-8";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".woff2")) {
            return "font/woff2";
        }
        return "application/octet-stream";
    }
}
