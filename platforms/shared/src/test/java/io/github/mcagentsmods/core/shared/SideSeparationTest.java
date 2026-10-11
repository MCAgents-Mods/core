package io.github.mcagentsmods.core.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Keeps the two physical sides apart inside one module.
 *
 * <p>The client and the server half share {@code platforms:shared}, so the
 * compiler would let either import the other. Nothing may: a dedicated server
 * that links a client class fails with a {@code NoClassDefFoundError} at
 * whatever moment the code path is first reached, and a green build proves
 * nothing about it. {@link io.github.mcagentsmods.core.shared.environment.ModBootstrap}
 * reaches each half by name, never by import, and this test is what holds every
 * other class to the same rule.</p>
 */
@DisplayName("Side separation")
class SideSeparationTest {

    private static final String BASE = "io.github.mcagentsmods.core.shared";
    private static final String CLIENT = BASE + ".client";
    private static final String SERVER = BASE + ".server";

    @Test
    @DisplayName("the server half never imports the client half")
    void serverNeverImportsClient() throws IOException {
        assertNoImports("server", CLIENT);
    }

    @Test
    @DisplayName("the client half never imports the server half")
    void clientNeverImportsServer() throws IOException {
        assertNoImports("client", SERVER);
    }

    @Test
    @DisplayName("shared code imports neither half")
    void sharedImportsNeither() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (Path source : sources()) {
            String pkg = packageOf(source);
            if (pkg.startsWith(CLIENT) || pkg.startsWith(SERVER)) {
                continue;
            }
            offenders.addAll(importsOf(source, CLIENT));
            offenders.addAll(importsOf(source, SERVER));
        }
        assertTrue(offenders.isEmpty(), "shared code imports a side: " + offenders);
    }

    private static void assertNoImports(String side, String forbidden) throws IOException {
        List<String> offenders = new ArrayList<>();
        for (Path source : sources()) {
            if (packageOf(source).startsWith(BASE + "." + side)) {
                offenders.addAll(importsOf(source, forbidden));
            }
        }
        assertTrue(offenders.isEmpty(), side + " imports " + forbidden + ": " + offenders);
    }

    private static List<Path> sources() throws IOException {
        Path root = Path.of(System.getProperty("shared.sources", "src/main/java"));
        assertTrue(Files.isDirectory(root), "no sources at " + root.toAbsolutePath());
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(path -> path.toString().endsWith(".java")).toList();
        }
    }

    private static String packageOf(Path source) throws IOException {
        for (String line : Files.readAllLines(source)) {
            String trimmed = line.strip();
            if (trimmed.startsWith("package ")) {
                return trimmed.substring("package ".length(), trimmed.length() - 1);
            }
        }
        return "";
    }

    private static List<String> importsOf(Path source, String forbidden) throws IOException {
        List<String> found = new ArrayList<>();
        for (String line : Files.readAllLines(source)) {
            String trimmed = line.strip();
            if ((trimmed.startsWith("import " + forbidden + ".")
                    || trimmed.startsWith("import static " + forbidden + "."))) {
                found.add(source.getFileName() + ": " + trimmed);
            }
        }
        return found;
    }
}
