package io.github.mcagentsmods.core.shared;

import io.github.mcagentsmods.core.api.llm.LlmCredentials;
import io.github.mcagentsmods.core.api.llm.LlmVendor;
import io.github.mcagentsmods.core.api.token.TokenState;
import io.github.mcagentsmods.core.common.MCAgentsProvider;
import io.github.mcagentsmods.core.common.TokenHandles;
import io.github.mcagentsmods.core.shared.store.SharedTokenStore;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * The client side of {@code /agents}, minus the loader.
 *
 * <p>Both mod loaders build their commands on Brigadier, and both take
 * suggestions from a {@code SuggestionProvider} — but the types come from the
 * loader's own remapped Minecraft, which this module does not compile against.
 * So the loader entry point owns the twenty lines of Brigadier wiring, and
 * everything it needs to answer with lives here: the same subcommands, the same
 * masked handles, and the same results as the server command.</p>
 *
 * <p>Keeping it here rather than in each loader is what stops the NeoForge and
 * Fabric builds drifting apart, and means the behaviour is testable without a
 * game.</p>
 */
public final class ModTokenCommands {

    /**
     * The shared credential file this command manages.
     */
    private final SharedTokenStore store;

    /**
     * Where problems are reported.
     */
    private final Logger logger;

    /**
     * How long one request may take before it is abandoned.
     *
     * <p>Defined here rather than in any mod that consumes the API: a request
     * either answers or fails within this window,
     * which is what lets a consumer rely on its future always completing
     * without owning a timer of its own.</p>
     */
    private final Duration requestTimeout;

    /**
     * Opens the client side commands over the shared credential file.
     *
     * @param loaderDirectory The game directory the loader reported, or
     *                        {@code null} to fall back to the conventional
     *                        location for this operating system.
     * @param logger Where to report problems.
     * @throws NullPointerException When {@code logger} is {@code null}.
     */
    public ModTokenCommands(Path loaderDirectory, Logger logger) {
        this(loaderDirectory, logger, LlmCredentials.DEFAULT_TIMEOUT);
    }

    /**
     * Opens the client side commands with a non default request timeout.
     *
     * @param loaderDirectory The game directory the loader reported, or
     *                        {@code null} to fall back to the conventional
     *                        location for this operating system.
     * @param logger Where to report problems.
     * @param requestTimeout How long one request may take before it is
     *                       abandoned.
     * @throws NullPointerException When the logger or the timeout is
     *                              {@code null}.
     */
    public ModTokenCommands(Path loaderDirectory, Logger logger, Duration requestTimeout) {
        this.logger = Objects.requireNonNull(logger, "logger cannot be null");
        this.requestTimeout = Objects.requireNonNull(requestTimeout, "requestTimeout cannot be null");
        this.store = new SharedTokenStore(loaderDirectory, logger);
        reload();
    }

    /**
     * Lists the platform names, for the first argument's suggestions.
     *
     * @return Every supported platform code, plus the two bare subcommands.
     */
    public List<String> suggestFirstArgument() {
        List<String> options = new ArrayList<>(List.of("status", "reload"));
        for (LlmVendor vendor : LlmVendor.values()) {
            options.add(vendor.code());
        }
        return options;
    }

    /**
     * Lists the masked handles of a platform's stored credentials, for the
     * {@code remove} argument's suggestions.
     *
     * <p>Handles, never tokens. A client's suggestion list is drawn on screen,
     * and a key is worth money — see {@link TokenHandles}.</p>
     *
     * @param platform The platform code.
     * @return One handle per stored credential, or empty when the platform is
     *         unknown or has none.
     */
    public List<String> suggestRemovable(String platform) {
        return vendor(platform)
                .map(v -> TokenHandles.all(store.load(v.code())))
                .orElseGet(List::of);
    }

    /**
     * Stores a credential and puts it into use immediately.
     *
     * @param platform The platform code.
     * @param token The credential to store.
     * @return A message to show the player. Never contains the credential.
     */
    public String add(String platform, String token) {
        Optional<LlmVendor> vendor = vendor(platform);
        if (vendor.isEmpty()) {
            return "Unknown platform: " + platform;
        }
        if (!store.add(vendor.get().code(), token)) {
            return "That token is already stored for " + vendor.get().code()
                    + ", or the file could not be written.";
        }

        reload();
        List<String> tokens = store.load(vendor.get().code());
        return "Added " + TokenHandles.of(tokens.size() - 1, token.trim())
                + " to " + vendor.get().code() + ". " + tokens.size() + " token(s) stored.";
    }

    /**
     * Removes a stored credential named by handle or by value.
     *
     * @param platform The platform code.
     * @param typed The handle from tab completion, or the credential itself.
     * @return A message to show the player. Never contains the credential.
     */
    public String remove(String platform, String typed) {
        Optional<LlmVendor> vendor = vendor(platform);
        if (vendor.isEmpty()) {
            return "Unknown platform: " + platform;
        }

        List<String> tokens = store.load(vendor.get().code());
        Optional<String> target = TokenHandles.resolve(tokens, typed);
        if (target.isEmpty()) {
            return "No stored token matches " + typed + " for " + vendor.get().code() + ".";
        }

        store.evict(vendor.get().code(), target.get());
        reload();
        return "Removed a token from " + vendor.get().code() + ". "
                + store.load(vendor.get().code()).size() + " token(s) remain.";
    }

    /**
     * Describes every platform's credential state.
     *
     * @return One line per platform, with masked handles. Never contains a
     *         credential.
     */
    public List<String> status() {
        List<String> lines = new ArrayList<>();
        lines.add("Credentials: " + store.describe());

        MCAgentsProvider provider = MCAgentsProvider.instance;
        for (LlmVendor vendor : LlmVendor.values()) {
            TokenState state = provider == null ? TokenState.NOT_SET : provider.tokenState(vendor);
            List<String> handles = TokenHandles.all(store.load(vendor.code()));
            lines.add("  " + vendor.code() + ": " + describe(state)
                    + (handles.isEmpty() ? "" : "  " + String.join(" ", handles)));
        }
        return lines;
    }

    /**
     * Re-reads the shared credential file and reinstalls every platform.
     *
     * @return A message to show the player.
     */
    public String reload() {
        MCAgentsProvider provider = MCAgentsProvider.instance;
        if (provider == null) {
            logger.warning("MCAgents core has no provider installed, so credentials cannot be applied.");
            return "MCAgents is not ready.";
        }

        int ready = 0;
        for (LlmVendor vendor : LlmVendor.values()) {
            // The template carries the timeout, so it survives every rotation.
            LlmCredentials template = LlmCredentials.of(vendor, "placeholder").withTimeout(requestTimeout);
            if (provider.registerStore(vendor, store, template) == TokenState.READY) {
                ready++;
            }
        }
        return "Credentials reloaded. " + ready + " platform(s) have a usable token.";
    }

    /**
     * Renders a credential state as a phrase.
     *
     * @param state The state to describe.
     * @return The phrase to show.
     */
    private String describe(TokenState state) {
        return switch (state) {
            case READY -> "ready";
            case NOT_SET -> "not configured";
            case EXPIRED -> "expired — every token was rejected and removed";
        };
    }

    /**
     * Resolves a platform name.
     *
     * @param name What was typed.
     * @return The vendor, or empty when the name matches none.
     */
    private Optional<LlmVendor> vendor(String name) {
        if (name == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(LlmVendor.fromCode(name.toLowerCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
