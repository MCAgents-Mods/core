package io.github.mcagentsmods.core.common.hologram;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * Cuts text into lines no wider than a width, the way a hologram's text panels show it.
 *
 * <p>A line break always ends a line. Otherwise a line ends at the last space that fits, and
 * the space is dropped; a word wider than the whole width is cut between two characters.
 * Widths come from the caller, one code point at a time, so the same rules serve any font.
 */
public final class TextWrap {

    private TextWrap() {
    }

    /**
     * Wraps {@code text}. Empty text is one empty line, and text that ends in a line break ends
     * in an empty line, so a caret after the break has a line to stand on.
     *
     * @param text    the text
     * @param width   the widest a line may be, in the advance's units; at least 1
     * @param advance how wide one code point is
     * @return the lines, top to bottom
     */
    public static List<TextLine> wrap(String text, int width, IntUnaryOperator advance) {
        if (width < 1) {
            throw new IllegalArgumentException("width must be at least 1");
        }
        List<TextLine> lines = new ArrayList<>();
        int start = 0;
        while (true) {
            int lineBreak = text.indexOf('\n', start);
            int end = lineBreak < 0 ? text.length() : lineBreak;
            wrapParagraph(text, start, end, width, advance, lines);
            if (lineBreak < 0) {
                return lines;
            }
            start = lineBreak + 1;
        }
    }

    /**
     * Finds the line a character index falls on: the last line starting at or before it.
     *
     * @param lines what {@link #wrap} gave
     * @param index a character index, 0 to the text's length
     * @return the line's position in {@code lines}
     */
    public static int lineAt(List<TextLine> lines, int index) {
        int found = 0;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).start() <= index) {
                found = i;
            } else {
                break;
            }
        }
        return found;
    }

    private static void wrapParagraph(String text, int start, int end, int width,
                                      IntUnaryOperator advance, List<TextLine> lines) {
        int lineStart = start;
        while (true) {
            int used = 0;
            int lastSpace = -1;
            int i = lineStart;
            while (i < end) {
                int codePoint = text.codePointAt(i);
                int next = i + Character.charCount(codePoint);
                if (codePoint == ' ') {
                    lastSpace = i;
                }
                used += advance.applyAsInt(codePoint);
                if (used > width && codePoint != ' ') {
                    break;
                }
                i = next;
            }
            if (i >= end) {
                lines.add(new TextLine(lineStart, end));
                return;
            }
            if (lastSpace > lineStart) {
                // Cut at the last space that fits, and drop it.
                lines.add(new TextLine(lineStart, lastSpace));
                lineStart = lastSpace + 1;
            } else if (i > lineStart) {
                // One word wider than the line: cut it where it stops fitting.
                lines.add(new TextLine(lineStart, i));
                lineStart = i;
            } else {
                // Not even one character fits; take it anyway so the text moves on.
                int next = i + Character.charCount(text.codePointAt(i));
                lines.add(new TextLine(lineStart, next));
                lineStart = next;
            }
        }
    }
}
