package io.github.mcagentsmods.core.shared.server;

import io.github.mcagentsmods.core.shared.environment.ServerOnly;

import java.util.Objects;
import java.util.UUID;

/**
 * Who is asking, as the <strong>server</strong> knows them.
 *
 * <p>Every field here must be read from server-side state — the player the
 * server has authenticated, and the permission level the server assigned. None
 * of it may come from a packet. A client can claim any name and any level it
 * likes; believing the claim is the whole class of bug this record exists to
 * make visible, because a reviewer can see at the construction site where each
 * value came from.</p>
 *
 * @param uniqueId The player's identity, or {@code null} for the server console
 *                 — which has no player behind it and is trusted absolutely,
 *                 since whoever reached it already owns the machine.
 * @param name What to call them in a message. Never blank; a blank name is a
 *             caller that was never really identified.
 * @param permissionLevel The level the server assigned, on Minecraft's own
 *                        0 to 4 scale. A negative value is clamped to
 *                        {@code 0} rather than rejected — a malformed level
 *                        must fail closed, never open.
 */
@ServerOnly
public record CommandCaller(UUID uniqueId, String name, int permissionLevel) {

    /**
     * The level a server owner holds, and what credential management requires.
     *
     * <p>Minecraft's own scale: {@code 0} is everyone, {@code 2} is the level
     * commands like {@code /gamemode} sit at, and {@code 4} is full ownership.
     * Credentials are the owner's money, so they sit at the top of it.</p>
     */
    public static final int OWNER_LEVEL = 4;

    /**
     * Validates and clamps the components.
     *
     * @throws NullPointerException When the name is {@code null}.
     * @throws IllegalArgumentException When the name is blank.
     */
    public CommandCaller {
        Objects.requireNonNull(name, "name cannot be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name cannot be blank");
        }

        name = name.trim();
        // Fail closed. A level that arrived negative is a level that was
        // computed wrong, and the safe reading of a wrong number is "no
        // permissions at all".
        permissionLevel = Math.max(0, permissionLevel);
    }

    /**
     * Creates the caller representing the server console.
     *
     * @return A caller at {@link #OWNER_LEVEL} with no player identity.
     */
    public static CommandCaller console() {
        return new CommandCaller(null, "Console", OWNER_LEVEL);
    }

    /**
     * Creates a caller for an authenticated player.
     *
     * @param uniqueId The identity the server authenticated.
     * @param name The player's name.
     * @param permissionLevel The level the server assigned.
     * @return The new caller.
     * @throws NullPointerException When the identity or name is {@code null}.
     */
    public static CommandCaller player(UUID uniqueId, String name, int permissionLevel) {
        Objects.requireNonNull(uniqueId, "uniqueId cannot be null");
        return new CommandCaller(uniqueId, name, permissionLevel);
    }

    /**
     * Reports whether this is the server console rather than a player.
     *
     * @return {@code true} when no player identity is attached.
     */
    public boolean isConsole() {
        return uniqueId == null;
    }

    /**
     * Reports whether this caller holds at least a permission level.
     *
     * @param required The level to meet.
     * @return {@code true} when the assigned level is at least {@code required}.
     */
    public boolean hasLevel(int required) {
        return permissionLevel >= required;
    }
}
