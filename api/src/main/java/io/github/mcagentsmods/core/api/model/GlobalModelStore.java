package io.github.mcagentsmods.core.api.model;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Where the {@link GlobalModels global model} is kept.
 *
 * <p>Core keeps the player's in a file and hands it out through {@code core.common.model.Models};
 * a test, or a mod checking its own logic, uses {@link #inMemory(GlobalModels)}.
 */
public interface GlobalModelStore {

    /** The global model now. Cheap: read it per request rather than holding a copy. */
    GlobalModels current();

    /**
     * Makes {@code models} the global model at once and keeps it.
     *
     * @return what is now current; fails if it could not be kept, in which case it still
     *         applies until the game closes
     */
    CompletableFuture<GlobalModels> save(GlobalModels models);

    /**
     * Whether the player's global model has ever been saved.
     *
     * <p>{@code false} means {@link #current()} is {@link GlobalModels#DEFAULTS}, which is the
     * moment a mod that kept model settings of its own before this store existed moves them in.
     */
    boolean saved();

    /** Routing over this store, read per request. See {@link GlobalModels#routes}. */
    default ProviderRoutes routes() {
        return GlobalModels.routes(this::current);
    }

    /** A store that lives in memory only, starting from {@code initial} and never saved. */
    static GlobalModelStore inMemory(GlobalModels initial) {
        Objects.requireNonNull(initial, "initial");
        AtomicReference<GlobalModels> current = new AtomicReference<>(initial);
        AtomicBoolean saved = new AtomicBoolean();
        return new GlobalModelStore() {
            @Override
            public GlobalModels current() {
                return current.get();
            }

            @Override
            public CompletableFuture<GlobalModels> save(GlobalModels models) {
                current.set(Objects.requireNonNull(models, "models"));
                saved.set(true);
                return CompletableFuture.completedFuture(models);
            }

            @Override
            public boolean saved() {
                return saved.get();
            }
        };
    }
}
