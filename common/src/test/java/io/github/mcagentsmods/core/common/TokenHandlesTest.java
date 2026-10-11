package io.github.mcagentsmods.core.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link TokenHandles}.
 *
 * <p>This class is worth testing above everything else in {@code common}: it is
 * the one place where a credential is turned into something safe to show, and
 * the one place where what a player typed is turned back into a credential to
 * delete. A defect in the first leaks a key onto a screen; a defect in the
 * second deletes the wrong key.</p>
 */
@DisplayName("TokenHandles")
class TokenHandlesTest {

    @Nested
    @DisplayName("of")
    class Of {

        @Test
        @DisplayName("reveals the last four characters and a one-based position")
        void revealsTailAndPosition() {
            assertEquals("#1:a3f9", TokenHandles.of(0, "sk-or-v1-secreta3f9"));
            assertEquals("#3:cdef", TokenHandles.of(2, "sk-abcdef"));
        }

        @Test
        @DisplayName("never contains the credential it describes")
        void hidesTheCredential() {
            String token = "sk-or-v1-0123456789abcdef";

            String handle = TokenHandles.of(0, token);

            assertFalse(handle.contains(token));
            assertEquals(4, handle.substring(handle.indexOf(':') + 1).length());
        }

        @Test
        @DisplayName("does not pad a value shorter than a handle")
        void doesNotPadShortValues() {
            // Padding would dress a three character string up as a real key.
            assertEquals("#1:abc", TokenHandles.of(0, "abc"));
            assertEquals("#1:", TokenHandles.of(0, ""));
        }

        @Test
        @DisplayName("rejects a null credential")
        void rejectsNull() {
            assertThrows(NullPointerException.class, () -> TokenHandles.of(0, null));
        }
    }

    @Nested
    @DisplayName("all")
    class All {

        @Test
        @DisplayName("numbers the handles in list order")
        void numbersInOrder() {
            List<String> handles = TokenHandles.all(List.of("sk-aaaa", "sk-bbbb", "sk-cccc"));

            assertEquals(List.of("#1:aaaa", "#2:bbbb", "#3:cccc"), handles);
        }

        @Test
        @DisplayName("returns nothing for an empty list")
        void handlesEmptyList() {
            assertTrue(TokenHandles.all(List.of()).isEmpty());
        }
    }

    @Nested
    @DisplayName("resolve")
    class Resolve {

        private final List<String> tokens = List.of("sk-aaaa", "sk-bbbb", "sk-cccc");

        @Test
        @DisplayName("accepts a handle from tab completion")
        void acceptsHandle() {
            assertEquals(Optional.of("sk-bbbb"), TokenHandles.resolve(tokens, "#2:bbbb"));
        }

        @Test
        @DisplayName("accepts the credential itself")
        void acceptsRawCredential() {
            assertEquals(Optional.of("sk-cccc"), TokenHandles.resolve(tokens, "sk-cccc"));
        }

        @Test
        @DisplayName("trims what was typed")
        void trimsInput() {
            assertEquals(Optional.of("sk-aaaa"), TokenHandles.resolve(tokens, "  #1:aaaa  "));
        }

        @Test
        @DisplayName("refuses a handle whose slot now holds something else")
        void refusesStaleHandle() {
            // The list shifted between tab completion offering #2:bbbb and the
            // command running. Resolving it would delete whatever moved into
            // slot two, which is the one genuinely dangerous outcome here.
            List<String> shifted = List.of("sk-cccc", "sk-dddd");

            assertEquals(Optional.empty(), TokenHandles.resolve(shifted, "#2:bbbb"));
        }

        @Test
        @DisplayName("accepts a bare position with no revealed characters")
        void acceptsBarePosition() {
            assertEquals(Optional.of("sk-aaaa"), TokenHandles.resolve(tokens, "#1"));
        }

        @Test
        @DisplayName("returns nothing for a position outside the list")
        void refusesOutOfRange() {
            assertEquals(Optional.empty(), TokenHandles.resolve(tokens, "#0:aaaa"));
            assertEquals(Optional.empty(), TokenHandles.resolve(tokens, "#4:dddd"));
        }

        @Test
        @DisplayName("returns nothing for input that names no credential")
        void refusesNonsense() {
            assertEquals(Optional.empty(), TokenHandles.resolve(tokens, "#not-a-number:aaaa"));
            assertEquals(Optional.empty(), TokenHandles.resolve(tokens, "sk-never-stored"));
            assertEquals(Optional.empty(), TokenHandles.resolve(tokens, "   "));
            assertEquals(Optional.empty(), TokenHandles.resolve(tokens, null));
        }
    }
}
