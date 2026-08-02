package io.github.urntt.litematicacreator.creator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatorPlacementEntityCollisionPolicyTest
{
    @Test
    void checksOnlyAnActiveCameraWhenIgnoreIsDisabled()
    {
        assertTrue(CreatorPlacementEntityCollisionPolicy.shouldCheck(true, false));
        assertFalse(CreatorPlacementEntityCollisionPolicy.shouldCheck(false, false));
        assertFalse(CreatorPlacementEntityCollisionPolicy.shouldCheck(true, true));
    }

    @Test
    void bothWorldEntitiesAndTheUnregisteredCameraCanBlockPlacement()
    {
        assertTrue(CreatorPlacementEntityCollisionPolicy.canPlace(true, false));
        assertFalse(CreatorPlacementEntityCollisionPolicy.canPlace(false, false));
        assertFalse(CreatorPlacementEntityCollisionPolicy.canPlace(true, true));
        assertFalse(CreatorPlacementEntityCollisionPolicy.canPlace(false, true));
    }
}
