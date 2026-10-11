package io.github.mcagentsmods.core.mods.server;

import io.github.mcagentsmods.core.mods.environment.ServerOnly;

import java.util.Objects;
import java.util.Optional;

/**
 * Decides whether a caller may do something to the server's credentials.
 *
 * <p>This is the truth check, and it belongs on the server for one reason: a
 * client cannot be believed. On a client, "am I allowed?" is a question with no
 * adversary — the player owns the machine and the keys. On a server the same
 * question is the only thing standing between a player and the owner's billing
 * account, and any part of the answer that a client could influence is not an
 * answer at all.</p>
 *
 * <p>So the input is a {@link CommandCaller} built from server-side state, and
 * the rules here are deliberately dull: one required level, applied to every
 * action, with no exceptions and no per-action softening. Rules that are easy
 * to state are rules that are easy to audit.</p>
 *
 * <h2>Refusals are values, not exceptions</h2>
 *
 * <p>{@link #refusalFor(CommandCaller, ServerAction)} returns the reason as an
 * {@link Optional}, because being refused is an ordinary outcome that a command
 * shows the player, not an error with a stack trace. Callers that would rather
 * fail loudly use {@link #require(CommandCaller, ServerAction)}.</p>
 */
@ServerOnly
public final class ServerCommandAuthority {

    /**
     * The permission level every credential action requires.
     */
    private final int requiredLevel;

    /**
     * Builds an authority requiring the server owner's level.
     */
    public ServerCommandAuthority() {
        this(CommandCaller.OWNER_LEVEL);
    }

    /**
     * Builds an authority requiring a level.
     *
     * @param requiredLevel The permission level every action requires. Below
     *                      {@code 1} would let any player manage the owner's
     *                      credentials, so it is refused here rather than
     *                      discovered later.
     * @throws IllegalArgumentException When the level is below {@code 1}.
     */
    public ServerCommandAuthority(int requiredLevel) {
        if (requiredLevel < 1) {
            throw new IllegalArgumentException(
                    "requiredLevel must be at least 1, or every player could manage credentials");
        }
        this.requiredLevel = requiredLevel;
    }

    /**
     * Returns the permission level actions require.
     *
     * @return The required level.
     */
    public int requiredLevel() {
        return requiredLevel;
    }

    /**
     * Reports whether a caller may perform an action.
     *
     * @param caller Who is asking, as the server knows them. A {@code null}
     *               caller is one that was never identified, and is refused.
     * @param action What they are trying to do.
     * @return {@code true} when it is allowed.
     * @throws NullPointerException When {@code action} is {@code null}.
     */
    public boolean allows(CommandCaller caller, ServerAction action) {
        return refusalFor(caller, action).isEmpty();
    }

    /**
     * Explains why a caller may not perform an action.
     *
     * @param caller Who is asking, as the server knows them.
     * @param action What they are trying to do.
     * @return The reason to show them, or empty when it is allowed. The reason
     *         never names a credential and never says which platforms exist —
     *         a refusal that leaks the thing it refused would be worse than no
     *         refusal.
     * @throws NullPointerException When {@code action} is {@code null}.
     */
    public Optional<String> refusalFor(CommandCaller caller, ServerAction action) {
        Objects.requireNonNull(action, "action cannot be null");

        if (caller == null) {
            // Nothing identified the caller. Fail closed: an unidentified
            // caller is not the same as a caller with no permissions, it is a
            // bug, and letting it through would be the worst kind.
            return Optional.of("You are not allowed to " + action.description() + ".");
        }
        if (!caller.hasLevel(requiredLevel)) {
            return Optional.of("You are not allowed to " + action.description()
                    + ". This needs permission level " + requiredLevel + " on this server.");
        }
        return Optional.empty();
    }

    /**
     * Refuses to continue unless a caller may perform an action.
     *
     * @param caller Who is asking, as the server knows them.
     * @param action What they are trying to do.
     * @throws NullPointerException When {@code action} is {@code null}.
     * @throws SecurityException When the caller may not perform it.
     */
    public void require(CommandCaller caller, ServerAction action) {
        Optional<String> refusal = refusalFor(caller, action);
        if (refusal.isPresent()) {
            throw new SecurityException(refusal.get());
        }
    }
}
