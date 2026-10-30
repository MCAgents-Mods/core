package io.github.mcagentsmods.core.client.hologram;

import io.github.mcagentsmods.core.api.hologram.Form;
import io.github.mcagentsmods.core.api.hologram.FormArea;
import io.github.mcagentsmods.core.api.hologram.FormButton;
import io.github.mcagentsmods.core.api.hologram.FormButtons;
import io.github.mcagentsmods.core.api.hologram.FormField;
import io.github.mcagentsmods.core.api.hologram.FormRow;
import io.github.mcagentsmods.core.api.hologram.FormStepper;
import io.github.mcagentsmods.core.api.hologram.FormText;
import io.github.mcagentsmods.core.api.hologram.FormTone;
import io.github.mcagentsmods.core.api.hologram.FormToggle;
import io.github.mcagentsmods.core.api.hologram.FormWidth;
import io.github.mcagentsmods.core.common.hologram.ColumnLayout;
import io.github.mcagentsmods.core.common.hologram.FormLayout;
import io.github.mcagentsmods.core.common.hologram.PanelArea;
import io.github.mcagentsmods.core.common.hologram.ScrollWindow;
import io.github.mcagentsmods.core.common.hologram.TextBuffer;
import io.github.mcagentsmods.core.common.hologram.TextLine;
import io.github.mcagentsmods.core.common.hologram.TextWrap;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A {@link Form}: the title band, the rows stacked under it, and the buttons along the bottom.
 * A field's choices open as a list over the rows below it; the keys reach a field or the area
 * through {@link TypingScreen}, which a click on the box opens.
 */
final class FormPanelView extends ColumnPanelView {

    /** Lines the mouse wheel moves per notch in an area. */
    private static final int SCROLL_LINES = 3;

    /** The line shown over the world while the player types into a field. */
    private static final String TYPING_FIELD = "hologram.mcagents_core.typing.field";

    /** The line shown over the world while the player types into an area. */
    private static final String TYPING_AREA = "hologram.mcagents_core.typing.area";

    private static final String ON = "hologram.mcagents_core.on";
    private static final String OFF = "hologram.mcagents_core.off";

    /** What a field with choices draws in its arrow. */
    private static final String ARROW = "▼";

    private final Form spec;
    private final Actions actions;
    private final Consumer<TypingTarget> startTyping;
    private final int width;
    private final boolean footer;
    private final List<RowView> rows = new ArrayList<>();

    /** The boxes the player can type into, in order, for Tab. */
    private final List<TypingTarget> boxes = new ArrayList<>();

    /** The field whose choices are open, or null. */
    private FieldRow listOpen;
    /** Its open list, set while {@link #listOpen} is. */
    private ChoiceList list;

    private boolean disposed;

    FormPanelView(Form spec, Actions actions, Consumer<TypingTarget> startTyping) {
        super(spec.title());
        this.spec = spec;
        this.actions = actions;
        this.startTyping = startTyping;
        this.width = FormLayout.width(spec.width() == FormWidth.WIDE);
        this.footer = !spec.buttons().isEmpty();
        int[] heights = new int[spec.rows().size()];
        for (int i = 0; i < heights.length; i++) {
            heights[i] = height(spec.rows().get(i));
        }
        List<PanelArea> areas = FormLayout.stack(width, heights, footer);
        for (int i = 0; i < heights.length; i++) {
            PanelArea area = areas.get(i);
            if (!FormLayout.fits(area, footer)) {
                // Rows that do not fit are left out, as the form's contract says.
                break;
            }
            RowView row = row(spec.rows().get(i), area);
            rows.add(row);
            if (row instanceof TypingTarget box) {
                boxes.add(box);
            }
        }
    }

    @Override
    int width() {
        return width;
    }

    private static int height(FormRow row) {
        return switch (row) {
            case FormText text -> text.lines() * FormLayout.LINE;
            case FormField field -> field.label().isBlank() ? FormLayout.BOX : FormLayout.LABELLED_BOX;
            case FormArea area -> 0;
            case FormToggle toggle -> FormLayout.BUTTON;
            case FormStepper stepper -> FormLayout.BUTTON;
            case FormButtons buttons -> FormLayout.BUTTON;
        };
    }

    private RowView row(FormRow row, PanelArea area) {
        return switch (row) {
            case FormText text -> new TextRow(text, area);
            case FormField field -> new FieldRow(field, area);
            case FormArea box -> new AreaRow(box, area);
            case FormToggle toggle -> new ToggleRow(toggle, area);
            case FormStepper stepper -> new StepperRow(stepper, area);
            case FormButtons buttons -> new ButtonsRow(buttons.buttons(), area);
        };
    }

    @Override
    void draw(HologramPainter painter, double u, double v) {
        drawColumn(painter);
        boolean overList = listOpen != null && list.contains(u, v);
        double rowU = overList ? Double.NaN : u;
        double rowV = overList ? Double.NaN : v;
        for (RowView row : rows) {
            row.draw(painter, rowU, rowV);
        }
        List<FormButton> buttons = spec.buttons();
        for (int i = 0; i < buttons.size(); i++) {
            PanelArea area = FormLayout.footerButton(width, i, buttons.size());
            drawFormButton(painter, buttons.get(i), area, rowU, rowV);
        }
        if (listOpen != null) {
            list.draw(painter, u, v, read(listOpen.spec.value(), ""));
        }
    }

    @Override
    boolean click(double u, double v) {
        if (listOpen != null) {
            FieldRow field = listOpen;
            if (list.contains(u, v)) {
                pick(list.click(u, v));
                return true;
            }
            closeList();
            if (FormLayout.arrow(field.box).contains(u, v)) {
                // The arrow that opened the list closes it.
                return true;
            }
        }
        List<FormButton> buttons = spec.buttons();
        for (int i = 0; i < buttons.size(); i++) {
            if (FormLayout.footerButton(width, i, buttons.size()).contains(u, v)) {
                press(buttons.get(i));
                return true;
            }
        }
        for (RowView row : rows) {
            if (row.area.contains(u, v)) {
                return row.click(u, v);
            }
        }
        return false;
    }

    @Override
    void scroll(double u, double v, int amount) {
        if (listOpen != null && list.contains(u, v)) {
            list.scroll(amount);
            return;
        }
        for (RowView row : rows) {
            if (row.area.contains(u, v)) {
                row.scroll(amount);
                return;
            }
        }
    }

    @Override
    void dispose() {
        disposed = true;
    }

    private void press(FormButton button) {
        if (readBoolean(button.enabled(), false)) {
            actions.run("The form button " + button.label(), button.action());
        }
    }

    private void drawFormButton(HologramPainter painter, FormButton button, PanelArea area,
                                double u, double v) {
        boolean enabled = readBoolean(button.enabled(), false);
        drawButton(painter, area, label(button.label()), enabled && area.contains(u, v), enabled);
    }

    /**
     * The next box after {@code box} that takes typing now, going round, with its text loaded
     * for typing; null for none.
     */
    private TypingTarget after(TypingTarget box) {
        int at = boxes.indexOf(box);
        for (int step = 1; step < boxes.size(); step++) {
            TypingTarget next = boxes.get((at + step) % boxes.size());
            if (next instanceof FieldRow field && field.editable()) {
                field.load();
                return field;
            }
            if (next instanceof AreaRow area) {
                area.load();
                return area;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ choices

    private void openList(FieldRow field) {
        list = ChoiceList.open(field.box, read(field.spec.choices(), List.of()),
                read(field.spec.value(), ""));
        listOpen = list == null ? null : field;
    }

    private void closeList() {
        listOpen = null;
        list = null;
    }

    /** Closes the list and hands the choice to its field; a null choice keeps the list open. */
    private void pick(String choice) {
        if (choice == null) {
            return;
        }
        FieldRow field = listOpen;
        closeList();
        actions.run("Picking a choice in " + title(), () -> field.spec.onPick().accept(choice));
    }

    // --------------------------------------------------------------------- rows

    private static int colour(FormTone tone) {
        return switch (tone) {
            case TEXT -> HologramStyle.TEXT;
            case SOFT -> HologramStyle.TEXT_SOFT;
            case GOOD -> HologramStyle.GOOD;
            case BAD -> HologramStyle.BAD;
            case WARN -> HologramStyle.WARN;
        };
    }

    /** Draws a sunken one-line box, greyed while it cannot be typed into. */
    private static void drawFieldBox(HologramPainter painter, PanelArea box, boolean focused,
                                     boolean enabled) {
        drawBox(painter, box, focused);
        if (!enabled) {
            painter.rect((float) box.left() + 1, (float) box.top() + 1, (float) box.right() - 1,
                    (float) box.bottom() - 1, 3, HologramStyle.DISABLED);
        }
    }

    /** One row, drawn and clicked inside its area. */
    private abstract static class RowView {

        final PanelArea area;

        RowView(PanelArea area) {
            this.area = area;
        }

        abstract void draw(HologramPainter painter, double u, double v);

        /** @return whether the click did something */
        abstract boolean click(double u, double v);

        void scroll(int amount) {
        }
    }

    private final class TextRow extends RowView {

        private final FormText spec;
        private String wrapped;
        private List<String> lines = List.of();

        TextRow(FormText spec, PanelArea area) {
            super(area);
            this.spec = spec;
        }

        @Override
        void draw(HologramPainter painter, double u, double v) {
            String raw = read(spec.text(), "");
            String text = raw.isBlank() ? "" : translate(raw);
            if (!text.equals(wrapped)) {
                lines = wrap(painter, text);
                wrapped = text;
            }
            int colour = colour(read(spec.tone(), FormTone.TEXT));
            for (int i = 0; i < lines.size(); i++) {
                painter.plainText(lines.get(i), (float) area.left(),
                        (float) area.top() + i * FormLayout.LINE + 1, 6, colour);
            }
        }

        /** The text's lines, at most the row's; the last ends in three dots when cut. */
        private List<String> wrap(HologramPainter painter, String text) {
            if (text.isEmpty()) {
                return List.of();
            }
            float room = (float) area.width();
            List<TextLine> all = TextWrap.wrap(text, (int) room, painter::advance);
            List<String> out = new ArrayList<>();
            for (int i = 0; i < all.size() && i < spec.lines(); i++) {
                String line = all.get(i).of(text);
                if (i == spec.lines() - 1 && all.size() > spec.lines()) {
                    line = painter.fit(line + " " + all.get(i + 1).of(text), room);
                }
                out.add(line);
            }
            return out;
        }

        @Override
        boolean click(double u, double v) {
            return false;
        }
    }

    private final class FieldRow extends RowView implements TypingTarget {

        private final FormField spec;
        private final PanelArea box;
        private TextBuffer buffer;
        private boolean typing;

        FieldRow(FormField spec, PanelArea area) {
            super(area);
            this.spec = spec;
            this.box = FormLayout.box(area);
        }

        boolean editable() {
            return readBoolean(spec.editable(), false);
        }

        private boolean hasChoices() {
            return !read(spec.choices(), List.of()).isEmpty();
        }

        @Override
        void draw(HologramPainter painter, double u, double v) {
            if (!spec.label().isBlank()) {
                painter.plainText(painter.fit(translate(spec.label()), (float) area.width()),
                        (float) area.left(), (float) area.top() + 1, 6, HologramStyle.TEXT);
            }
            boolean editable = editable();
            boolean arrow = hasChoices();
            drawFieldBox(painter, box, typing, editable);
            PanelArea text = FormLayout.text(box, arrow);
            float room = (float) text.width();
            float left = (float) text.left();
            float top = (float) text.top();
            String value = typing ? buffer.text() : read(spec.value(), "");
            String shown = spec.secret() ? "*".repeat(value.codePointCount(0, value.length())) : value;
            if (value.isEmpty() && !typing) {
                String hint = read(spec.hint(), null);
                if (hint != null && !hint.isBlank()) {
                    painter.plainText(painter.fit(translate(hint), room), left, top, 6,
                            HologramStyle.TEXT_SOFT);
                }
            } else if (typing) {
                int caret = spec.secret() ? value.codePointCount(0, buffer.caret()) : buffer.caret();
                int start = startFor(painter, shown, caret, room);
                String visible = cut(painter, shown.substring(start), room);
                painter.plainText(visible, left, top, 6, HologramStyle.TEXT);
                if (Util.getMillis() / 500 % 2 == 0) {
                    float x = left + painter.plainWidth(shown.substring(start, caret));
                    painter.rect(x, top - 1, x + 1, top + 8, 7, HologramStyle.CARET);
                }
            } else {
                painter.plainText(painter.fit(shown, room), left, top, 6,
                        editable ? HologramStyle.TEXT : HologramStyle.TEXT_SOFT);
            }
            if (arrow) {
                PanelArea area = FormLayout.arrow(box);
                boolean hover = editable && area.contains(u, v);
                painter.rect((float) area.left(), (float) area.top(), (float) area.right(),
                        (float) area.bottom(), 4, hover ? HologramStyle.BUTTON_HOVER : HologramStyle.BUTTON);
                painter.rect((float) area.left(), (float) area.top(), (float) area.left() + 1,
                        (float) area.bottom(), 5, HologramStyle.BUTTON_EDGE);
                painter.centred(ARROW, (float) (area.left() + area.right()) / 2 + 0.5f,
                        (float) area.top() + 2.5f, 6, 0.75f,
                        editable ? HologramStyle.TEXT : HologramStyle.TEXT_FADED, 0);
            }
        }

        @Override
        boolean click(double u, double v) {
            if (!editable() || !box.contains(u, v)) {
                return false;
            }
            if (hasChoices() && FormLayout.arrow(box).contains(u, v)) {
                openList(this);
                return true;
            }
            load();
            startTyping.accept(this);
            return true;
        }

        void load() {
            buffer = new TextBuffer(spec.maxLength());
            buffer.insert(read(spec.value(), ""));
        }

        @Override
        public TextBuffer buffer() {
            return buffer;
        }

        @Override
        public void insert(String typed) {
            buffer.insert(typed.replace("\r\n", " ").replace('\r', ' ').replace('\n', ' '));
        }

        @Override
        public void edited() {
            String text = buffer.text();
            actions.run("Typing into " + spec.label(), () -> spec.onChange().accept(text));
        }

        @Override
        public boolean enter(boolean shift) {
            return true;
        }

        @Override
        public TypingTarget next() {
            return after(this);
        }

        @Override
        public void typing(boolean typing) {
            this.typing = typing;
        }

        @Override
        public boolean disposed() {
            return disposed;
        }

        @Override
        public String hint() {
            return TYPING_FIELD;
        }
    }

    /** Where to start drawing a one-line text so the caret stays inside {@code room}. */
    private static int startFor(HologramPainter painter, String text, int caret, float room) {
        if (painter.plainWidth(text.substring(0, caret)) <= room - 1) {
            return 0;
        }
        int start = caret;
        float used = 1;
        while (start > 0) {
            int codePoint = text.codePointBefore(start);
            float advance = painter.advance(codePoint);
            if (used + advance > room) {
                break;
            }
            used += advance;
            start -= Character.charCount(codePoint);
        }
        return start;
    }

    /** The longest start of {@code text} that fits {@code room}, with no dots. */
    private static String cut(HologramPainter painter, String text, float room) {
        int end = 0;
        float used = 0;
        while (end < text.length()) {
            int codePoint = text.codePointAt(end);
            used += painter.advance(codePoint);
            if (used > room) {
                break;
            }
            end += Character.charCount(codePoint);
        }
        return text.substring(0, end);
    }

    private final class AreaRow extends RowView implements TypingTarget {

        private final FormArea spec;
        private final ScrollWindow window;
        private TextBuffer buffer;
        private boolean typing;
        private boolean caretMoved;
        private String wrapped;
        private List<TextLine> lines = List.of();

        AreaRow(FormArea spec, PanelArea area) {
            super(area);
            this.spec = spec;
            this.window = new ScrollWindow(ColumnLayout.capacity(area, ColumnLayout.LINE_HEIGHT), false);
        }

        void load() {
            buffer = new TextBuffer(spec.maxLength());
            buffer.insert(read(spec.value(), ""));
        }

        @Override
        void draw(HologramPainter painter, double u, double v) {
            PanelArea content = ColumnLayout.content(area);
            String text = typing ? buffer.text() : read(spec.value(), "");
            if (!text.equals(wrapped)) {
                lines = TextWrap.wrap(text, (int) content.width() - 1, painter::advance);
                wrapped = text;
                window.setTotal(lines.size());
            }
            int caretLine = typing ? TextWrap.lineAt(lines, buffer.caret()) : -1;
            if (typing && caretMoved) {
                window.reveal(caretLine);
                caretMoved = false;
            }
            drawBox(painter, area, typing);
            float left = (float) content.left();
            if (text.isEmpty() && !typing) {
                painter.plainText(painter.fit(translate(spec.hint()), (float) content.width()), left,
                        (float) content.top() + 1, 6, HologramStyle.TEXT_SOFT);
            }
            for (int slot = 0; slot < window.visible(); slot++) {
                int index = window.offset() + slot;
                if (index >= lines.size()) {
                    break;
                }
                float top = (float) content.top() + slot * ColumnLayout.LINE_HEIGHT + 1;
                TextLine line = lines.get(index);
                painter.plainText(line.of(text), left, top, 6, HologramStyle.TEXT);
                if (index == caretLine && Util.getMillis() / 500 % 2 == 0) {
                    int end = Math.min(buffer.caret(), line.end());
                    float x = left + painter.plainWidth(text.substring(line.start(), end));
                    painter.rect(x, top - 1, x + 1, top + 8, 7, HologramStyle.CARET);
                }
            }
            drawScrollbar(painter, area, window, u, v);
        }

        @Override
        boolean click(double u, double v) {
            if (clickScrollbar(area, window, u, v)) {
                return true;
            }
            load();
            startTyping.accept(this);
            return true;
        }

        @Override
        void scroll(int amount) {
            window.scrollBy(amount * SCROLL_LINES);
        }

        @Override
        public TextBuffer buffer() {
            return buffer;
        }

        @Override
        public void insert(String typed) {
            buffer.insert(typed);
        }

        @Override
        public void edited() {
            caretMoved = true;
            String text = buffer.text();
            actions.run("Typing into " + title(), () -> spec.onChange().accept(text));
        }

        @Override
        public boolean enter(boolean shift) {
            buffer.insert("\n");
            edited();
            return false;
        }

        @Override
        public TypingTarget next() {
            return after(this);
        }

        @Override
        public void typing(boolean typing) {
            this.typing = typing;
            caretMoved = true;
        }

        @Override
        public boolean disposed() {
            return disposed;
        }

        @Override
        public String hint() {
            return TYPING_AREA;
        }
    }

    private final class ToggleRow extends RowView {

        private final FormToggle spec;

        ToggleRow(FormToggle spec, PanelArea area) {
            super(area);
            this.spec = spec;
        }

        @Override
        void draw(HologramPainter painter, double u, double v) {
            boolean on = readBoolean(spec.on(), false);
            drawButton(painter, area, "", area.contains(u, v), true);
            String name = label(spec.label()) + ": ";
            String state = label(on ? ON : OFF);
            float total = painter.width(name) + painter.width(state);
            float scale = Math.max(0.5f, Math.min(1, (float) (area.width() - 5) / total));
            float x = (float) (area.left() + area.right()) / 2 - total * scale / 2;
            float y = (float) area.top() + ((float) area.height() - 8 * scale) / 2 + 0.5f;
            painter.text(name, x, y, 6, scale, HologramStyle.TEXT, 0);
            painter.text(state, x + painter.width(name) * scale, y, 6, scale,
                    on ? HologramStyle.GOOD : HologramStyle.BAD, 0);
        }

        @Override
        boolean click(double u, double v) {
            actions.run("The toggle " + spec.label(), spec.flip());
            return true;
        }
    }

    private final class StepperRow extends RowView {

        private final FormStepper spec;

        StepperRow(FormStepper spec, PanelArea area) {
            super(area);
            this.spec = spec;
        }

        @Override
        void draw(HologramPainter painter, double u, double v) {
            PanelArea minus = FormLayout.minus(area);
            PanelArea number = FormLayout.number(area);
            PanelArea plus = FormLayout.plus(area);
            float room = (float) (minus.left() - area.left() - 4);
            float textTop = (float) area.top() + 4;
            painter.plainText(painter.fit(translate(spec.label()), room), (float) area.left(), textTop, 6,
                    HologramStyle.TEXT);
            drawButton(painter, minus, "-", minus.contains(u, v), true);
            drawButton(painter, plus, "+", plus.contains(u, v), true);
            String value = Integer.toString(readInt(spec.value(), 0));
            painter.centred(value, (float) (number.left() + number.right()) / 2, textTop, 6, 1,
                    HologramStyle.TEXT, 0);
        }

        @Override
        boolean click(double u, double v) {
            if (FormLayout.minus(area).contains(u, v)) {
                actions.run("Stepping " + spec.label() + " down", () -> spec.step().accept(-1));
                return true;
            }
            if (FormLayout.plus(area).contains(u, v)) {
                actions.run("Stepping " + spec.label() + " up", () -> spec.step().accept(1));
                return true;
            }
            return false;
        }
    }

    private final class ButtonsRow extends RowView {

        private final List<FormButton> buttons;

        ButtonsRow(List<FormButton> buttons, PanelArea area) {
            super(area);
            this.buttons = buttons;
        }

        @Override
        void draw(HologramPainter painter, double u, double v) {
            for (int i = 0; i < buttons.size(); i++) {
                drawFormButton(painter, buttons.get(i), FormLayout.share(area, i, buttons.size()), u, v);
            }
        }

        @Override
        boolean click(double u, double v) {
            for (int i = 0; i < buttons.size(); i++) {
                if (FormLayout.share(area, i, buttons.size()).contains(u, v)) {
                    press(buttons.get(i));
                    return true;
                }
            }
            return false;
        }
    }
}
