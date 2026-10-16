package io.github.mcagentsmods.core.client.hologram;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;

import java.util.List;

/**
 * Sends a panel's rectangles to the game, for Minecraft 26.3. It sits in the version folder
 * because the way a coloured rectangle is drawn changes between Minecraft releases: 26.3
 * removed the render type the release before it drew with.
 *
 * <p>26.3 draws a coloured rectangle as a text background, the way a text display's
 * background is drawn, one submission per rectangle. The game sorts those by their pose's
 * distance from the camera, so each is drawn from its own centre. A text background is seen
 * from the side text is, so one meant to be seen from behind is mirrored, which turns it
 * round without moving it.
 */
final class RectangleSubmitter {

    private RectangleSubmitter() {
    }

    static void submit(SubmitNodeCollector collector, PoseStack pose,
                       List<HologramPainter.Rect> rects) {
        for (HologramPainter.Rect rect : rects) {
            float halfWidth = (rect.right() - rect.left()) / 2;
            float halfHeight = (rect.bottom() - rect.top()) / 2;
            pose.pushPose();
            pose.translate(rect.left() + halfWidth, rect.top() + halfHeight, rect.z());
            if (rect.back()) {
                pose.scale(-1, 1, 1);
            }
            collector.submitTextBackground(pose, -halfWidth, -halfHeight, halfWidth, halfHeight,
                    rect.colour(), Font.DisplayMode.NORMAL, HologramPainter.LIGHT);
            pose.popPose();
        }
    }
}
