package io.github.mcagentsmods.core.common.hologram;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Hologram geometry: placing a hologram in front of the player, arranging its panels, finding
 * the point the player aims at, and moving a carried hologram with the player.
 *
 * <p>Panels are measured in panel pixels and drawn at {@link #BLOCKS_PER_PIXEL}, so a font
 * glyph eight pixels tall is 0.08 blocks tall.
 */
public final class HologramSpace {

    /** The size of one panel pixel, in blocks. */
    public static final double BLOCKS_PER_PIXEL = 0.01;

    /** The gap between two panels of one hologram, in blocks. */
    public static final double PANEL_GAP = 0.08;

    private HologramSpace() {
    }

    /**
     * Where a hologram opens: {@code distance} blocks ahead of the eye along the level
     * direction the player faces, a little below eye height so its middle meets the
     * crosshair's natural line.
     *
     * @param eye      the player's eye
     * @param yaw      the player's yaw
     * @param distance how far ahead
     * @return the pose, facing the player
     */
    public static HologramPose inFront(Vector3 eye, double yaw, double distance) {
        Objects.requireNonNull(eye, "eye");
        Vector3 ahead = new Vector3(0, 0, distance).rotateY(-yaw);
        return new HologramPose(eye.add(ahead).add(new Vector3(0, -0.15, 0)), yaw);
    }

    /**
     * The turns, in degrees and in the order tried, that {@link #clearOf} turns a new
     * hologram's place around the player by: straight ahead, then right and left in widening
     * steps. A positive turn is to the player's right.
     */
    static final double[] TURNS = {0, 40, -40, 80, -80, 120, -120};

    /**
     * Where a hologram opens when others already stand: in front of the player as
     * {@link #inFront} places it or, when that spot overlaps a hologram in {@code others}, the
     * first spot turned around the player by one of {@link #TURNS} that is clear. When none is
     * clear, straight ahead, overlapping.
     *
     * @param eye       the player's eye
     * @param yaw       the player's yaw
     * @param distance  how far ahead
     * @param halfWidth half the new hologram's width, in blocks
     * @param others    the footprints of the holograms already standing
     * @return the pose, facing the player
     */
    public static HologramPose clearOf(Vector3 eye, double yaw, double distance, double halfWidth,
                                       Collection<HologramFootprint> others) {
        Objects.requireNonNull(others, "others");
        for (double turn : TURNS) {
            HologramPose pose = inFront(eye, yaw + turn, distance);
            HologramFootprint footprint = new HologramFootprint(pose.anchor(), halfWidth);
            if (others.stream().noneMatch(footprint::overlaps)) {
                return pose;
            }
        }
        return inFront(eye, yaw, distance);
    }

    /**
     * Arranges panels on an arc around a player standing {@code radius} blocks in front of the
     * middle, each turned to face that player. A lone panel stands at the anchor; with several,
     * the outer ones are turned in so their far edges stay on screen.
     *
     * @param widths each panel's width, left to right, in blocks
     * @param radius how far the player stands from the middle, in blocks
     * @return one placement per panel, left to right
     */
    public static List<PanelPlacement> arrange(double[] widths, double radius) {
        Objects.requireNonNull(widths, "widths");
        if (widths.length == 0 || !(radius > 0)) {
            throw new IllegalArgumentException("Need at least one panel and a positive radius");
        }
        double[] angles = new double[widths.length];
        for (int i = 1; i < widths.length; i++) {
            double span = widths[i - 1] / 2 + PANEL_GAP + widths[i] / 2;
            angles[i] = angles[i - 1] + Math.toDegrees(span / radius);
        }
        double middle = (angles[0] + angles[widths.length - 1]) / 2;
        List<PanelPlacement> placements = new ArrayList<>(widths.length);
        for (double angle : angles) {
            double a = Math.toRadians(angle - middle);
            // Subtracting from zero keeps the middle panel at 0.0 rather than -0.0.
            placements.add(new PanelPlacement(radius * Math.sin(a), radius - radius * Math.cos(a),
                    0.0 - Math.toDegrees(a)));
        }
        return List.copyOf(placements);
    }

    /**
     * Where a ray from the eye meets a panel's front, if it does. A panel is seen from its
     * front only, so a ray from behind it, or one that misses it, finds nothing.
     *
     * @param pose      the hologram's pose
     * @param placement the panel's placement in the hologram
     * @param width     the panel's width, in panel pixels
     * @param height    the panel's height, in panel pixels
     * @param eye       the ray's start
     * @param look      the ray's direction; need not be unit length
     * @return the point, in panel pixels from the top left corner
     */
    public static Optional<PanelHit> hit(HologramPose pose, PanelPlacement placement,
                                         double width, double height, Vector3 eye, Vector3 look) {
        double length = look.length();
        if (length == 0) {
            return Optional.empty();
        }
        Vector3 centre = new Vector3(placement.x(), 0, placement.z());
        Vector3 from = pose.toLocal(eye.subtract(pose.anchor())).subtract(centre)
                .rotateY(-placement.turn());
        Vector3 along = pose.toLocal(look.scale(1 / length)).rotateY(-placement.turn());
        if (from.z() <= 0 || along.z() >= 0) {
            return Optional.empty();
        }
        double distance = -from.z() / along.z();
        double u = (from.x() + along.x() * distance) / BLOCKS_PER_PIXEL + width / 2;
        double v = height / 2 - (from.y() + along.y() * distance) / BLOCKS_PER_PIXEL;
        if (u < 0 || u > width || v < 0 || v > height) {
            return Optional.empty();
        }
        return Optional.of(new PanelHit(u, v, distance));
    }

    /**
     * The point a carried hologram is held by, in its own frame, so that point can stay under
     * the crosshair while the player moves; see {@link #carry}.
     *
     * @param pose     the hologram's pose when it was taken
     * @param eye      the eye when it was taken
     * @param look     the unit look direction when it was taken
     * @param distance how far along {@code look} the hologram was hit
     * @return the held point, relative to the anchor, in the hologram's frame
     */
    public static Vector3 grip(HologramPose pose, Vector3 eye, Vector3 look, double distance) {
        return pose.toLocal(eye.add(look.scale(distance)).subtract(pose.anchor()));
    }

    /**
     * Where a carried hologram stands now: turned to face the player again, with its held
     * point {@code distance} blocks along the player's look.
     *
     * @param grip     the held point, from {@link #grip}
     * @param distance how far ahead the hologram is held
     * @param eye      the eye now
     * @param look     the unit look direction now
     * @param yaw      the player's yaw now
     * @return the new pose
     */
    public static HologramPose carry(Vector3 grip, double distance, Vector3 eye, Vector3 look,
                                     double yaw) {
        HologramPose facing = new HologramPose(Vector3.ZERO, yaw);
        Vector3 held = eye.add(look.scale(distance));
        return new HologramPose(held.subtract(facing.toWorld(grip)), yaw);
    }
}
