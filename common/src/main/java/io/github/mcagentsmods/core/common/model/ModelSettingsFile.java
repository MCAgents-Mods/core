package io.github.mcagentsmods.core.common.model;

import io.github.mcagentsmods.core.api.model.GlobalModels;
import io.github.mcagentsmods.core.api.model.ModelChoice;
import io.github.mcagentsmods.core.api.model.ModelProvider;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeSet;

/**
 * The global model on disk: {@code {game_dir}/MCAgents/core/models.properties}.
 *
 * <pre>
 * chat.provider=deepseek
 * chat.model=deepseek/deepseek-v4-flash
 * embedding.provider=openrouter
 * embedding.model=openai/text-embedding-3-small
 * provider.homelab=http://127.0.0.1:8080/v1
 * </pre>
 *
 * <p>A file that is unreadable, or holds a value that is not usable, is not an error: that value
 * falls back to its default and an unusable provider line is skipped, so a hand-edited file can
 * never stop the mod. No API key is ever written here; keys stay in the store of the mod that
 * asked for them.
 */
final class ModelSettingsFile {

    static final String PROVIDER_PREFIX = "provider.";

    private ModelSettingsFile() {
        throw new AssertionError("No instances.");
    }

    /** The settings in {@code file}, or empty when the file does not exist yet. */
    static Optional<GlobalModels> load(Path file) {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (NoSuchFileException e) {
            return Optional.empty();
        } catch (IOException | IllegalArgumentException e) {
            // There but unreadable: the defaults answer, and the next save writes a good file.
            return Optional.of(GlobalModels.DEFAULTS);
        }
        return Optional.of(read(properties));
    }

    static GlobalModels read(Properties properties) {
        List<ModelProvider> providers = new ArrayList<>();
        // Sorted, because Properties has no order; the file is written in the order added,
        // and a hand-added line lands in name order, which is as good as any.
        for (String key : new TreeSet<>(properties.stringPropertyNames())) {
            if (key.startsWith(PROVIDER_PREFIX)) {
                try {
                    providers.add(new ModelProvider(key.substring(PROVIDER_PREFIX.length()),
                            properties.getProperty(key)));
                } catch (IllegalArgumentException skipped) {
                    // A provider with a bad id or an unsafe address is left out, never called.
                }
            }
        }
        return new GlobalModels(
                choice(properties, "chat", GlobalModels.DEFAULTS.chat()),
                choice(properties, "embedding", GlobalModels.DEFAULTS.embedding()),
                providers);
    }

    private static ModelChoice choice(Properties properties, String prefix, ModelChoice fallback) {
        String model = properties.getProperty(prefix + ".model", "").trim();
        if (model.isEmpty()) {
            return fallback;
        }
        return ModelChoice.qualified(properties.getProperty(prefix + ".provider", ""), model);
    }

    /**
     * Writes {@code models} to {@code file}, creating its directory if needed: beside the file
     * and moved over it, so a game that stops mid-write leaves the old settings.
     */
    static void save(GlobalModels models, Path file) throws IOException {
        Path directory = file.toAbsolutePath().getParent();
        if (directory != null) {
            Files.createDirectories(directory);
        }
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
            writer.write("# The global model of every MCAgents mod, set on the Models page.\n");
            line(writer, "chat.provider", models.chat().provider());
            line(writer, "chat.model", models.chat().model());
            line(writer, "embedding.provider", models.embedding().provider());
            line(writer, "embedding.model", models.embedding().model());
            for (ModelProvider provider : models.customProviders()) {
                line(writer, PROVIDER_PREFIX + provider.id(), provider.baseUrl());
            }
        }
        try {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** One {@code key=value} line, escaped the way {@link Properties#load} reads it back. */
    private static void line(Writer writer, String key, String value) throws IOException {
        Properties one = new Properties();
        one.setProperty(key, value);
        java.io.StringWriter text = new java.io.StringWriter();
        one.store(text, null);
        // store() writes a date comment first; only the property line is kept.
        for (String stored : text.toString().split("\\R")) {
            if (!stored.startsWith("#") && !stored.isEmpty()) {
                writer.write(stored);
                writer.write('\n');
            }
        }
    }
}
