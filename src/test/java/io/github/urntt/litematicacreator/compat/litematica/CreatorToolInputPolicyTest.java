package io.github.urntt.litematicacreator.compat.litematica;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatorToolInputPolicyTest
{
    @Test
    void heldToolClaimsItsHotkeys()
    {
        assertTrue(CreatorToolInputPolicy.claimsPress(true, true, true, false));
        assertFalse(CreatorToolInputPolicy.claimsPress(true, true, false, false));
    }

    @Test
    void toolHotkeysWithoutTheToolStayWithCreator()
    {
        assertFalse(CreatorToolInputPolicy.claimsPress(true, false, true, false));
    }

    @Test
    void blockSelectionIsClaimedEvenWithoutTheTool()
    {
        assertTrue(CreatorToolInputPolicy.claimsPress(true, false, true, true));
    }

    @Test
    void disabledToolNeverClaimsInput()
    {
        assertFalse(CreatorToolInputPolicy.claimsPress(false, true, true, true));
        assertFalse(CreatorToolInputPolicy.claimsScroll(false, true, true));
    }

    @Test
    void scrollNeedsTheToolAndAModifier()
    {
        assertTrue(CreatorToolInputPolicy.claimsScroll(true, true, true));
        assertFalse(CreatorToolInputPolicy.claimsScroll(true, true, false));
        assertFalse(CreatorToolInputPolicy.claimsScroll(true, false, true));
    }
}
