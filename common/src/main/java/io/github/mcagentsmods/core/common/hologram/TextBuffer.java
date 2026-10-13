package io.github.mcagentsmods.core.common.hologram;

/**
 * The text being typed into a hologram's prompt, and the caret in it.
 *
 * <p>Typed or pasted text is cleaned on the way in: a carriage return or a carriage return and
 * line feed becomes one line break, a tab becomes a space, and other control characters are
 * dropped. What does not fit under the limit is dropped too, never half a character. The caret
 * moves by whole code points. One instance belongs to one open prompt and is used on the client
 * thread only; nothing here is saved.
 */
public final class TextBuffer {

    private final int maxLength;
    private final StringBuilder text = new StringBuilder();
    private int caret;

    /**
     * An empty buffer.
     *
     * @param maxLength the most characters it holds, at least 1
     */
    public TextBuffer(int maxLength) {
        if (maxLength < 1) {
            throw new IllegalArgumentException("maxLength must be at least 1");
        }
        this.maxLength = maxLength;
    }

    /** @return the text */
    public String text() {
        return text.toString();
    }

    /** @return the caret's index, 0 to {@link #length()} */
    public int caret() {
        return caret;
    }

    /** @return how many characters the text holds */
    public int length() {
        return text.length();
    }

    /** @return the most characters the text may hold */
    public int maxLength() {
        return maxLength;
    }

    /** @return whether the text holds nothing but blanks */
    public boolean isBlank() {
        return text.toString().isBlank();
    }

    /**
     * Inserts at the caret and moves the caret past what was inserted.
     *
     * @param typed what was typed or pasted
     * @return whether anything was inserted
     */
    public boolean insert(String typed) {
        String clean = clean(typed);
        int room = maxLength - text.length();
        if (clean.length() > room) {
            int cut = room;
            if (cut > 0 && Character.isHighSurrogate(clean.charAt(cut - 1))) {
                cut--;
            }
            clean = clean.substring(0, cut);
        }
        if (clean.isEmpty()) {
            return false;
        }
        text.insert(caret, clean);
        caret += clean.length();
        return true;
    }

    /**
     * Deletes the character before the caret.
     *
     * @return whether one was deleted
     */
    public boolean backspace() {
        if (caret == 0) {
            return false;
        }
        int before = text.offsetByCodePoints(caret, -1);
        text.delete(before, caret);
        caret = before;
        return true;
    }

    /**
     * Deletes the character after the caret.
     *
     * @return whether one was deleted
     */
    public boolean delete() {
        if (caret == text.length()) {
            return false;
        }
        text.delete(caret, text.offsetByCodePoints(caret, 1));
        return true;
    }

    /** Moves the caret one character left, if it can. */
    public void left() {
        if (caret > 0) {
            caret = text.offsetByCodePoints(caret, -1);
        }
    }

    /** Moves the caret one character right, if it can. */
    public void right() {
        if (caret < text.length()) {
            caret = text.offsetByCodePoints(caret, 1);
        }
    }

    /** Moves the caret to the start of its line, the one the last line break began. */
    public void home() {
        caret = text.lastIndexOf("\n", caret - 1) + 1;
    }

    /** Moves the caret to the end of its line, before the next line break. */
    public void end() {
        int lineBreak = text.indexOf("\n", caret);
        caret = lineBreak < 0 ? text.length() : lineBreak;
    }

    /**
     * Moves the caret to an index, kept inside the text and off the middle of a character.
     *
     * @param index where to
     */
    public void moveTo(int index) {
        int at = Math.max(0, Math.min(text.length(), index));
        if (at > 0 && at < text.length() && Character.isLowSurrogate(text.charAt(at))
                && Character.isHighSurrogate(text.charAt(at - 1))) {
            at--;
        }
        caret = at;
    }

    /** Empties the text. */
    public void clear() {
        text.setLength(0);
        caret = 0;
    }

    private static String clean(String typed) {
        StringBuilder out = new StringBuilder(typed.length());
        for (int i = 0; i < typed.length(); i++) {
            char c = typed.charAt(i);
            if (c == '\r') {
                out.append('\n');
                if (i + 1 < typed.length() && typed.charAt(i + 1) == '\n') {
                    i++;
                }
            } else if (c == '\t') {
                out.append(' ');
            } else if (c == '\n' || !Character.isISOControl(c)) {
                out.append(c);
            }
        }
        return out.toString();
    }
}
