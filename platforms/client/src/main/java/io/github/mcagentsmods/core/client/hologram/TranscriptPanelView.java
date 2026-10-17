package io.github.mcagentsmods.core.client.hologram;

import io.github.mcagentsmods.core.api.hologram.Transcript;
import io.github.mcagentsmods.core.api.hologram.TranscriptEntry;
import io.github.mcagentsmods.core.common.hologram.ColumnLayout;
import io.github.mcagentsmods.core.common.hologram.PanelArea;
import io.github.mcagentsmods.core.common.hologram.ScrollWindow;
import io.github.mcagentsmods.core.common.hologram.TextLine;
import io.github.mcagentsmods.core.common.hologram.TextWrap;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A {@link Transcript}: each entry's speaker on a line of its own and its text wrapped under
 * it, scrolling, and kept at the bottom while the player has not scrolled up; the buttons, if
 * any, under it.
 */
final class TranscriptPanelView extends ColumnPanelView {

    /** Lines the mouse wheel moves per notch. */
    private static final int SCROLL_LINES = 3;

    private final Transcript spec;
    private final Actions actions;
    private final PanelArea box;
    private final ScrollWindow window;
    private List<TranscriptEntry> shown;
    private List<String> lines = List.of();

    TranscriptPanelView(Transcript spec, Actions actions) {
        super(spec.title());
        this.spec = spec;
        this.actions = actions;
        this.box = ColumnLayout.box(!spec.buttons().isEmpty());
        this.window = new ScrollWindow(ColumnLayout.capacity(box, ColumnLayout.LINE_HEIGHT), true);
    }

    @Override
    void draw(HologramPainter painter, double u, double v) {
        PanelArea content = ColumnLayout.content(box);
        List<TranscriptEntry> entries = read(spec.entries(), List.of());
        if (!entries.equals(shown)) {
            // Wrapped again only when the conversation changes, never every frame. The copy keeps
            // a list the mod changes in place from looking unchanged.
            shown = entries.stream().filter(Objects::nonNull).toList();
            lines = wrap(shown, painter, (int) content.width());
            window.setTotal(lines.size());
        }

        drawColumn(painter);
        drawBox(painter, box, false);
        for (int slot = 0; slot < window.visible(); slot++) {
            int index = window.offset() + slot;
            if (index >= lines.size()) {
                break;
            }
            float top = (float) content.top() + slot * ColumnLayout.LINE_HEIGHT;
            painter.plainText(lines.get(index), (float) content.left(), top + 1, 6, HologramStyle.TEXT);
        }
        drawScrollbar(painter, box, window, u, v);
        drawFooter(painter, spec.buttons(), u, v);
    }

    @Override
    String heading() {
        String heading = read(spec.heading(), null);
        return heading == null || heading.isBlank() ? title() : heading;
    }

    private static List<String> wrap(List<TranscriptEntry> entries, HologramPainter painter, int width) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            TranscriptEntry entry = entries.get(i);
            if (i > 0) {
                out.add("");
            }
            out.add(painter.fit("<" + entry.speaker() + ">", width));
            String text = entry.text();
            for (TextLine line : TextWrap.wrap(text, width, painter::advance)) {
                out.add(line.of(text));
            }
        }
        return out;
    }

    @Override
    boolean click(double u, double v) {
        return clickFooter(spec.buttons(), actions, u, v) || clickScrollbar(box, window, u, v);
    }

    @Override
    void scroll(double u, double v, int rows) {
        window.scrollBy(rows * SCROLL_LINES);
    }
}
