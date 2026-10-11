package io.github.mcagentsmods.core.shared.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ClientCredentialDisplay}.
 */
@DisplayName("ClientCredentialDisplay")
class ClientCredentialDisplayTest {

    @Test
    @DisplayName("leaves a line that already fits alone")
    void leavesShortLinesAlone() {
        List<String> lines = List.of("openai: ready  #1:a3f9", "openrouter: not configured");

        assertEquals(lines, ClientCredentialDisplay.wrap(lines, 40));
    }

    @Test
    @DisplayName("breaks between words and indents the continuation")
    void breaksBetweenWords() {
        List<String> wrapped = ClientCredentialDisplay.wrap(
                List.of("openai ready openrouter ready deepseek ready"), 20);

        assertEquals(List.of("openai ready", "  openrouter ready", "  deepseek ready"), wrapped);
    }

    @Test
    @DisplayName("keeps every piece within the width")
    void respectsTheWidth() {
        List<String> wrapped = ClientCredentialDisplay.wrap(
                List.of("Credentials: /home/player/.minecraft/instances/pack/mcagents.json"), 24);

        assertTrue(wrapped.size() > 1, "a long line should have been split");
        wrapped.forEach(line -> assertTrue(line.length() <= 24, "too long: " + line));
    }

    @Test
    @DisplayName("splits a single word too long to fit rather than letting it run off")
    void splitsAnUnbreakableWord() {
        // A file path with no spaces is the realistic case. Seeing most of it
        // beats seeing the first twenty characters and nothing else.
        List<String> wrapped = ClientCredentialDisplay.wrap(List.of("/a/very/long/path/without/spaces"), 10);

        assertTrue(wrapped.size() > 1);
        assertEquals("/a/very/lo", wrapped.get(0));
        assertEquals("/a/very/long/path/without/spaces",
                String.join("", wrapped).replace("  ", ""));
    }

    @Test
    @DisplayName("never reveals more than the lines it was given")
    void addsNothingOfItsOwn() {
        String token = "sk-or-v1-0123456789abcdef";
        List<String> wrapped = ClientCredentialDisplay.wrap(List.of("openrouter: ready  #1:cdef"), 20);

        wrapped.forEach(line -> assertFalse(line.contains(token)));
    }

    @Test
    @DisplayName("handles an empty list and an empty line")
    void handlesNothingToDraw() {
        assertTrue(ClientCredentialDisplay.wrap(List.of()).isEmpty());
        assertEquals(List.of(""), ClientCredentialDisplay.wrap(List.of("")));
    }

    @Test
    @DisplayName("refuses input it cannot draw")
    void refusesUnusableInput() {
        assertThrows(NullPointerException.class, () -> ClientCredentialDisplay.wrap(null));
        assertThrows(IllegalArgumentException.class, () -> ClientCredentialDisplay.wrap(List.of("a"), 2));
    }
}
