package io.github.urntt.litematicacreator.gui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CreatorManagerListPolicyTest
{
    @Test
    void singlePlacementSchematicsUseOneCombinedListRow()
    {
        assertFalse(CreatorManagerListPolicy.includePlacementRows(0));
        assertFalse(CreatorManagerListPolicy.includePlacementRows(1));
        assertTrue(CreatorManagerListPolicy.includePlacementRows(2));
    }

    @Test
    void combinedSinglePlacementRowRemainsSelectedOnPlacementTab()
    {
        assertTrue(CreatorManagerListPolicy.schematicRowIsSelected(true, true, 1));
        assertFalse(CreatorManagerListPolicy.schematicRowIsSelected(true, true, 2));
        assertTrue(CreatorManagerListPolicy.schematicRowIsSelected(true, false, 2));
        assertFalse(CreatorManagerListPolicy.schematicRowIsSelected(false, false, 1));
    }
}
