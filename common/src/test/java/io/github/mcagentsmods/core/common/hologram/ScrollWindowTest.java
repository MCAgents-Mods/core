package io.github.mcagentsmods.core.common.hologram;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A window over a longer column, which stops at both ends and can follow the end. */
class ScrollWindowTest {

    @Test
    void scrollingStopsAtBothEnds() {
        ScrollWindow window = new ScrollWindow(5, false);
        window.setTotal(12);

        assertFalse(window.scrollBy(-1), "already at the top");
        assertTrue(window.scrollBy(4));
        assertEquals(4, window.offset());
        assertTrue(window.scrollBy(100));
        assertEquals(7, window.offset());
        assertFalse(window.scrollBy(1), "already at the bottom");
    }

    @Test
    void contentThatFitsNeverScrolls() {
        ScrollWindow window = new ScrollWindow(5, false);
        window.setTotal(5);

        assertEquals(0, window.maxOffset());
        assertFalse(window.scrollBy(1));
        assertTrue(window.atEnd());
    }

    @Test
    void aWindowThatFollowsTheEndStaysOnItAsTheContentGrows() {
        ScrollWindow window = new ScrollWindow(5, true);
        window.setTotal(3);
        window.setTotal(8);

        assertEquals(3, window.offset(), "was at the end, so it stays there");
        window.setTotal(20);
        assertEquals(15, window.offset());
    }

    @Test
    void scrollingUpStopsTheFollowingAndScrollingDownRestartsIt() {
        ScrollWindow window = new ScrollWindow(5, true);
        window.setTotal(20);
        window.scrollBy(-4);
        window.setTotal(30);

        assertEquals(11, window.offset(), "the player is reading further up; leave them there");
        window.scrollBy(1000);
        window.setTotal(31);
        assertEquals(26, window.offset());
    }

    @Test
    void aWindowThatDoesNotFollowKeepsItsPlace() {
        ScrollWindow window = new ScrollWindow(5, false);
        window.setTotal(8);
        window.setTotal(20);

        assertEquals(0, window.offset());
    }

    @Test
    void shrinkingTheContentPullsTheWindowBackInside() {
        ScrollWindow window = new ScrollWindow(5, false);
        window.setTotal(20);
        window.scrollTo(15);
        window.setTotal(9);

        assertEquals(4, window.offset());
    }

    @Test
    void revealScrollsTheLeastThatShowsAnItem() {
        ScrollWindow window = new ScrollWindow(5, false);
        window.setTotal(30);

        window.reveal(3);
        assertEquals(0, window.offset(), "already showing");
        window.reveal(12);
        assertEquals(8, window.offset(), "brought in at the bottom");
        window.reveal(2);
        assertEquals(2, window.offset(), "brought in at the top");
        window.reveal(99);
        assertEquals(2, window.offset(), "outside the content: ignored");
    }

    @Test
    void theThumbFillsTheTrackUntilTheContentOverflows() {
        ScrollWindow window = new ScrollWindow(10, false);
        window.setTotal(4);
        assertEquals(100, window.thumbLength(100, 8));
        assertEquals(0, window.thumbStart(100, 8));

        window.setTotal(40);
        assertEquals(25, window.thumbLength(100, 8), "a quarter of the content shows");
        window.scrollBy(1000);
        assertEquals(75, window.thumbStart(100, 8), "at the bottom of the track");

        window.setTotal(10_000);
        assertEquals(8, window.thumbLength(100, 8), "never shorter than the minimum");
    }

    @Test
    void clickingTheTrackCentresTheThumbThere() {
        ScrollWindow window = new ScrollWindow(10, false);
        window.setTotal(40);

        window.scrollToTrack(100, 100, 8);
        assertEquals(30, window.offset());
        window.scrollToTrack(0, 100, 8);
        assertEquals(0, window.offset());
        window.scrollToTrack(50, 100, 8);
        assertEquals(15, window.offset());
    }
}
