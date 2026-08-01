package io.github.urntt.litematicacreator.creator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatorPlacementPolicyTest
{
    @Test
    void allowsAirAndReplaceableProjectionTargets()
    {
        assertTrue(CreatorPlacementPolicy.canWrite(true, false));
        assertTrue(CreatorPlacementPolicy.canWrite(false, true));
    }

    @Test
    void rejectsOccupiedNonReplaceableProjectionTargets()
    {
        assertFalse(CreatorPlacementPolicy.canWrite(false, false));
    }
}
