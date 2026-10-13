package io.github.mcagentsmods.core.common.hologram;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.IntUnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Text cut into lines the way the transcript and the prompt show it. */
class TextWrapTest {

    /** Every character one unit wide, so a width is a character count. */
    private static final IntUnaryOperator ONE = codePoint -> 1;

    private static List<String> lines(String text, int width) {
        return TextWrap.wrap(text, width, ONE).stream().map(line -> line.of(text)).toList();
    }

    @Test
    void aLineEndsAtTheLastSpaceThatFitsAndTheSpaceIsDropped() {
        assertEquals(List.of("hello world", "foo"), lines("hello world foo", 11));
        assertEquals(List.of("hello", "world foo"), lines("hello world foo", 10));
        assertEquals(List.of("hello", "world", "foo"), lines("hello world foo", 7));
    }

    @Test
    void aWordWiderThanTheLineIsCutBetweenCharacters() {
        assertEquals(List.of("abcd", "efgh", "ij"), lines("abcdefghij", 4));
        assertEquals(List.of("a", "bcde", "fg"), lines("a bcdefg", 4));
    }

    @Test
    void aLineBreakAlwaysEndsALine() {
        assertEquals(List.of("one", "two", "", "four"), lines("one\ntwo\n\nfour", 20));
    }

    @Test
    void emptyTextAndATrailingLineBreakLeaveAnEmptyLineForTheCaret() {
        assertEquals(List.of(""), lines("", 10));
        assertEquals(List.of("one", ""), lines("one\n", 10));
    }

    @Test
    void widthsComeFromTheCaller() {
        IntUnaryOperator wide = codePoint -> codePoint == 'w' ? 3 : 1;

        List<TextLine> wrapped = TextWrap.wrap("ww ii", 4, wide);

        assertEquals(List.of("w", "w", "ii"),
                wrapped.stream().map(line -> line.of("ww ii")).toList());
    }

    @Test
    void aCharacterOutsideTheBasicPlaneIsNeverSplit() {
        String text = "a😀b";

        assertEquals(List.of("a", "😀", "b"), lines(text, 1));
    }

    @Test
    void lineAtFindsTheLineACaretStandsOn() {
        String text = "hello world";
        List<TextLine> wrapped = TextWrap.wrap(text, 5, ONE);

        assertEquals(0, TextWrap.lineAt(wrapped, 0));
        assertEquals(0, TextWrap.lineAt(wrapped, 5), "before the dropped space: end of line 0");
        assertEquals(1, TextWrap.lineAt(wrapped, 6), "after it: start of line 1");
        assertEquals(1, TextWrap.lineAt(wrapped, 11));

        List<TextLine> cut = TextWrap.wrap("abcdef", 3, ONE);
        assertEquals(1, TextWrap.lineAt(cut, 3), "a cut word continues on the next line");
    }
}
