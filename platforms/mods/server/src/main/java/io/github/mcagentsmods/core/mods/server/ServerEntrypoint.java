package io.github.mcagentsmods.core.mods.server;

import io.github.mcagentsmods.core.mods.ModTokenCommands;
import io.github.mcagentsmods.core.mods.environment.ModContext;
import io.github.mcagentsmods.core.mods.environment.PhysicalSide;
import io.github.mcagentsmods.core.mods.environment.ServerOnly;
import io.github.mcagentsmods.core.mods.environment.SideEntrypoint;
import io.github.mcagentsmods.core.mods.environment.WrongSideException;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The server half's entry point.
 *
 * <p>Two things differ from the client half, and both are the reason the halves
 * are separate rather than one class with a branch in it:</p>
 *
 * <ul>
 *   <li><strong>The credentials belong to the server owner</strong>, so they
 *       live under the server's run directory rather than under a player's
 *       Minecraft directory. See {@link ServerRunDirectory}.</li>
 *   <li><strong>Every operation is checked.</strong> A client has one user who
 *       already owns the keys; a server has as many users as it has players,
 *       and none of them owns the bill. See
 *       {@link ServerCommandAuthority}.</li>
 * </ul>
 *
 * <p>No side guard is asserted in the constructor. Server logic runs on a
 * client too — a single player world is a server — and refusing to construct
 * this there would break single player for nothing. What must not happen is the
 * reverse, and that is guarded on the client half.</p>
 */
@ServerOnly
public final class ServerEntrypoint implements SideEntrypoint {

    /**
     * What this half is called in a failure message.
     */
    private static final String FEATURE = "The MCAgents server entry point";

    /**
     * Who may do what. Fixed for the lifetime of the entry point: an authority
     * that could be swapped at runtime is an authority that can be swapped by a
     * bug.
     */
    private final ServerCommandAuthority authority = new ServerCommandAuthority();

    /**
     * The commands over the server's credential file, or {@code null} before
     * {@link #start(ModContext)} and after {@link #stop()}.
     */
    private volatile ModTokenCommands commands;

    /**
     * Where the server's credential file was resolved to, or {@code null}
     * before {@link #start(ModContext)}.
     */
    private volatile Path credentialFile;

    /**
     * {@inheritDoc}
     */
    @Override
    public PhysicalSide side() {
        return PhysicalSide.DEDICATED_SERVER;
    }

    /**
     * Opens the server's credential file and the commands over it.
     *
     * @param context What the loader knows.
     * @throws NullPointerException When {@code context} is {@code null}.
     * @throws WrongSideException Never from here — a client hosting a single
     *                            player world runs this half legitimately, so
     *                            no side is refused.
     */
    @Override
    public void start(ModContext context) {
        Objects.requireNonNull(context, "context cannot be null");

        Path directory = ServerRunDirectory.resolveDirectory(context.gameDirectory());
        this.credentialFile = ServerRunDirectory.resolveFile(context.gameDirectory());
        this.commands = new ModTokenCommands(directory, context.logger(), context.requestTimeout());

        context.logger().info("MCAgents server ready. Credentials: " + credentialFile
                + ". Managing them needs permission level " + authority.requiredLevel() + ".");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        this.commands = null;
        this.credentialFile = null;
    }

    /**
     * Returns who may do what on this server.
     *
     * @return The authority, never {@code null}.
     */
    public ServerCommandAuthority authority() {
        return authority;
    }

    /**
     * Returns where the server's credential file was resolved to.
     *
     * @return The resolved path, or {@code null} before the entry point
     *         started.
     */
    public Path credentialFile() {
        return credentialFile;
    }

    /**
     * Describes every platform's credential state, for a caller allowed to see
     * it.
     *
     * <p>The check happens here rather than at the command, so that adding a
     * second way to reach this — a web panel, a console command, an RCON
     * bridge — cannot accidentally skip it.</p>
     *
     * @param caller Who is asking, as the server knows them.
     * @return One line per platform with masked handles, or the refusal as a
     *         single line. Never contains a credential either way.
     * @throws IllegalStateException When called before {@link #start(ModContext)}.
     */
    public List<String> status(CommandCaller caller) {
        Optional<String> refusal = authority.refusalFor(caller, ServerAction.VIEW_STATUS);
        if (refusal.isPresent()) {
            return List.of(refusal.get());
        }
        return commands().status();
    }

    /**
     * Re-reads the credential file, for a caller allowed to do it.
     *
     * @param caller Who is asking, as the server knows them.
     * @return A message to show them.
     * @throws IllegalStateException When called before {@link #start(ModContext)}.
     */
    public String reload(CommandCaller caller) {
        Optional<String> refusal = authority.refusalFor(caller, ServerAction.RELOAD);
        return refusal.orElseGet(() -> commands().reload());
    }

    /**
     * Stores a credential, for a caller allowed to do it.
     *
     * @param caller Who is asking, as the server knows them.
     * @param platform The platform code.
     * @param token The credential to store.
     * @return A message to show them. Never contains the credential.
     * @throws IllegalStateException When called before {@link #start(ModContext)}.
     */
    public String addToken(CommandCaller caller, String platform, String token) {
        Optional<String> refusal = authority.refusalFor(caller, ServerAction.ADD_TOKEN);
        return refusal.orElseGet(() -> commands().add(platform, token));
    }

    /**
     * Removes a credential, for a caller allowed to do it.
     *
     * @param caller Who is asking, as the server knows them.
     * @param platform The platform code.
     * @param handleOrToken The masked handle from tab completion, or the
     *                      credential itself.
     * @return A message to show them. Never contains the credential.
     * @throws IllegalStateException When called before {@link #start(ModContext)}.
     */
    public String removeToken(CommandCaller caller, String platform, String handleOrToken) {
        Optional<String> refusal = authority.refusalFor(caller, ServerAction.REMOVE_TOKEN);
        return refusal.orElseGet(() -> commands().remove(platform, handleOrToken));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String describe() {
        Path file = credentialFile;
        return file == null
                ? "MCAgents server (not started)"
                : "MCAgents server, credentials at " + file;
    }

    /**
     * Returns the started commands.
     *
     * @return The commands.
     * @throws IllegalStateException When the entry point has not been started.
     */
    private ModTokenCommands commands() {
        ModTokenCommands current = commands;
        if (current == null) {
            throw new IllegalStateException(FEATURE + " has not been started.");
        }
        return current;
    }
}
