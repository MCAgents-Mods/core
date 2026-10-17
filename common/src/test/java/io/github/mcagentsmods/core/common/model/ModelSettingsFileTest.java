package io.github.mcagentsmods.core.common.model;

import io.github.mcagentsmods.core.api.model.GlobalModels;
import io.github.mcagentsmods.core.api.model.ModelChoice;
import io.github.mcagentsmods.core.api.model.ModelProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The global model's file: a round trip, a missing file, and a hand-mangled one. */
class ModelSettingsFileTest {

    @TempDir
    Path directory;

    @Test
    void readsBackWhatItWrote() throws IOException {
        GlobalModels written = new GlobalModels(
                new ModelChoice("deepseek", "deepseek/deepseek-v4-flash"),
                new ModelChoice("homelab", "homelab/nomic-embed"),
                List.of(new ModelProvider("homelab", "http://127.0.0.1:8080/v1"),
                        new ModelProvider("deepseek", "https://eu.deepseek.example/v1")));
        Path file = directory.resolve("agents/models.properties");

        ModelSettingsFile.save(written, file);

        assertEquals(Optional.of(written).map(GlobalModels::chat),
                ModelSettingsFile.load(file).map(GlobalModels::chat));
        GlobalModels read = ModelSettingsFile.load(file).orElseThrow();
        assertEquals(written.embedding(), read.embedding());
        assertEquals(2, read.customProviders().size());
        assertEquals("http://127.0.0.1:8080/v1", read.provider("homelab").orElseThrow().baseUrl());
        assertFalse(Files.exists(file.resolveSibling("models.properties.tmp")));
    }

    @Test
    void aMissingFileIsNotYetSaved() {
        assertTrue(ModelSettingsFile.load(directory.resolve("nothing.properties")).isEmpty());
    }

    @Test
    void skipsWhatItCannotUseAndKeepsTheRest() throws IOException {
        Path file = directory.resolve("models.properties");
        Files.writeString(file, String.join("\n",
                "chat.provider=DeepSeek",
                "chat.model=deepseek-v4-flash",
                "embedding.model=",
                "provider.homelab=http://example.com/v1",
                "provider.bad/id=https://example.com/v1",
                "provider.local=http://localhost:1234/v1",
                "nonsense"), StandardCharsets.UTF_8);

        GlobalModels read = ModelSettingsFile.load(file).orElseThrow();

        // A bare id at a direct provider is stored qualified, as the naming rule wants.
        assertEquals(new ModelChoice("deepseek", "deepseek/deepseek-v4-flash"), read.chat());
        assertEquals(GlobalModels.DEFAULTS.embedding(), read.embedding());
        assertEquals(List.of("local"), read.customProviders().stream().map(ModelProvider::id).toList());
    }

    @Test
    void theAccessPointFallsBackPerTaskAndStartsOnce() {
        ModelChoice own = new ModelChoice("openai", "openai/gpt-4o-mini");
        assertEquals(own, Models.chat(own));
        assertEquals(Models.global().embedding(), Models.embedding(null));
    }
}
