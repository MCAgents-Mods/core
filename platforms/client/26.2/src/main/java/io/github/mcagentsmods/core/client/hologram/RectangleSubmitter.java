package io.github.mcagentsmods.core.client.hologram;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;

import java.util.List;

/**
 * Sends a panel's rectangles to the game, for Minecraft 26.2. Every Minecraft version core
 * builds for has its own copy of this class, because 26.3 removed the render type this one
 * draws with.
 *
 * <p>26.2 draws every rectangle of a panel as one piece of custom geometry in the text
 * background's render type, with each rectangle's corners wound to face the side that should
 * see it.
 */
final class RectangleSubmitter {

    private RectangleSubmitter() {
    }

    static void submit(SubmitNodeCollector collector, PoseStack pose,
                       List<HologramPainter.Rect> rects) {
        collector.submitCustomGeometry(pose, RenderTypes.textBackground(), (last, buffer) -> {
            for (HologramPainter.Rect rect : rects) {
                quad(buffer, last, rect);
            }
        });
    }

    private static void quad(VertexConsumer buffer, PoseStack.Pose last, HologramPainter.Rect rect) {
        float left = rect.left();
        float top = rect.top();
        float right = rect.right();
        float bottom = rect.bottom();
        float z = rect.z();
        int colour = rect.colour();
        int light = HologramPainter.LIGHT;
        // Counter-clockwise as seen from the side that should show it.
        if (rect.back()) {
            buffer.addVertex(last, left, top, z).setColor(colour).setLight(light);
            buffer.addVertex(last, right, top, z).setColor(colour).setLight(light);
            buffer.addVertex(last, right, bottom, z).setColor(colour).setLight(light);
            buffer.addVertex(last, left, bottom, z).setColor(colour).setLight(light);
        } else {
            buffer.addVertex(last, left, top, z).setColor(colour).setLight(light);
            buffer.addVertex(last, left, bottom, z).setColor(colour).setLight(light);
            buffer.addVertex(last, right, bottom, z).setColor(colour).setLight(light);
            buffer.addVertex(last, right, top, z).setColor(colour).setLight(light);
        }
    }
}
