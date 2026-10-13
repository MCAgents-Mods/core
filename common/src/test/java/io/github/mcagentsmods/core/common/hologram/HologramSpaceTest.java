package io.github.mcagentsmods.core.common.hologram;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Placement, aiming and carrying, checked against Minecraft's axes: yaw 0 looks south (+z). */
class HologramSpaceTest {

    private static final double EPSILON = 1e-9;

    private static Vector3 look(double yaw, double pitch) {
        double y = Math.toRadians(yaw);
        double p = Math.toRadians(pitch);
        return new Vector3(-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p));
    }

    private static void assertNear(Vector3 expected, Vector3 actual) {
        assertEquals(expected.x(), actual.x(), 1e-6, "x");
        assertEquals(expected.y(), actual.y(), 1e-6, "y");
        assertEquals(expected.z(), actual.z(), 1e-6, "z");
    }

    @Test
    void aHologramOpensAheadOfThePlayerWhicheverWayTheyFace() {
        Vector3 eye = new Vector3(10, 70, 10);

        assertNear(new Vector3(10, 69.85, 13), HologramSpace.inFront(eye, 0, 3).anchor());
        assertNear(new Vector3(7, 69.85, 10), HologramSpace.inFront(eye, 90, 3).anchor());
        assertNear(new Vector3(10, 69.85, 7), HologramSpace.inFront(eye, 180, 3).anchor());
    }

    @Test
    void theFrameFacesThePlayerWithTheirRightOnPlusX() {
        HologramPose south = new HologramPose(Vector3.ZERO, 0);

        // Looking south, the player's right is west and they stand to the north.
        assertNear(new Vector3(-1, 0, 0), south.toWorld(new Vector3(1, 0, 0)));
        assertNear(new Vector3(0, 0, -1), south.toWorld(new Vector3(0, 0, 1)));
        assertNear(new Vector3(1, 2, 3), south.toLocal(south.toWorld(new Vector3(1, 2, 3))));
    }

    @Test
    void lookingAtTheMiddleHitsTheMiddle() {
        Vector3 eye = new Vector3(0, 70, 0);
        HologramPose pose = new HologramPose(new Vector3(0, 70, 3), 0);

        PanelHit hit = HologramSpace.hit(pose, PanelPlacement.CENTRE, 200, 100, eye, look(0, 0))
                .orElseThrow();

        assertEquals(100, hit.u(), EPSILON);
        assertEquals(50, hit.v(), EPSILON);
        assertEquals(3, hit.distance(), EPSILON);
    }

    @Test
    void lookingRightAndUpMovesTheHitRightAndUp() {
        Vector3 eye = new Vector3(0, 70, 0);
        HologramPose pose = new HologramPose(new Vector3(0, 70, 3), 0);
        // Half a block to the player's right is west, and the panel is 0.01 blocks a pixel.
        Vector3 towards = new Vector3(-0.5, 0.25, 3);

        PanelHit hit = HologramSpace.hit(pose, PanelPlacement.CENTRE, 200, 100, eye, towards)
                .orElseThrow();

        assertEquals(150, hit.u(), 1e-6);
        assertEquals(25, hit.v(), 1e-6);
    }

    @Test
    void aRayThatMissesOrComesFromBehindFindsNothing() {
        HologramPose pose = new HologramPose(new Vector3(0, 70, 3), 0);
        Vector3 eye = new Vector3(0, 70, 0);

        assertEquals(Optional.empty(), HologramSpace.hit(pose, PanelPlacement.CENTRE, 200, 100,
                eye, look(0, -30)), "over the top");
        assertEquals(Optional.empty(), HologramSpace.hit(pose, PanelPlacement.CENTRE, 200, 100,
                eye, look(180, 0)), "facing away");
        assertEquals(Optional.empty(), HologramSpace.hit(pose, PanelPlacement.CENTRE, 200, 100,
                new Vector3(0, 70, 6), look(180, 0)), "from behind");
    }

    @Test
    void aLonePanelStandsAtTheAnchor() {
        assertEquals(List.of(new PanelPlacement(0, 0, 0)),
                HologramSpace.arrange(new double[] {2.5}, 3));
    }

    @Test
    void outerPanelsStandOnTheArcTurnedTowardsThePlayer() {
        List<PanelPlacement> panels = HologramSpace.arrange(new double[] {1.5, 1.5, 1.5}, 3);

        assertEquals(0, panels.get(1).x(), EPSILON);
        assertEquals(0, panels.get(1).turn(), EPSILON);
        assertEquals(-panels.get(2).x(), panels.get(0).x(), EPSILON);
        assertEquals(-panels.get(2).turn(), panels.get(0).turn(), EPSILON);
        assertTrue(panels.get(2).x() > 0 && panels.get(2).turn() < 0, "right panel turned left");
        for (PanelPlacement panel : panels) {
            Vector3 centre = new Vector3(panel.x(), 0, panel.z());
            assertEquals(3, new Vector3(0, 0, 3).subtract(centre).length(), 1e-9,
                    "every centre is 3 blocks from the player");
        }
    }

    @Test
    void aTurnedPanelIsHitSquarelyFromThePlayer() {
        List<PanelPlacement> panels = HologramSpace.arrange(new double[] {1.5, 1.5, 1.5}, 3);
        HologramPose pose = new HologramPose(new Vector3(0, 70, 3), 0);
        Vector3 eye = new Vector3(0, 70, 0);
        PanelPlacement right = panels.get(2);
        Vector3 centre = pose.anchor().add(pose.toWorld(new Vector3(right.x(), 0, right.z())));

        PanelHit hit = HologramSpace.hit(pose, right, 150, 230, eye, centre.subtract(eye))
                .orElseThrow();

        assertEquals(75, hit.u(), 1e-6);
        assertEquals(115, hit.v(), 1e-6);
        assertEquals(3, hit.distance(), 1e-6);
    }

    @Test
    void aCarriedHologramKeepsTheHeldPointUnderTheCrosshair() {
        HologramPose pose = new HologramPose(new Vector3(0, 70, 3), 0);
        Vector3 eye = new Vector3(0, 70, 0);
        Vector3 aim = new Vector3(-0.4, 0.2, 3);
        double distance = HologramSpace.hit(pose, PanelPlacement.CENTRE, 250, 130, eye, aim)
                .orElseThrow().distance();
        Vector3 unit = aim.scale(1 / aim.length());
        Vector3 grip = HologramSpace.grip(pose, eye, unit, distance);

        // The player walks east and turns to face west.
        Vector3 newEye = new Vector3(20, 71, 0);
        Vector3 newLook = look(90, 0);
        HologramPose moved = HologramSpace.carry(grip, distance, newEye, newLook, 90);

        assertEquals(90, moved.yaw(), EPSILON);
        Vector3 held = moved.anchor().add(moved.toWorld(grip));
        assertNear(newEye.add(newLook.scale(distance)), held);
        PanelHit again = HologramSpace.hit(moved, PanelPlacement.CENTRE, 250, 130, newEye, newLook)
                .orElseThrow();
        PanelHit before = HologramSpace.hit(pose, PanelPlacement.CENTRE, 250, 130, eye, aim)
                .orElseThrow();
        assertEquals(before.u(), again.u(), 1e-6);
        assertEquals(before.v(), again.v(), 1e-6);
    }
}
