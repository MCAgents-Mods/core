package io.github.mcagentsmods.core.mods.client;

import io.github.mcagentsmods.core.mods.environment.ClientOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Turns credential status lines into something a client chat box can show.
 *
 * <p>Local rendering, and nothing else: no state, no I/O, no game types. It
 * takes the lines the shared command layer produced and wraps them to a width,
 * because a client chat box truncates rather than wraps and a status line that
 * runs off the right edge is a status line nobody read.</p>
 *
 * <h2>Why this is client-only</h2>
 *
 * <p>A dedicated server writes to a console, which wraps on its own and is far
 * wider than any chat box. Wrapping there would mangle output that was fine.
 * The difference is small, and that is the point — a small difference between
 * the sides still belongs on one side, not behind a branch inside shared
 * code.</p>
 *
 * <h2>What it must never do</h2>
 *
 * <p>Reveal a credential. The lines arriving here already carry masked handles
 * rather than tokens; this class only ever splits and indents them, so there is
 * no path by which a token could appear. Do not add one.</p>
 */
@ClientOnly
public final class ClientCredentialDisplay {

    /**
     * How many characters fit on a line of the client's chat box at the default
     * scale and width.
     *
     * <p>An approximation, and deliberately conservative: Minecraft's font is
     * proportional, so no character count is exactly right. Wrapping a little
     * early costs a line; wrapping late costs the end of the sentence.</p>
     */
    public static final int DEFAULT_WIDTH = 50;

    /**
     * What a wrapped continuation is indented with, so it reads as part of the
     * line above rather than as a new entry.
     */
    private static final String CONTINUATION = "  ";

    /**
     * Not instantiable — this class is a formatter.
     */
    private ClientCredentialDisplay() {
    }

    /**
     * Wraps status lines to the default chat width.
     *
     * @param lines The lines to show, as the command layer produced them.
     * @return The lines to draw, in order.
     * @throws NullPointerException When {@code lines} is {@code null}.
     */
    public static List<String> wrap(List<String> lines) {
        return wrap(lines, DEFAULT_WIDTH);
    }

    /**
     * Wraps status lines to a width.
     *
     * <p>Breaks between words where it can. A single word longer than the width
     * — a long file path, most likely — is split rather than left to run off
     * the edge, because seeing most of a path is more useful than seeing the
     * start of one.</p>
     *
     * @param lines The lines to show.
     * @param width The most characters to put on one line. Must be more than
     *              the continuation indent, or there would be no room for
     *              content.
     * @return The lines to draw, in order.
     * @throws NullPointerException When {@code lines} is {@code null}, or holds
     *                             a {@code null}.
     * @throws IllegalArgumentException When the width leaves no room for
     *                                  content.
     */
    public static List<String> wrap(List<String> lines, int width) {
        Objects.requireNonNull(lines, "lines cannot be null");
        if (width <= CONTINUATION.length()) {
            throw new IllegalArgumentException("width must be greater than " + CONTINUATION.length());
        }

        List<String> drawn = new ArrayList<>();
        for (String line : lines) {
            Objects.requireNonNull(line, "lines cannot hold a null");
            wrapOne(line, width, drawn);
        }
        return drawn;
    }

    /**
     * Wraps one line, appending the pieces in order.
     *
     * @param line The line to wrap.
     * @param width The most characters to put on one line.
     * @param target Where to append the pieces.
     */
    private static void wrapOne(String line, int width, List<String> target) {
        if (line.length() <= width) {
            target.add(line);
            return;
        }

        String remaining = line;
        String prefix = "";
        while (true) {
            int room = width - prefix.length();
            if (remaining.length() <= room) {
                target.add(prefix + remaining);
                return;
            }

            int split = remaining.lastIndexOf(' ', room);
            if (split <= 0) {
                // One long word. Splitting mid-word beats losing the tail.
                split = room;
            }

            target.add(prefix + remaining.substring(0, split).stripTrailing());
            remaining = remaining.substring(split).stripLeading();
            prefix = CONTINUATION;

            if (remaining.isEmpty()) {
                return;
            }
        }
    }
}
