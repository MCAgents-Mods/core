package io.github.mcagentsmods.core.common.hologram;

/**
 * One line of wrapped text, as the characters {@code start} (inclusive) to {@code end}
 * (exclusive) of the text it was cut from. The space or line break a line was cut at belongs to
 * neither line.
 *
 * @param start the first character's index
 * @param end   the index after the last character
 */
public record TextLine(int start, int end) {

    public TextLine {
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("A line runs from " + start + " to " + end);
        }
    }

    /**
     * @param text the text the line was cut from
     * @return the line's characters
     */
    public String of(String text) {
        return text.substring(start, end);
    }
}
