package io.github.mcagentsmods.core.api.hologram;

import java.util.List;
import java.util.Objects;

/**
 * A page of settings or a dialog: a title over rows of text, fields, toggles, steppers and
 * buttons, top to bottom, and up to {@link #MAX_BUTTONS} buttons along the bottom.
 *
 * <p>Each row takes the height its kind needs, and a {@link FormArea} takes whatever is left.
 * Rows that do not fit above the bottom are not drawn, so a form is kept short; a long list
 * belongs in a {@link SelectList} beside it.
 *
 * <p>Core keeps none of the form's values. It reads the rows' suppliers every frame on the
 * client thread, so they must answer from memory at once; the calling mod changes what they
 * return and the form follows. What the player types goes to the rows' callbacks as it is
 * typed.
 *
 * @param title   the title at the top: a translation key, or the text itself
 * @param rows    the rows, top to bottom; at most one {@link FormArea}
 * @param buttons the buttons along the bottom, left to right; at most {@link #MAX_BUTTONS}
 * @param width   how wide the panel is
 */
public record Form(String title, List<FormRow> rows, List<FormButton> buttons, FormWidth width)
        implements HologramPanel {

    /** The most buttons that fit along the bottom. */
    public static final int MAX_BUTTONS = 3;

    public Form {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(width, "width");
        rows = List.copyOf(rows);
        buttons = List.copyOf(buttons);
        if (buttons.size() > MAX_BUTTONS) {
            throw new IllegalArgumentException("A form has at most " + MAX_BUTTONS
                    + " buttons along the bottom, not " + buttons.size());
        }
        if (rows.stream().filter(FormArea.class::isInstance).count() > 1) {
            throw new IllegalArgumentException("A form has at most one area");
        }
    }

    /**
     * A form as wide as the tall panels.
     *
     * @param title   the title at the top
     * @param rows    the rows, top to bottom
     * @param buttons the buttons along the bottom
     */
    public Form(String title, List<FormRow> rows, List<FormButton> buttons) {
        this(title, rows, buttons, FormWidth.NORMAL);
    }

    /** @return this form, half as wide again */
    public Form wide() {
        return new Form(title, rows, buttons, FormWidth.WIDE);
    }
}
