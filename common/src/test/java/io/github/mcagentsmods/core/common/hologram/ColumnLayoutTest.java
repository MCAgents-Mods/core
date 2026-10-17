package io.github.mcagentsmods.core.common.hologram;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The tall panels' box, rows and bottom buttons, hit where they are drawn. */
class ColumnLayoutTest {

    @Test
    void theBoxStopsAboveTheButtonsWhenThereAreSome() {
        PanelArea open = ColumnLayout.box(false);
        PanelArea above = ColumnLayout.box(true);

        assertEquals(ColumnLayout.HEIGHT - ColumnLayout.EDGE, open.bottom());
        assertTrue(above.bottom() < ColumnLayout.FOOTER.top());
        assertEquals(19, ColumnLayout.capacity(open, ColumnLayout.LINE_HEIGHT));
        assertEquals(14, ColumnLayout.capacity(above, ColumnLayout.ROW_HEIGHT));
    }

    @Test
    void theScrollbarAndTheTextStayApart() {
        PanelArea box = ColumnLayout.box(false);
        PanelArea track = ColumnLayout.scrollbar(box);
        PanelArea content = ColumnLayout.content(box);

        assertEquals(ColumnLayout.SCROLLBAR_WIDTH, track.width());
        assertTrue(content.right() < track.left());
        assertTrue(content.left() > box.left());
    }

    @Test
    void aPointFindsTheRowUnderItCountingTheScroll() {
        PanelArea box = ColumnLayout.box(true);
        ScrollWindow window = new ScrollWindow(ColumnLayout.capacity(box, ColumnLayout.ROW_HEIGHT), false);
        window.setTotal(30);
        double top = ColumnLayout.content(box).top();
        double u = box.left() + 10;

        assertEquals(0, ColumnLayout.itemAt(box, ColumnLayout.ROW_HEIGHT, window, u, top + 1));
        assertEquals(2, ColumnLayout.itemAt(box, ColumnLayout.ROW_HEIGHT, window, u, top + 2 * 12 + 5));
        window.scrollBy(5);
        assertEquals(7, ColumnLayout.itemAt(box, ColumnLayout.ROW_HEIGHT, window, u, top + 2 * 12 + 5));
        assertEquals(-1, ColumnLayout.itemAt(box, ColumnLayout.ROW_HEIGHT, window,
                ColumnLayout.scrollbar(box).left() + 2, top + 1), "on the scrollbar");
        assertEquals(-1, ColumnLayout.itemAt(box, ColumnLayout.ROW_HEIGHT, window, u, Double.NaN));
    }

    @Test
    void aPointBelowTheLastRowIsOnNone() {
        PanelArea box = ColumnLayout.box(true);
        ScrollWindow window = new ScrollWindow(ColumnLayout.capacity(box, ColumnLayout.ROW_HEIGHT), false);
        window.setTotal(2);
        double top = ColumnLayout.content(box).top();

        assertEquals(1, ColumnLayout.itemAt(box, ColumnLayout.ROW_HEIGHT, window, 20, top + 13));
        assertEquals(-1, ColumnLayout.itemAt(box, ColumnLayout.ROW_HEIGHT, window, 20, top + 30));
    }

    @Test
    void theBottomButtonsShareTheRowEvenly() {
        PanelArea first = ColumnLayout.footerButton(0, 3);
        PanelArea last = ColumnLayout.footerButton(2, 3);

        assertEquals(ColumnLayout.FOOTER.left(), first.left());
        assertEquals(ColumnLayout.FOOTER.right(), last.right(), 1e-9);
        assertEquals(first.width(), last.width(), 1e-9);
        assertFalse(first.contains(first.right() + 1, first.top() + 1), "the gap belongs to neither");
        assertEquals(ColumnLayout.FOOTER.right(), ColumnLayout.sendButton().right());
    }

    @Test
    void navButtonsStandAtTheRightOfTheTitleBandClearOfTheCloseButton() {
        PanelArea only = ColumnLayout.navButton(0, 1);
        PanelArea first = ColumnLayout.navButton(0, 2);
        PanelArea second = ColumnLayout.navButton(1, 2);

        assertEquals(ColumnLayout.NAV_RIGHT, only.right());
        assertEquals(ColumnLayout.NAV_RIGHT, second.right());
        assertTrue(first.right() + ColumnLayout.BUTTON_GAP <= second.left());
        assertTrue(ColumnLayout.HEADER.top() < only.top());
        assertTrue(only.bottom() < ColumnLayout.HEADER.bottom());
        // The close button takes the last 20 pixels of the band.
        assertTrue(only.right() <= ColumnLayout.WIDTH - 20);
    }

    @Test
    void theNavTitleStopsShortOfTheButtons() {
        assertEquals(ColumnLayout.NAV_RIGHT - ColumnLayout.NAV_TITLE_LEFT, ColumnLayout.navTitleRoom(0));
        assertTrue(ColumnLayout.NAV_TITLE_LEFT + ColumnLayout.navTitleRoom(1)
                < ColumnLayout.navButton(0, 1).left());
        assertTrue(ColumnLayout.navTitleRoom(2) < ColumnLayout.navTitleRoom(1));
        assertFalse(ColumnLayout.navTitleRoom(2) <= 0);
    }
}
