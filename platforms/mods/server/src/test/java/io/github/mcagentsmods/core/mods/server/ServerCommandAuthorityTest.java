package io.github.mcagentsmods.core.mods.server;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ServerCommandAuthority} and {@link CommandCaller}.
 *
 * <p>These are the checks that separate a player from the server owner's
 * billing account, so the interesting cases are all the ones that must
 * <em>fail</em>: an unidentified caller, a level that arrived negative, a
 * player one level short.</p>
 */
@DisplayName("ServerCommandAuthority")
class ServerCommandAuthorityTest {

    /**
     * The authority under test, with the shipped requirement.
     */
    private final ServerCommandAuthority authority = new ServerCommandAuthority();

    @Nested
    @DisplayName("refusals")
    class Refusals {

        @Test
        @DisplayName("refuses an ordinary player every action")
        void refusesOrdinaryPlayers() {
            CommandCaller player = CommandCaller.player(UUID.randomUUID(), "Steve", 0);

            for (ServerAction action : ServerAction.values()) {
                assertFalse(authority.allows(player, action), action.name());
                assertTrue(authority.refusalFor(player, action).isPresent(), action.name());
            }
        }

        @Test
        @DisplayName("refuses a caller one level short")
        void refusesNearlyEnough() {
            CommandCaller almost = CommandCaller.player(UUID.randomUUID(), "Alex",
                    CommandCaller.OWNER_LEVEL - 1);

            assertFalse(authority.allows(almost, ServerAction.ADD_TOKEN));
        }

        @Test
        @DisplayName("refuses a caller nothing identified")
        void refusesAnUnidentifiedCaller() {
            // Fails closed. An absent caller is a bug at the call site, and the
            // dangerous reading of a bug is "allow".
            assertFalse(authority.allows(null, ServerAction.ADD_TOKEN));
            assertTrue(authority.refusalFor(null, ServerAction.VIEW_STATUS).isPresent());
        }

        @Test
        @DisplayName("says what was refused without naming what it protects")
        void explainsWithoutLeaking() {
            CommandCaller player = CommandCaller.player(UUID.randomUUID(), "Steve", 0);

            Optional<String> refusal = authority.refusalFor(player, ServerAction.REMOVE_TOKEN);

            assertTrue(refusal.isPresent());
            assertTrue(refusal.get().contains(ServerAction.REMOVE_TOKEN.description()), refusal.get());
            // A refusal that listed the configured platforms would tell a
            // player exactly what is worth attacking.
            assertFalse(refusal.get().contains("openai"), refusal.get());
        }

        @Test
        @DisplayName("throws for a caller that will not take no for an answer")
        void requireThrows() {
            CommandCaller player = CommandCaller.player(UUID.randomUUID(), "Steve", 0);

            SecurityException thrown = assertThrows(SecurityException.class,
                    () -> authority.require(player, ServerAction.RELOAD));

            assertTrue(thrown.getMessage().contains(ServerAction.RELOAD.description()));
        }

        @Test
        @DisplayName("refuses a null action outright, rather than deciding about nothing")
        void refusesNullAction() {
            assertThrows(NullPointerException.class,
                    () -> authority.refusalFor(CommandCaller.console(), null));
        }
    }

    @Nested
    @DisplayName("permissions")
    class Permissions {

        @Test
        @DisplayName("lets the owner do everything")
        void allowsTheOwner() {
            CommandCaller owner = CommandCaller.player(UUID.randomUUID(), "Notch",
                    CommandCaller.OWNER_LEVEL);

            for (ServerAction action : ServerAction.values()) {
                assertTrue(authority.allows(owner, action), action.name());
                assertEquals(Optional.empty(), authority.refusalFor(owner, action));
            }
        }

        @Test
        @DisplayName("lets the console do everything, since it already owns the machine")
        void allowsTheConsole() {
            for (ServerAction action : ServerAction.values()) {
                assertTrue(authority.allows(CommandCaller.console(), action), action.name());
            }
        }

        @Test
        @DisplayName("refuses to be built with a level any player would meet")
        void refusesAToothlessRequirement() {
            assertThrows(IllegalArgumentException.class, () -> new ServerCommandAuthority(0));
            assertThrows(IllegalArgumentException.class, () -> new ServerCommandAuthority(-1));
        }

        @Test
        @DisplayName("honours a stricter or looser requirement it was built with")
        void honoursItsRequirement() {
            ServerCommandAuthority lenient = new ServerCommandAuthority(2);
            CommandCaller moderator = CommandCaller.player(UUID.randomUUID(), "Alex", 2);

            assertEquals(2, lenient.requiredLevel());
            assertTrue(lenient.allows(moderator, ServerAction.VIEW_STATUS));
            assertFalse(authority.allows(moderator, ServerAction.VIEW_STATUS));
        }
    }

    @Nested
    @DisplayName("CommandCaller")
    class Callers {

        @Test
        @DisplayName("clamps a negative level to none at all")
        void clampsNegativeLevels() {
            // A level computed wrong must not become a level that grants
            // anything, and must not throw either — a malformed value at a
            // command boundary should refuse, not crash the server.
            CommandCaller malformed = new CommandCaller(UUID.randomUUID(), "Steve", Integer.MIN_VALUE);

            assertEquals(0, malformed.permissionLevel());
            assertFalse(authority.allows(malformed, ServerAction.ADD_TOKEN));
        }

        @Test
        @DisplayName("knows the console from a player")
        void knowsTheConsole() {
            assertTrue(CommandCaller.console().isConsole());
            assertFalse(CommandCaller.player(UUID.randomUUID(), "Steve", 4).isConsole());
        }

        @Test
        @DisplayName("refuses a caller that was never really identified")
        void refusesAnEmptyIdentity() {
            assertThrows(IllegalArgumentException.class,
                    () -> new CommandCaller(UUID.randomUUID(), "   ", 4));
            assertThrows(NullPointerException.class,
                    () -> new CommandCaller(UUID.randomUUID(), null, 4));
            assertThrows(NullPointerException.class,
                    () -> CommandCaller.player(null, "Steve", 4));
        }
    }
}
