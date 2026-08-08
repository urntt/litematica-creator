package io.github.urntt.litematicacreator.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CreatorManagerLayoutTest
{
    @Test
    void keepsAUsableContentPaneAtNarrowWidths()
    {
        assertEquals(160, CreatorManagerLayout.leftPaneWidth(480));
        assertEquals(240, CreatorManagerLayout.leftPaneWidth(640));
        assertEquals(320, CreatorManagerLayout.leftPaneWidth(960));
        assertEquals(320, CreatorManagerLayout.leftPaneWidth(1920));
    }

    @Test
    void wrapsLongTranslatedButtonsInsteadOfOverlappingThem()
    {
        assertEquals(1, CreatorManagerLayout.buttonColumns(360, 3, 220, 4));
        assertEquals(3, CreatorManagerLayout.buttonColumns(660, 3, 200, 4));

        int columns = CreatorManagerLayout.buttonColumns(420, 3, 180, 4);
        int cellWidth = CreatorManagerLayout.buttonCellWidth(420, columns, 4);
        assertEquals(2, columns);
        assertTrue(columns * cellWidth + (columns - 1) * 4 <= 420);
    }

    @Test
    void reservesControlSpaceWhenLabelsAreLong()
    {
        assertEquals(168, CreatorManagerLayout.labelColumnWidth(500, 260));
        assertEquals(104, CreatorManagerLayout.labelColumnWidth(200, 260));
        assertEquals(64, CreatorManagerLayout.labelColumnWidth(200, 50));
    }
}
