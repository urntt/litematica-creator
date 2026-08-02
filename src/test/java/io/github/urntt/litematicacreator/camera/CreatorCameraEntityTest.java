package io.github.urntt.litematicacreator.camera;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatorCameraEntityTest
{
    @Test
    void reservesAnAssignedClientOnlyEntityId()
    {
        assertNotEquals(0, CreatorCameraEntity.CLIENT_ENTITY_ID);
        assertTrue(CreatorCameraEntity.CLIENT_ENTITY_ID < 0);
    }
}
