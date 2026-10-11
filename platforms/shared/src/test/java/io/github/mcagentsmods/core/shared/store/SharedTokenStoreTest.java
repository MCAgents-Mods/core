package io.github.mcagentsmods.core.shared.store;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The player's key file: what is kept, what is refused, and what is never written over. */
class SharedTokenStoreTest {

    @TempDir
    Path directory;

    @Test
    void neverWritesOverAFileItCouldNotRead() throws IOException {
        Path file = directory.resolve("mcagents.json");
        String broken = "{ \"openrouter\": { \"token\": [\"sk-or-v1-kept\", ] ";
        Files.writeString(file, broken, StandardCharsets.UTF_8);

        SharedTokenStore store = SharedTokenStore.atFile(file, quietLogger(new ArrayList<>()));

        assertFalse(store.add("openrouter", "sk-or-v1-another"));
        store.evict("openrouter", "sk-or-v1-kept");
        assertEquals(broken, Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void neverLogsWhatTheParserChokedOn() throws IOException {
        Path file = directory.resolve("mcagents.json");
        // Gson reads leniently, so only an unfinished document fails.
        Files.writeString(file, "{ \"openrouter\": { \"token\": [\"sk-or-v1-unquoted\"", StandardCharsets.UTF_8);
        List<String> logged = new ArrayList<>();

        SharedTokenStore.atFile(file, quietLogger(logged));

        assertFalse(logged.isEmpty());
        logged.forEach(line -> assertFalse(line.contains("unquoted"), line));
    }

    @Test
    void skipsAnEntryThatCouldNotBeSentAndSaysSoOnce() throws IOException {
        Path file = directory.resolve("mcagents.json");
        Files.writeString(file, """
                {"openrouter": {"token": ["sk-or-v1-good", "sk-or-v1 pasted with a space"]}}
                """, StandardCharsets.UTF_8);
        List<String> logged = new ArrayList<>();
        SharedTokenStore store = SharedTokenStore.atFile(file, quietLogger(logged));

        assertEquals(List.of("sk-or-v1-good"), store.load("openrouter"));
        store.load("openrouter");
        assertEquals(1, logged.stream().filter(line -> line.contains("not a usable token")).count());
        logged.forEach(line -> assertFalse(line.contains("pasted"), line));
    }

    @Test
    void refusesToStoreAKeyThatCouldNotBeSent() {
        SharedTokenStore store = SharedTokenStore.atFile(directory.resolve("mcagents.json"),
                quietLogger(new ArrayList<>()));

        assertFalse(store.add("openrouter", "sk-or-v1-line\nX-Evil: 1"));
        assertTrue(store.add("openrouter", "  sk-or-v1-fine  "));
        assertEquals(List.of("sk-or-v1-fine"), store.load("openrouter"));
    }

    @Test
    void twoStoresOnOneFileKeepEachOthersKeys() {
        Path file = directory.resolve("mcagents.json");
        SharedTokenStore client = SharedTokenStore.atFile(file, quietLogger(new ArrayList<>()));
        SharedTokenStore server = SharedTokenStore.atFile(file, quietLogger(new ArrayList<>()));

        assertTrue(client.add("openrouter", "sk-or-v1-client"));
        assertTrue(server.add("openrouter", "sk-or-v1-server"));
        client.reload();

        assertEquals(List.of("sk-or-v1-client", "sk-or-v1-server"), client.load("openrouter"));
    }

    private static Logger quietLogger(List<String> lines) {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                lines.add(record.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        return logger;
    }
}
