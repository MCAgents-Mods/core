package io.github.mcagentsmods.core.shared.client;

import io.github.mcagentsmods.core.shared.ModTokenCommands;
import io.github.mcagentsmods.core.shared.environment.ClientOnly;
import io.github.mcagentsmods.core.shared.environment.ModContext;
import io.github.mcagentsmods.core.shared.environment.PhysicalSide;
import io.github.mcagentsmods.core.shared.environment.SideEntrypoint;
import io.github.mcagentsmods.core.shared.environment.SideGuard;
import io.github.mcagentsmods.core.shared.environment.WrongSideException;
import io.github.mcagentsmods.core.shared.store.MinecraftDirectory;
import io.github.mcagentsmods.core.shared.store.SharedTokenStore;

import java.nio.file.Path;
import java.util.Objects;

/**
 * The client half's entry point.
 *
 * <p>On a client the credentials are the <em>player's own</em>. They live in the
 * shared {@code MCAgents/core/mcagents.json} under the Minecraft directory, where every
 * MCAgents mod the player installs finds the same file, and they are managed
 * from the player's own commands. None of that is true on a dedicated server,
 * which is the whole reason this half exists separately —
 * {@code io.github.mcagentsmods.core.shared.server.ServerEntrypoint} is the
 * counterpart, and is deliberately never imported from here.</p>
 *
 * <h2>Lifecycle</h2>
 *
 * <p>A loader constructs this reflectively through
 * {@link io.github.mcagentsmods.core.shared.environment.ModBootstrap} and calls
 * {@link #start(ModContext)} once. Constructing it on a dedicated server throws
 * immediately rather than failing later somewhere less legible.</p>
 */
@ClientOnly
public final class ClientEntrypoint implements SideEntrypoint {

    /**
     * What this half is called in a failure message.
     */
    private static final String FEATURE = "The MCAgents client entry point";

    /**
     * The client commands over the shared credential file, or {@code null}
     * before {@link #start(ModContext)} and after {@link #stop()}.
     */
    private volatile ModTokenCommands commands;

    /**
     * Where the shared credential file was resolved to, or {@code null} before
     * {@link #start(ModContext)}.
     */
    private volatile Path credentialFile;

    /**
     * Refuses to exist anywhere but a client.
     *
     * @throws WrongSideException When constructed on a dedicated server.
     */
    public ClientEntrypoint() {
        SideGuard.requireClient(FEATURE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PhysicalSide side() {
        return PhysicalSide.CLIENT;
    }

    /**
     * Opens the player's credential file and the commands over it.
     *
     * <p>The game directory the loader reported is passed straight through:
     * a launcher or a modpack relocates the Minecraft directory freely, and the
     * loader is the only thing that knows where this instance actually
     * lives.</p>
     *
     * @param context What the loader knows.
     * @throws NullPointerException When {@code context} is {@code null}.
     * @throws WrongSideException When the context names the other side.
     */
    @Override
    public void start(ModContext context) {
        Objects.requireNonNull(context, "context cannot be null");
        if (!context.side().isClient()) {
            throw new WrongSideException(FEATURE, PhysicalSide.CLIENT, context.side());
        }

        this.credentialFile = MinecraftDirectory.resolveFile(context.gameDirectory());
        // Before 0.14.0 the file was loose in the Minecraft directory.
        SharedTokenStore.moveLegacyFile(MinecraftDirectory.legacyFile(context.gameDirectory()),
                credentialFile, context.logger());
        this.commands = new ModTokenCommands(context.gameDirectory(), context.logger(), context.requestTimeout());

        context.logger().info("MCAgents client ready. Credentials: " + credentialFile);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Nothing here holds a thread, a socket, or a file handle between calls
     * — the credential store opens the file per operation — so stopping is
     * dropping the references.</p>
     */
    @Override
    public void stop() {
        this.commands = null;
        this.credentialFile = null;
    }

    /**
     * Returns the client commands over the shared credential file.
     *
     * <p>The loader wires these into its own command tree: both loaders build
     * commands on Brigadier, whose types this module does not compile against,
     * so the twenty lines of wiring stay in the loader and everything it needs
     * to answer with is here.</p>
     *
     * @return The commands.
     * @throws IllegalStateException When called before {@link #start(ModContext)}
     *                               or after {@link #stop()}.
     */
    public ModTokenCommands commands() {
        ModTokenCommands current = commands;
        if (current == null) {
            throw new IllegalStateException(FEATURE + " has not been started.");
        }
        return current;
    }

    /**
     * Returns where the shared credential file was resolved to.
     *
     * @return The resolved path, or {@code null} before the entry point
     *         started. Diagnostic only — the path, never the contents.
     */
    public Path credentialFile() {
        return credentialFile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String describe() {
        Path file = credentialFile;
        return file == null
                ? "MCAgents client (not started)"
                : "MCAgents client, credentials at " + file;
    }
}
