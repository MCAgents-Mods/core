package io.github.mcagentsmods.core.client.hologram;

import io.github.mcagentsmods.core.common.hologram.TextBuffer;

/**
 * A box on a hologram the keys go to while {@link TypingScreen} is open: a prompt, a form's
 * field or a form's area. The screen edits the text; the box says what Enter and Tab do.
 */
interface TypingTarget {

    /** @return the text being typed, for the screen to edit */
    TextBuffer buffer();

    /**
     * Inserts typed or pasted text at the caret, as the box allows: a one-line box turns line
     * breaks into spaces.
     */
    void insert(String typed);

    /** Tells the box its text or caret changed, so it shows the caret and passes the text on. */
    void edited();

    /**
     * Enter was pressed.
     *
     * @param shift whether Shift was held
     * @return whether typing stops
     */
    boolean enter(boolean shift);

    /** @return the box Tab moves to, or {@code null} when Tab only stops typing */
    TypingTarget next();

    /** Marks whether the keys come here now, which rings the box and shows the caret. */
    void typing(boolean typing);

    /** @return whether the hologram the box belongs to has closed */
    boolean disposed();

    /** @return the translation key of the line the screen shows while the player types here */
    String hint();
}
