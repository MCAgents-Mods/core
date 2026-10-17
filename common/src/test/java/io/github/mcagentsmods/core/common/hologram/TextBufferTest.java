package io.github.mcagentsmods.core.common.hologram;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Typing into a prompt: inserting at the caret, deleting, moving, and the limit. */
class TextBufferTest {

    @Test
    void typingInsertsAtTheCaret() {
        TextBuffer buffer = new TextBuffer(100);
        buffer.insert("held");
        buffer.left();
        buffer.left();
        buffer.insert("l");

        assertEquals("helld", buffer.text());
        assertEquals(3, buffer.caret());
    }

    @Test
    void backspaceAndDeleteTakeOneCharacterEitherSide() {
        TextBuffer buffer = new TextBuffer(100);
        buffer.insert("abc");
        buffer.left();

        assertTrue(buffer.backspace());
        assertEquals("ac", buffer.text());
        assertTrue(buffer.delete());
        assertEquals("a", buffer.text());
        assertFalse(buffer.delete(), "nothing after the caret");
        assertTrue(buffer.backspace());
        assertFalse(buffer.backspace(), "nothing before the caret");
    }

    @Test
    void whatDoesNotFitUnderTheLimitIsDropped() {
        TextBuffer buffer = new TextBuffer(5);

        assertTrue(buffer.insert("abcdefg"));
        assertEquals("abcde", buffer.text());
        assertFalse(buffer.insert("x"), "full");
    }

    @Test
    void theLimitNeverKeepsHalfACharacter() {
        TextBuffer buffer = new TextBuffer(2);

        buffer.insert("a😀");

        assertEquals("a", buffer.text());
    }

    @Test
    void controlCharactersAreCleanedOnTheWayIn() {
        TextBuffer buffer = new TextBuffer(100);

        buffer.insert("one\r\ntwo\rthree\tfour\u0007");

        assertEquals("one\ntwo\nthree four", buffer.text());
    }

    @Test
    void theCaretStepsOverWholeCharacters() {
        TextBuffer buffer = new TextBuffer(100);
        buffer.insert("a😀b");
        buffer.left();
        buffer.left();

        assertEquals(1, buffer.caret());
        assertTrue(buffer.delete());
        assertEquals("ab", buffer.text());

        buffer.moveTo(99);
        assertEquals(2, buffer.caret());
    }

    @Test
    void moveToNeverStopsInsideACharacter() {
        TextBuffer buffer = new TextBuffer(100);
        buffer.insert("a😀b");

        buffer.moveTo(2);

        assertEquals(1, buffer.caret());
    }

    @Test
    void homeAndEndKeepToTheCaretsLine() {
        TextBuffer buffer = new TextBuffer(100);
        buffer.insert("first\nsecond line\nthird");
        buffer.moveTo(9);

        buffer.home();
        assertEquals(6, buffer.caret());
        buffer.end();
        assertEquals(17, buffer.caret());
        buffer.moveTo(0);
        buffer.home();
        assertEquals(0, buffer.caret());
    }

    @Test
    void blanksAloneCountAsNothingToSend() {
        TextBuffer buffer = new TextBuffer(100);
        buffer.insert("  \n ");

        assertTrue(buffer.isBlank());
        buffer.clear();
        assertEquals("", buffer.text());
        assertEquals(0, buffer.caret());
    }

    @Test
    void controlBackspaceEmptiesTheLineUpToTheCaret() {
        TextBuffer buffer = new TextBuffer(100);
        buffer.insert("first\nsecond line");

        assertTrue(buffer.backspaceLine());
        assertEquals("first\n", buffer.text());
        assertEquals(6, buffer.caret());
        assertFalse(buffer.backspaceLine());
    }
}
