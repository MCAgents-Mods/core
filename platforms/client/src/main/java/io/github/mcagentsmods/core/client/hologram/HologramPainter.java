package io.github.mcagentsmods.core.client.hologram;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws one panel in panel pixels: x to the right and y down from the panel's top left
 * corner, with z towards the player. Rectangles are gathered and sent to the game in one
 * piece by {@link #flush()}; text goes straight away.
 *
 * <p>Depth is a layer number rather than a distance: each layer lifts what it draws a little
 * further off the panel, so a button sits on the face and its label on the button.
 */
final class HologramPainter {

    /** How far one layer lifts, in panel pixels. */
    private static final float LAYER = 0.12f;

    private static final int LIGHT = LightCoordsUtil.FULL_BRIGHT;

    private final SubmitNodeCollector collector;
    private final PoseStack pose;
    private final Font font;
    private final List<float[]> quads = new ArrayList<>();

    HologramPainter(SubmitNodeCollector collector, PoseStack pose, Font font) {
        this.collector = collector;
        this.pose = pose;
        this.font = font;
    }

    /** Fills a rectangle on {@code layer}, seen from the front. */
    void rect(float left, float top, float right, float bottom, int layer, int colour) {
        quads.add(new float[] {left, top, right, bottom, layer * LAYER, colour, 0});
    }

    /** Fills a rectangle seen from behind, so the panel is solid from both sides. */
    void back(float left, float top, float right, float bottom, int colour) {
        quads.add(new float[] {left, top, right, bottom, -LAYER, colour, 1});
    }

    /** Draws a one-pixel outline just inside a rectangle. */
    void outline(float left, float top, float right, float bottom, int layer, int colour) {
        rect(left, top, right, top + 1, layer, colour);
        rect(left, bottom - 1, right, bottom, layer, colour);
        rect(left, top + 1, left + 1, bottom - 1, layer, colour);
        rect(right - 1, top + 1, right, bottom - 1, layer, colour);
    }

    /** Lights the top and left inner edges and shades the bottom and right ones. */
    void bevel(float left, float top, float right, float bottom, int layer, int light, int shade) {
        rect(left, top, right, top + 1, layer, light);
        rect(left, top + 1, left + 1, bottom, layer, light);
        rect(left + 1, bottom - 1, right, bottom, layer, shade);
        rect(right - 1, top + 1, right, bottom - 1, layer, shade);
    }

    /** @return how wide {@code text} is at scale 1, in panel pixels */
    float width(String text) {
        return font.width(text);
    }

    /**
     * Draws text with its top left corner at {@code (x, y)}.
     *
     * @param outline an outline colour, or 0 for none
     */
    void text(String text, float x, float y, int layer, float scale, int colour, int outline) {
        FormattedCharSequence line = Component.literal(text).getVisualOrderText();
        pose.pushPose();
        pose.translate(x, y, layer * LAYER);
        pose.scale(scale, scale, 1);
        collector.order(1).submitText(pose, 0, 0, line, false, Font.DisplayMode.POLYGON_OFFSET,
                LIGHT, colour, 0, outline);
        pose.popPose();
    }

    /** Draws text centred on {@code centreX}. */
    void centred(String text, float centreX, float y, int layer, float scale, int colour,
                 int outline) {
        text(text, centreX - width(text) * scale / 2, y, layer, scale, colour, outline);
    }

    /** Sends the gathered rectangles to the game, in the pose they were drawn in. */
    void flush() {
        if (quads.isEmpty()) {
            return;
        }
        List<float[]> batch = List.copyOf(quads);
        quads.clear();
        collector.submitCustomGeometry(pose, RenderTypes.textBackground(), (last, buffer) -> {
            for (float[] q : batch) {
                quad(buffer, last, q[0], q[1], q[2], q[3], q[4], (int) q[5], q[6] != 0);
            }
        });
    }

    private static void quad(VertexConsumer buffer, PoseStack.Pose last, float left, float top,
                             float right, float bottom, float z, int colour, boolean back) {
        // Counter-clockwise as seen from the side that should show it.
        if (back) {
            buffer.addVertex(last, left, top, z).setColor(colour).setLight(LIGHT);
            buffer.addVertex(last, right, top, z).setColor(colour).setLight(LIGHT);
            buffer.addVertex(last, right, bottom, z).setColor(colour).setLight(LIGHT);
            buffer.addVertex(last, left, bottom, z).setColor(colour).setLight(LIGHT);
        } else {
            buffer.addVertex(last, left, top, z).setColor(colour).setLight(LIGHT);
            buffer.addVertex(last, left, bottom, z).setColor(colour).setLight(LIGHT);
            buffer.addVertex(last, right, bottom, z).setColor(colour).setLight(LIGHT);
            buffer.addVertex(last, right, top, z).setColor(colour).setLight(LIGHT);
        }
    }
}
