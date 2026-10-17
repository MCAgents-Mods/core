package io.github.mcagentsmods.core.common.hologram;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A form's rows, boxes, steppers, choices and bottom buttons, hit where they are drawn. */
class FormLayoutTest {

    @Test
    void rowsStackUnderTheTitleWithAGapBetween() {
        List<PanelArea> rows = FormLayout.stack(FormLayout.NORMAL_WIDTH,
                new int[] {FormLayout.LINE, FormLayout.LABELLED_BOX, FormLayout.BUTTON}, true);

        assertEquals(FormLayout.TOP, rows.get(0).top());
        assertEquals(rows.get(0).bottom() + FormLayout.GAP, rows.get(1).top());
        assertEquals(rows.get(1).bottom() + FormLayout.GAP, rows.get(2).top());
        assertEquals(FormLayout.LABELLED_BOX, rows.get(1).height());
        assertEquals(FormLayout.EDGE, rows.get(2).left());
        assertEquals(FormLayout.NORMAL_WIDTH - FormLayout.EDGE, rows.get(2).right());
        assertTrue(FormLayout.fits(rows.get(2), true));
    }

    @Test
    void theAreaTakesWhatTheOtherRowsLeave() {
        List<PanelArea> rows = FormLayout.stack(FormLayout.WIDE_WIDTH,
                new int[] {FormLayout.LINE, 0, FormLayout.BUTTON}, true);

        assertEquals(FormLayout.bottom(true), rows.get(2).bottom());
        assertTrue(rows.get(1).height() > FormLayout.MIN_AREA);
        assertEquals(FormLayout.WIDE_WIDTH - FormLayout.EDGE, rows.get(1).right());
    }

    @Test
    void anAreaKeepsItsLeastHeightAndPushesLaterRowsOutOfTheForm() {
        int[] heights = new int[12];
        java.util.Arrays.fill(heights, FormLayout.LABELLED_BOX);
        heights[0] = 0;
        List<PanelArea> rows = FormLayout.stack(FormLayout.NORMAL_WIDTH, heights, false);

        assertEquals(FormLayout.MIN_AREA, rows.get(0).height());
        assertTrue(FormLayout.fits(rows.get(1), false));
        assertFalse(FormLayout.fits(rows.get(11), false));
    }

    @Test
    void theBoxSitsUnderTheLabelAndTheArrowInsideItsRightEnd() {
        PanelArea row = new PanelArea(6, 40, 154, 40 + FormLayout.LABELLED_BOX);
        PanelArea box = FormLayout.box(row);
        PanelArea arrow = FormLayout.arrow(box);

        assertEquals(40 + FormLayout.LINE, box.top());
        assertEquals(row.bottom(), box.bottom());
        assertEquals(box.right() - 1, arrow.right());
        assertTrue(FormLayout.text(box, true).right() < arrow.left());
        assertTrue(FormLayout.text(box, false).right() > arrow.left());
    }

    @Test
    void aStepperPutsItsNumberBetweenMinusAndPlusOnTheRight() {
        PanelArea row = new PanelArea(6, 40, 154, 56);
        PanelArea minus = FormLayout.minus(row);
        PanelArea number = FormLayout.number(row);
        PanelArea plus = FormLayout.plus(row);

        assertEquals(row.right(), plus.right());
        assertEquals(plus.left(), number.right());
        assertEquals(number.left(), minus.right());
        assertEquals(FormLayout.STEP_WIDTH, minus.width());
    }

    @Test
    void choicesOpenUnderTheBoxWhenThereIsRoomAndAboveWhenThereIsNot() {
        PanelArea high = new PanelArea(6, 40, 154, 54);
        PanelArea under = FormLayout.choices(high, 20, FormLayout.bottom(true));
        assertEquals(high.bottom(), under.top());
        assertEquals(FormLayout.MAX_CHOICES, FormLayout.choiceCapacity(under));

        PanelArea low = new PanelArea(6, 180, 154, 194);
        PanelArea over = FormLayout.choices(low, 5, FormLayout.bottom(true));
        assertEquals(low.top(), over.bottom());
        assertEquals(5, FormLayout.choiceCapacity(over));
    }

    @Test
    void bottomButtonsShareTheFormsWidth() {
        PanelArea first = FormLayout.footerButton(FormLayout.WIDE_WIDTH, 0, 2);
        PanelArea second = FormLayout.footerButton(FormLayout.WIDE_WIDTH, 1, 2);

        assertEquals(FormLayout.EDGE, first.left());
        assertEquals(FormLayout.WIDE_WIDTH - FormLayout.EDGE, second.right());
        assertEquals(first.width(), second.width(), 1e-9);
        assertEquals(ColumnLayout.FOOTER.top(), first.top());
        assertThrows(IllegalArgumentException.class, () -> FormLayout.footerButton(160, 2, 2));
    }
}
