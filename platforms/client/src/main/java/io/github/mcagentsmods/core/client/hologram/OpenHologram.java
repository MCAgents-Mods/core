package io.github.mcagentsmods.core.client.hologram;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.mcagentsmods.core.api.hologram.Hologram;
import io.github.mcagentsmods.core.common.hologram.HologramPose;
import io.github.mcagentsmods.core.common.hologram.HologramSpace;
import io.github.mcagentsmods.core.common.hologram.PanelHit;
import io.github.mcagentsmods.core.common.hologram.PanelPlacement;
import io.github.mcagentsmods.core.common.hologram.Vector3;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * A hologram standing in the world: its panels, where it stands, what the player aims at, and
 * whether it is being carried. Client thread only.
 */
final class OpenHologram {

    /** The nearest a hologram opens, in blocks. */
    private static final double MIN_RADIUS = 2.4;

    /** How far past its width a wider hologram opens, so its edges stay on screen. */
    private static final double RADIUS_PER_WIDTH = 0.75;

    /** The furthest the player can aim at a hologram from, in blocks. */
    static final double REACH = 12;

    final Hologram spec;
    final ClientLevel level;
    private final List<PanelView> views;
    private final List<PanelPlacement> placements;
    private HologramPose pose;

    private int hoverPanel = -1;
    private double hoverU = Double.NaN;
    private double hoverV = Double.NaN;
    private double hoverDistance;

    private Vector3 grip;
    private double gripDistance;

    OpenHologram(Hologram spec, ClientLevel level, List<PanelView> views, Vector3 eye, double yaw) {
        this.spec = spec;
        this.level = level;
        this.views = List.copyOf(views);
        double[] widths = new double[views.size()];
        double total = 0;
        for (int i = 0; i < widths.length; i++) {
            widths[i] = views.get(i).width() * HologramSpace.BLOCKS_PER_PIXEL;
            total += widths[i] + (i > 0 ? HologramSpace.PANEL_GAP : 0);
        }
        double radius = Math.max(MIN_RADIUS, total * RADIUS_PER_WIDTH);
        this.placements = HologramSpace.arrange(widths, radius);
        this.pose = HologramSpace.inFront(eye, yaw, radius);
    }

    HologramPose pose() {
        return pose;
    }

    boolean hovered() {
        return hoverPanel >= 0;
    }

    boolean carried() {
        return grip != null;
    }

    /**
     * Finds what the player aims at. Nothing is aimed at while {@code eye} is null, or when a
     * block or entity the crosshair is on stands nearer than the hologram.
     *
     * @param blocked how far away the crosshair's own target is, or infinity
     */
    void aim(Vector3 eye, Vector3 look, double blocked) {
        hoverPanel = -1;
        hoverU = Double.NaN;
        hoverV = Double.NaN;
        if (eye == null) {
            return;
        }
        double nearest = Math.min(REACH, blocked);
        for (int i = 0; i < views.size(); i++) {
            PanelView view = views.get(i);
            Optional<PanelHit> hit = HologramSpace.hit(pose, placements.get(i), view.width(),
                    view.height(), eye, look);
            if (hit.isPresent() && hit.get().distance() < nearest) {
                nearest = hit.get().distance();
                hoverPanel = i;
                hoverU = hit.get().u();
                hoverV = hit.get().v();
                hoverDistance = hit.get().distance();
            }
        }
    }

    /**
     * Presses whatever the player aims at.
     *
     * @return {@code true} when the click closed the hologram
     */
    boolean click() {
        if (hoverPanel < 0) {
            return false;
        }
        PanelView view = views.get(hoverPanel);
        if (hoverPanel == views.size() - 1 && view.onClose(hoverU, hoverV)) {
            return true;
        }
        view.click(hoverU, hoverV);
        return false;
    }

    void scroll(int rows) {
        if (hoverPanel >= 0) {
            views.get(hoverPanel).scroll(hoverU, hoverV, rows);
        }
    }

    /** Takes hold of the hologram at the point aimed at; it must be aimed at. */
    void take(Vector3 eye, Vector3 look) {
        if (hoverPanel >= 0) {
            gripDistance = hoverDistance;
            grip = HologramSpace.grip(pose, eye, look, gripDistance);
        }
    }

    /** Moves a carried hologram to follow the player's eye and look. */
    void follow(Vector3 eye, Vector3 look, double yaw) {
        if (grip != null) {
            pose = HologramSpace.carry(grip, gripDistance, eye, look, yaw);
        }
    }

    /** Tells every panel its hologram has closed. */
    void dispose() {
        views.forEach(PanelView::dispose);
    }

    /** Lets go: the hologram stays where it is now. */
    void release() {
        grip = null;
    }

    /** Sends every panel to the game for this frame. */
    void submit(SubmitNodeCollector collector, Vec3 camera, Font font) {
        Vector3 anchor = pose.anchor();
        for (int i = 0; i < views.size(); i++) {
            PanelView view = views.get(i);
            PanelPlacement placement = placements.get(i);
            PoseStack stack = new PoseStack();
            stack.translate(anchor.x() - camera.x, anchor.y() - camera.y, anchor.z() - camera.z);
            stack.mulPose(Axis.YP.rotationDegrees((float) pose.turn()));
            stack.translate(placement.x(), 0, placement.z());
            stack.mulPose(Axis.YP.rotationDegrees((float) placement.turn()));
            float scale = (float) HologramSpace.BLOCKS_PER_PIXEL;
            stack.scale(scale, -scale, scale);
            stack.translate(-view.width() / 2f, -view.height() / 2f, 0);

            HologramPainter painter = new HologramPainter(collector, stack, font);
            boolean aimed = i == hoverPanel;
            double u = aimed ? hoverU : Double.NaN;
            double v = aimed ? hoverV : Double.NaN;
            view.draw(painter, u, v);
            if (i == views.size() - 1) {
                view.drawClose(painter, u, v);
            }
            painter.flush();
        }
    }
}
