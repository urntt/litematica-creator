package io.github.urntt.litematicacreator.camera;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatorProjectionCollisionsTest
{
    private static final VoxelShape UPSIDE_DOWN_STAIR = Shapes.or(
            Shapes.box(0.0D, 0.5D, 0.0D, 1.0D, 1.0D, 1.0D),
            Shapes.box(0.0D, 0.0D, 0.5D, 1.0D, 0.5D, 1.0D)
    );

    @Test
    void ignoresTheEmptyHalfInsideAProjectionShapeBoundingBox()
    {
        AABB emptyLowerHalf = new AABB(0.1D, 0.1D, 0.1D, 0.9D, 0.49D, 0.49D);

        assertFalse(CreatorProjectionCollisions.intersects(UPSIDE_DOWN_STAIR, emptyLowerHalf));
    }

    @Test
    void detectsBothOccupiedPartsOfAProjectionShape()
    {
        AABB upperHalf = new AABB(0.1D, 0.51D, 0.1D, 0.9D, 0.9D, 0.9D);
        AABB lowerStep = new AABB(0.1D, 0.1D, 0.51D, 0.9D, 0.49D, 0.9D);

        assertTrue(CreatorProjectionCollisions.intersects(UPSIDE_DOWN_STAIR, upperHalf));
        assertTrue(CreatorProjectionCollisions.intersects(UPSIDE_DOWN_STAIR, lowerStep));
    }
}
