package io.github.mcagentsmods.core.client.hologram;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.TextInputManager;
import io.github.mcagentsmods.core.common.hologram.TextBuffer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Catches the keys while the player types into a box on a hologram (a prompt, or a form's field
 * or area) and draws nothing over the world but one line saying how to stop. The text itself
 * shows on the hologram.
 *
 * <p>The box decides what Enter does: a prompt sends, a field stops, an area starts a new line.
 * Tab moves to the form's next box. Escape or any click stops typing and keeps the text. The
 * screen also closes on its own when the hologram does.
 *
 * <p>The game hands typed characters only to a screen that asks for them: while this one is
 * open it holds the game's text input, as the sign editor does, and the keys alone would
 * otherwise reach it without a single letter.
 */
final class TypingScreen extends Screen {

    /** How far down the screen the hint line sits. */
    private static final int HINT_TOP = 12;

    private final TypingTarget target;

    TypingScreen(TypingTarget target) {
        super(Component.translatable(target.hint()));
        this.target = target;
    }

    @Override
    protected void init() {
        // Run again when the window is resized, which moves the hint line; asking twice is harmless.
        TextInputManager input = minecraft.textInputManager();
        input.startTextInput(this);
        // An input method's candidates open by the hint line: the box is in the world, not here.
        int textWidth = font.width(title);
        input.setTextInputArea((width - textWidth) / 2, HINT_TOP, textWidth, font.lineHeight);
        target.typing(true);
    }

    @Override
    public void removed() {
        minecraft.textInputManager().stopTextInput(this);
        target.typing(false);
    }

    @Override
    public void tick() {
        if (target.disposed()) {
            onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        TextBuffer buffer = target.buffer();
        int key = event.key();
        if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
            if (target.enter(event.hasShiftDown())) {
                onClose();
            }
            return true;
        }
        if (key == InputConstants.KEY_TAB) {
            TypingTarget next = target.next();
            if (next == null) {
                onClose();
            } else {
                // Replacing this screen stops its text input before the next one starts it.
                minecraft.setScreenAndShow(new TypingScreen(next));
            }
            return true;
        }
        if (event.isPaste()) {
            target.insert(minecraft.keyboardHandler.getClipboard());
            target.edited();
            return true;
        }
        boolean handled = true;
        switch (key) {
            case InputConstants.KEY_BACKSPACE -> {
                if (event.hasControlDown()) {
                    buffer.backspaceLine();
                } else {
                    buffer.backspace();
                }
            }
            case InputConstants.KEY_DELETE -> buffer.delete();
            case InputConstants.KEY_LEFT -> buffer.left();
            case InputConstants.KEY_RIGHT -> buffer.right();
            case InputConstants.KEY_HOME -> buffer.home();
            case InputConstants.KEY_END -> buffer.end();
            default -> handled = false;
        }
        if (handled) {
            target.edited();
            return true;
        }
        // Escape closes the screen; every other key is swallowed so it moves nothing in game.
        super.keyPressed(event);
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (event.isAllowedChatCharacter()) {
            target.insert(event.codepointAsString());
            target.edited();
        }
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        onClose();
        return true;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Nothing: the world and the hologram stay in plain view.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // At the top, on a dark backdrop, clear of the hologram and the hotbar.
        int textWidth = font.width(title);
        graphics.textWithBackdrop(font, title, (width - textWidth) / 2, HINT_TOP, textWidth, 0xFFFFFFFF);
    }
}
