package io.github.mcagentsmods.core.api.hologram;

import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A one-line box on a {@link Form} the player types into, under its label.
 *
 * <p>Clicking the box starts typing: the keys go to the box until the player presses Enter or
 * Escape, clicks, or presses Tab for the form's next box. The mod holds the text: core shows
 * {@code value} and hands every edit to {@code onChange} while the player types.
 *
 * <p>A field with {@code choices} has an arrow at its right; the arrow opens the choices as a
 * list over the rows below, and picking one hands it to {@code onPick}. The choices suggest and
 * never limit: the player can still type anything.
 *
 * <p>Build one with {@link #of} and the {@code with} methods:
 * <pre>{@code
 * FormField.of("mymod.key", () -> key, text -> key = text).hidden().withHint("mymod.key.hint")
 * }</pre>
 *
 * @param label     the label over the box: a translation key, or the text itself; blank for
 *                  none, and then the row is only the box
 * @param value     the text in the box, read every frame while the player is not typing in it
 * @param onChange  called with the whole text after every edit, on the client thread
 * @param hint      what the box shows in grey while it is empty, a translation key or the text
 *                  itself; read every frame, and nothing is shown while it gives null
 * @param maxLength the most characters the box takes, 1 to {@link #MAX_LENGTH}
 * @param secret    whether the text is drawn as stars, as for an API key
 * @param choices   what the arrow offers, read when the list opens; empty for no arrow
 * @param onPick    called with a choice the player picks, on the client thread
 * @param editable  whether the player can type into it now, read every frame; the box is
 *                  greyed while they cannot
 */
public record FormField(String label, Supplier<String> value, Consumer<String> onChange,
                        Supplier<String> hint, int maxLength, boolean secret,
                        Supplier<List<String>> choices, Consumer<String> onPick,
                        BooleanSupplier editable) implements FormRow {

    /** The longest {@code maxLength} may be. */
    public static final int MAX_LENGTH = 4_000;

    /** What {@link #of} uses for {@code maxLength}. */
    public static final int DEFAULT_MAX_LENGTH = 256;

    public FormField {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(onChange, "onChange");
        Objects.requireNonNull(hint, "hint");
        Objects.requireNonNull(choices, "choices");
        Objects.requireNonNull(onPick, "onPick");
        Objects.requireNonNull(editable, "editable");
        if (maxLength < 1 || maxLength > MAX_LENGTH) {
            throw new IllegalArgumentException("maxLength must be 1 to " + MAX_LENGTH
                    + ", not " + maxLength);
        }
    }

    /**
     * A plain field of up to {@link #DEFAULT_MAX_LENGTH} characters, with no hint and no
     * choices, that can always be typed into.
     *
     * @param label    the label over the box, or blank for none
     * @param value    the text in the box
     * @param onChange called with the text after every edit
     * @return the field
     */
    public static FormField of(String label, Supplier<String> value, Consumer<String> onChange) {
        return new FormField(label, value, onChange, () -> null, DEFAULT_MAX_LENGTH, false,
                List::of, onChange, () -> true);
    }

    /**
     * @param keyOrText what the empty box shows
     * @return this field with that hint
     */
    public FormField withHint(String keyOrText) {
        Objects.requireNonNull(keyOrText, "keyOrText");
        return withHint(() -> keyOrText);
    }

    /**
     * @param hint what the empty box shows, read every frame
     * @return this field with that hint
     */
    public FormField withHint(Supplier<String> hint) {
        return new FormField(label, value, onChange, hint, maxLength, secret, choices, onPick, editable);
    }

    /**
     * @param length the most characters the box takes
     * @return this field taking at most that many
     */
    public FormField withMaxLength(int length) {
        return new FormField(label, value, onChange, hint, length, secret, choices, onPick, editable);
    }

    /** @return this field, its text drawn as stars */
    public FormField hidden() {
        return new FormField(label, value, onChange, hint, maxLength, true, choices, onPick, editable);
    }

    /**
     * Offers choices from an arrow at the box's right; picking one calls {@code onChange}
     * unless {@link #onPick(Consumer)} says otherwise.
     *
     * @param choices what the arrow offers, read when the list opens
     * @return this field with the choices
     */
    public FormField withChoices(Supplier<List<String>> choices) {
        return new FormField(label, value, onChange, hint, maxLength, secret, choices, onPick, editable);
    }

    /**
     * @param onPick called with a choice the player picks, instead of {@code onChange}
     * @return this field calling it
     */
    public FormField onPick(Consumer<String> onPick) {
        return new FormField(label, value, onChange, hint, maxLength, secret, choices, onPick, editable);
    }

    /**
     * @param editable whether the player can type into the box now, read every frame
     * @return this field, greyed while that answers {@code false}
     */
    public FormField editableWhen(BooleanSupplier editable) {
        return new FormField(label, value, onChange, hint, maxLength, secret, choices, onPick, editable);
    }
}
