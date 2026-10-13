package io.github.mcagentsmods.core.client.hologram;

import io.github.mcagentsmods.core.api.hologram.PromptInput;
import io.github.mcagentsmods.core.common.hologram.ColumnLayout;
import io.github.mcagentsmods.core.common.hologram.PanelArea;
import io.github.mcagentsmods.core.common.hologram.ScrollWindow;
import io.github.mcagentsmods.core.common.hologram.TextBuffer;
import io.github.mcagentsmods.core.common.hologram.TextLine;
import io.github.mcagentsmods.core.common.hologram.TextWrap;
import net.minecraft.util.Util;

import java.util.List;
import java.util.function.Consumer;

/**
 * A {@link PromptInput}: the box the player types into and the send button. The keys reach it
 * through {@link TypingScreen}, which a click on the box opens.
 */
final class PromptPanelView extends ColumnPanelView {

    /** Lines the mouse wheel moves per notch. */
    private static final int SCROLL_LINES = 3;

    /** What the empty box says until the player clicks it. */
    private static final String HINT = "hologram.mcagents_core.prompt.hint";

    private final PromptInput spec;
    private final Actions actions;
    private final Consumer<PromptPanelView> startTyping;
    private final TextBuffer buffer;
    private final PanelArea box = ColumnLayout.box(true);
    private final ScrollWindow window;
    private String wrapped;
    private List<TextLine> lines = List.of();
    private boolean caretMoved;
    private boolean typing;
    private boolean disposed;

    PromptPanelView(PromptInput spec, Actions actions, Consumer<PromptPanelView> startTyping) {
        super(spec.title());
        this.spec = spec;
        this.actions = actions;
        this.startTyping = startTyping;
        this.buffer = new TextBuffer(spec.maxLength());
        this.window = new ScrollWindow(ColumnLayout.capacity(box, ColumnLayout.LINE_HEIGHT), false);
    }

    @Override
    void draw(HologramPainter painter, double u, double v) {
        PanelArea content = ColumnLayout.content(box);
        String text = buffer.text();
        if (!text.equals(wrapped)) {
            // One pixel short of the box, so the caret after a full line still shows.
            lines = TextWrap.wrap(text, (int) content.width() - 1, painter::advance);
            wrapped = text;
            window.setTotal(lines.size());
        }
        int caretLine = TextWrap.lineAt(lines, buffer.caret());
        if (caretMoved) {
            window.reveal(caretLine);
            caretMoved = false;
        }

        drawColumn(painter);
        drawBox(painter, box, typing);
        float left = (float) content.left();
        if (text.isEmpty() && !typing) {
            painter.plainText(painter.fit(translate(HINT), (float) content.width()), left,
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
            if (typing && index == caretLine && Util.getMillis() / 500 % 2 == 0) {
                int end = Math.min(buffer.caret(), line.end());
                float x = left + painter.plainWidth(text.substring(line.start(), end));
                painter.rect(x, top - 1, x + 1, top + 8, 7, HologramStyle.CARET);
            }
        }
        drawScrollbar(painter, box, window, u, v);

        boolean ready = canSend();
        PanelArea send = ColumnLayout.sendButton();
        drawButton(painter, send, label(spec.send()), ready && send.contains(u, v), ready);
    }

    @Override
    boolean click(double u, double v) {
        if (ColumnLayout.sendButton().contains(u, v)) {
            send();
            return true;
        }
        if (clickScrollbar(box, window, u, v)) {
            return true;
        }
        if (box.contains(u, v)) {
            startTyping.accept(this);
            return true;
        }
        return false;
    }

    @Override
    void scroll(double u, double v, int rows) {
        window.scrollBy(rows * SCROLL_LINES);
    }

    @Override
    void dispose() {
        disposed = true;
    }

    /** @return whether the hologram this box belongs to has closed */
    boolean disposed() {
        return disposed;
    }

    /** Marks whether the keys come here now, which rings the box and shows the caret. */
    void typing(boolean typing) {
        this.typing = typing;
        caretMoved = true;
    }

    /**
     * Hands the text to the mod and empties the box, unless it holds only blanks or the mod is
     * not ready. When the mod's callback throws, the text stays.
     *
     * @return whether the text was sent
     */
    boolean send() {
        if (!canSend()) {
            return false;
        }
        String text = buffer.text();
        if (!actions.run("The prompt " + title(), () -> spec.onSend().accept(text))) {
            return false;
        }
        buffer.clear();
        caretMoved = true;
        return true;
    }

    /** @return the text being typed, for {@link TypingScreen} to edit */
    TextBuffer buffer() {
        return buffer;
    }

    /** Keeps the caret in view after {@link TypingScreen} moves it. */
    void caretMoved() {
        caretMoved = true;
    }

    private boolean canSend() {
        return !buffer.isBlank() && readBoolean(spec.ready(), false);
    }
}
