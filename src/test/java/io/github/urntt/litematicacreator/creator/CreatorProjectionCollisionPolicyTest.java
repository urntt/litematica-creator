package io.github.urntt.litematicacreator.creator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatorProjectionCollisionPolicyTest
{
    @Test
    void bothGlobalRenderingTogglesMustBeEnabled()
    {
        assertTrue(CreatorProjectionCollisionPolicy.globallyEnabled(true, true));
        assertFalse(CreatorProjectionCollisionPolicy.globallyEnabled(false, true));
        assertFalse(CreatorProjectionCollisionPolicy.globallyEnabled(true, false));
    }

    @Test
    void loadEntireSchematicsAllowsAnUnloadedClientChunk()
    {
        assertTrue(CreatorProjectionCollisionPolicy.chunkEligible(true, false));
        assertTrue(CreatorProjectionCollisionPolicy.chunkEligible(false, true));
        assertFalse(CreatorProjectionCollisionPolicy.chunkEligible(false, false));
    }

    @Test
    void airStillOverwritesAnEarlierProjectionState()
    {
        assertEquals("air", CreatorProjectionCollisionPolicy.applyContribution("stone", true, false, "air"));
    }

    @Test
    void structureVoidAndUncoveredRegionsDoNotOverwrite()
    {
        assertEquals("stone", CreatorProjectionCollisionPolicy.applyContribution("stone", true, true, "void"));
        assertEquals("stone", CreatorProjectionCollisionPolicy.applyContribution("stone", false, false, "air"));
    }
}
