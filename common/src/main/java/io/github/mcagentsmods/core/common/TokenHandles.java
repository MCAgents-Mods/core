package io.github.mcagentsmods.core.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Short, safe names for stored credentials, so one can be picked from a list
 * without the list being a list of secrets.
 *
 * <p>A handle looks like {@code #2:a3f9} — the credential's position, and its
 * last four characters. That is enough for an administrator to tell their keys
 * apart and enough for tab completion to be useful, while the value itself never
 * leaves the file.</p>
 *
 * <p>The alternative — completing on the tokens themselves — would push live API
 * keys into every client's suggestion list, where they show up on a stream, a
 * screenshot, or a shared screen. A key is worth money, so the convenience is
 * not worth it.</p>
 *
 * <p>Four characters is the same disclosure a payment card gets, and is far too
 * little to reconstruct a key from.</p>
 */
public final class TokenHandles {

    /**
     * How many trailing characters a handle reveals.
     */
    private static final int REVEALED = 4;

    /**
     * Not instantiable — this class is a pair of pure functions.
     */
    private TokenHandles() {
    }

    /**
     * Builds the handle for a credential at a position.
     *
     * @param index The credential's zero-based position in the list.
     * @param token The credential itself.
     * @return A handle such as {@code #1:a3f9}. A credential shorter than four
     *         characters is not padded — a short string is not a real key, and
     *         padding it would only obscure that.
     */
    public static String of(int index, String token) {
        Objects.requireNonNull(token, "token cannot be null");

        String tail = token.length() <= REVEALED ? token : token.substring(token.length() - REVEALED);
        return "#" + (index + 1) + ":" + tail;
    }

    /**
     * Builds a handle for every credential in a list, in order.
     *
     * @param tokens The credentials, as the store returned them.
     * @return One handle per credential, positions matching the list.
     */
    public static List<String> all(List<String> tokens) {
        Objects.requireNonNull(tokens, "tokens cannot be null");

        List<String> handles = new ArrayList<>(tokens.size());
        for (int i = 0; i < tokens.size(); i++) {
            handles.add(of(i, tokens.get(i)));
        }
        return handles;
    }

    /**
     * Resolves what an administrator typed into the credential it names.
     *
     * <p>Accepts either a handle or the credential itself, because both are
     * reasonable things to type: the handle comes from tab completion, and the
     * full value is what someone has in their clipboard when revoking a key they
     * just found in a provider dashboard.</p>
     *
     * <p>A handle is matched on its position, and the revealed characters are
     * then checked against the credential actually at that position. That guards
     * the one dangerous case: the list shifting between the moment tab
     * completion offered {@code #2:a3f9} and the moment the command ran, which
     * would otherwise delete whatever had moved into slot two.</p>
     *
     * @param tokens The credentials, as the store returned them.
     * @param typed What the administrator supplied.
     * @return The credential to remove, or empty when nothing matches — which
     *         includes a handle whose position no longer holds the credential it
     *         named.
     */
    public static Optional<String> resolve(List<String> tokens, String typed) {
        Objects.requireNonNull(tokens, "tokens cannot be null");
        if (typed == null || typed.isBlank()) {
            return Optional.empty();
        }

        String candidate = typed.trim();
        if (tokens.contains(candidate)) {
            return Optional.of(candidate);
        }
        if (!candidate.startsWith("#")) {
            return Optional.empty();
        }

        int separator = candidate.indexOf(':');
        String positionText = separator < 0 ? candidate.substring(1) : candidate.substring(1, separator);

        int position;
        try {
            position = Integer.parseInt(positionText) - 1;
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
        if (position < 0 || position >= tokens.size()) {
            return Optional.empty();
        }

        // Confirm the handle still describes what is in that slot.
        String actual = tokens.get(position);
        if (separator >= 0 && !of(position, actual).equals(candidate)) {
            return Optional.empty();
        }
        return Optional.of(actual);
    }
}
