package io.github.mcagentsmods.core.common.hologram;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The two-chunk rule, with the owner's example: placed in chunk 1, gone in chunk 4. */
class HologramRangeTest {

    @Test
    void placedInChunkOneItStaysInChunksTwoAndThreeAndGoesInFour() {
        assertTrue(HologramRange.inRange(1, 0, 1, 0));
        assertTrue(HologramRange.inRange(1, 0, 2, 0));
        assertTrue(HologramRange.inRange(1, 0, 3, 0));
        assertFalse(HologramRange.inRange(1, 0, 4, 0));
    }

    @Test
    void eitherAxisAndEitherDirectionCounts() {
        assertTrue(HologramRange.inRange(0, 0, -2, 2));
        assertFalse(HologramRange.inRange(0, 0, -3, 0));
        assertFalse(HologramRange.inRange(0, 0, 0, 3));
        assertFalse(HologramRange.inRange(0, 0, 0, -3));
        assertTrue(HologramRange.inRange(0, 0, 2, -2), "diagonal, two on each axis");
    }

    @Test
    void blockPositionsFallInTheirChunks() {
        assertEquals(0, HologramRange.chunk(0));
        assertEquals(0, HologramRange.chunk(15.99));
        assertEquals(1, HologramRange.chunk(16));
        assertEquals(-1, HologramRange.chunk(-0.01));
        assertEquals(-1, HologramRange.chunk(-16));
        assertEquals(-2, HologramRange.chunk(-16.5));
    }

    @Test
    void worldPositionsUseTheChunkTheyStandIn() {
        Vector3 hologram = new Vector3(20, 64, 5);
        assertTrue(HologramRange.inRange(hologram, new Vector3(63.9, 70, 5)));
        assertFalse(HologramRange.inRange(hologram, new Vector3(64, 70, 5)));
        assertFalse(HologramRange.inRange(hologram, new Vector3(20, 64, -32.1)));
    }

    @Test
    void theRuleCountsTwoChunks() {
        assertEquals(2, HologramRange.CHUNKS);
    }
}
