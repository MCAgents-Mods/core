package io.github.mcagentsmods.core.common.model;

import io.github.mcagentsmods.core.api.model.GlobalModelStore;
import io.github.mcagentsmods.core.api.model.GlobalModels;
import io.github.mcagentsmods.core.api.model.ModelChoice;
import io.github.mcagentsmods.core.api.model.ProviderRoutes;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The global model, for every MCAgents mod: what a task with no model of its own uses, and the
 * providers a request can go to.
 *
 * <pre>
 * ModelChoice chat = Models.global().chatOr(ownChoiceOrNull);
 * client.complete(new ChatRequest(chat.model(), messages).withProvider(chat.provider()));
 * </pre>
 *
 * <p>Kept in {@code {game_dir}/MCAgents/core/models.properties} once {@link #start(Path)} has
 * named the file; core does that when the client starts, and a mod that needs the global model
 * earlier may call it first with the same file, after
 * {@link io.github.mcagentsmods.core.common.agent.AgentsPaths#moveLegacyFiles()}. Until then, and in tests, the global model is
 * {@link GlobalModels#DEFAULTS} in memory.
 */
public final class Models {

    private static final FileStore STORE = new FileStore();

    private Models() {
        throw new AssertionError("No instances.");
    }

    /** The global model now. Read it per request; it changes when the player saves the Models page. */
    public static GlobalModels global() {
        return STORE.current();
    }

    /** The store behind {@link #global()}, for the page that edits it. */
    public static GlobalModelStore store() {
        return STORE;
    }

    /** Routing that sends a chosen provider's request to that provider's address. */
    public static ProviderRoutes routes() {
        return STORE.routes();
    }

    /** {@code own} when the task set one, else the global chat model. */
    public static ModelChoice chat(ModelChoice own) {
        return global().chatOr(own);
    }

    /** {@code own} when the task set one, else the global embedding model. */
    public static ModelChoice embedding(ModelChoice own) {
        return global().embeddingOr(own);
    }

    /**
     * Reads the global model from {@code file}. Only the first call does anything; a missing
     * or unreadable file gives the defaults, and the first save writes it.
     *
     * @return whether this call was the one that started it
     */
    public static boolean start(Path file) {
        return STORE.start(Objects.requireNonNull(file, "file"));
    }

    /** The file-backed store; in memory until it is started. */
    private static final class FileStore implements GlobalModelStore {

        private final AtomicReference<Path> file = new AtomicReference<>();
        private final AtomicReference<GlobalModels> current = new AtomicReference<>(GlobalModels.DEFAULTS);
        private final AtomicBoolean saved = new AtomicBoolean();
        private final Object saving = new Object();

        boolean start(Path path) {
            if (!file.compareAndSet(null, path)) {
                return false;
            }
            ModelSettingsFile.load(path).ifPresent(models -> {
                current.set(models);
                saved.set(true);
            });
            return true;
        }

        @Override
        public GlobalModels current() {
            return current.get();
        }

        @Override
        public boolean saved() {
            return saved.get();
        }

        @Override
        public CompletableFuture<GlobalModels> save(GlobalModels models) {
            Objects.requireNonNull(models, "models");
            current.set(models);
            saved.set(true);
            Path path = file.get();
            if (path == null) {
                return CompletableFuture.completedFuture(models);
            }
            return CompletableFuture.supplyAsync(() -> {
                synchronized (saving) {
                    try {
                        // The newest settings, not necessarily these: two quick saves write the
                        // second twice rather than leaving the first on disk.
                        ModelSettingsFile.save(current.get(), path);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
                return models;
            });
        }
    }
}
