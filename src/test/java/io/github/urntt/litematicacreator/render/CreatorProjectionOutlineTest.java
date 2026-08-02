package io.github.urntt.litematicacreator.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatorProjectionOutlineTest
{
    @Test
    void outlineIsActiveOnlyInCreatorWorldView()
    {
        assertTrue(CreatorProjectionOutline.isActive(true, false));
        assertFalse(CreatorProjectionOutline.isActive(false, false));
        assertFalse(CreatorProjectionOutline.isActive(true, true));
    }

    @Test
    void outlineRequiresAnIndexedNonAirProjection()
    {
        assertTrue(CreatorProjectionOutline.isRenderableTarget(true, true));
        assertFalse(CreatorProjectionOutline.isRenderableTarget(false, true));
        assertFalse(CreatorProjectionOutline.isRenderableTarget(true, false));
    }
}
