package io.github.mcagentsmods.core.common.hologram;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Five buttons to a row, three rows showing, the rest scrolling. */
class HologramGridTest {

    @Test
    void fiveButtonsFillOneRowAndTheSixthStartsAnother() {
        assertEquals(0, new HologramGrid(0).rows());
        assertEquals(1, new HologramGrid(5).rows());
        assertEquals(2, new HologramGrid(6).rows());
        assertEquals(3, new HologramGrid(15).rows());
        assertEquals(4, new HologramGrid(16).rows());
    }

    @Test
    void onlyRowsBeyondTheThirdScroll() {
        assertEquals(0, new HologramGrid(15).maxScroll());
        assertEquals(1, new HologramGrid(16).maxScroll());
        assertEquals(3, new HologramGrid(30).maxScroll());
    }

    @Test
    void scrollingStopsAtBothEnds() {
        HologramGrid grid = new HologramGrid(30);

        assertFalse(grid.scrollBy(-1), "already at the top");
        assertTrue(grid.scrollBy(2));
        assertEquals(2, grid.scroll());
        assertTrue(grid.scrollBy(10));
        assertEquals(3, grid.scroll());
        assertFalse(grid.scrollBy(1), "already at the bottom");
        assertFalse(new HologramGrid(15).scrollBy(1), "three rows never scroll");
    }

    @Test
    void slotsReadLeftToRightThenRowByRow() {
        HologramGrid grid = new HologramGrid(12);

        assertEquals(0, grid.indexAt(0, 0));
        assertEquals(4, grid.indexAt(4, 0));
        assertEquals(5, grid.indexAt(0, 1));
        assertEquals(11, grid.indexAt(1, 2));
        assertEquals(-1, grid.indexAt(2, 2), "past the last button");
        assertEquals(-1, grid.indexAt(5, 0), "no sixth column");
    }

    @Test
    void scrollingShiftsWhichButtonsShow() {
        HologramGrid grid = new HologramGrid(23);
        grid.scrollBy(1);
        assertEquals(5, grid.indexAt(0, 0));
        assertEquals(17, grid.indexAt(2, 2));

        grid.scrollBy(1);
        assertEquals(10, grid.indexAt(0, 0));
        assertEquals(22, grid.indexAt(2, 2));
        assertEquals(-1, grid.indexAt(3, 2), "the last row is short");
    }

    @Test
    void aPointOnAButtonFindsItAndAGapFindsNothing() {
        HologramGrid grid = new HologramGrid(15);
        int left = HologramGrid.buttonLeft(2);
        int top = HologramGrid.buttonTop(1);

        assertEquals(7, grid.buttonAt(left + 1, top + 1));
        assertEquals(7, grid.buttonAt(left + HologramGrid.BUTTON_WIDTH - 0.5,
                top + HologramGrid.BUTTON_HEIGHT - 0.5));
        assertEquals(-1, grid.buttonAt(left - 1, top + 1), "the gap before the column");
        assertEquals(-1, grid.buttonAt(left + 1, top - 1), "the gap above the row");
        assertEquals(-1, grid.buttonAt(HologramGrid.SCROLLBAR_LEFT + 1, top + 1));
    }

    @Test
    void theButtonsAndTheScrollbarFitThePanel() {
        int lastRight = HologramGrid.buttonLeft(4) + HologramGrid.BUTTON_WIDTH;
        assertTrue(lastRight < HologramGrid.SCROLLBAR_LEFT);
        assertTrue(HologramGrid.SCROLLBAR_LEFT + HologramGrid.SCROLLBAR_WIDTH
                <= HologramGrid.WIDTH - HologramGrid.GRID_LEFT);
        assertTrue(HologramGrid.GRID_TOP + HologramGrid.GRID_HEIGHT < HologramGrid.HEIGHT);
    }

    @Test
    void theThumbFillsTheTrackUntilTheRowsOverflow() {
        assertEquals(HologramGrid.GRID_HEIGHT, new HologramGrid(4).thumbHeight());
        assertEquals(HologramGrid.GRID_HEIGHT / 2, new HologramGrid(30).thumbHeight());

        HologramGrid grid = new HologramGrid(30);
        assertEquals(HologramGrid.GRID_TOP, grid.thumbTop());
        grid.scrollTo(3);
        assertEquals(HologramGrid.GRID_TOP + HologramGrid.GRID_HEIGHT - grid.thumbHeight(),
                grid.thumbTop());
    }

    @Test
    void clickingTheTrackMovesTheThumbThere() {
        HologramGrid grid = new HologramGrid(30);

        grid.scrollToTrack(HologramGrid.GRID_TOP + HologramGrid.GRID_HEIGHT);
        assertEquals(3, grid.scroll());
        grid.scrollToTrack(HologramGrid.GRID_TOP);
        assertEquals(0, grid.scroll());
        assertTrue(HologramGrid.onScrollbar(HologramGrid.SCROLLBAR_LEFT + 1, HologramGrid.GRID_TOP));
        assertFalse(HologramGrid.onScrollbar(HologramGrid.SCROLLBAR_LEFT - 1, HologramGrid.GRID_TOP));
    }
}
