package io.github.urntt.litematicacreator.camera;

import java.util.List;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CreatorCameraCollisionResolverTest
{
    @Test
    void returnsUnchangedMovementWithoutShapes()
    {
        Vec3 movement = new Vec3(1.0D, -0.25D, 0.5D);

        assertSame(
                movement,
                CreatorCameraCollisionResolver.collideWithShapes(
                        movement,
                        new AABB(0.0D, 0.0D, 0.0D, 0.6D, 1.8D, 0.6D),
                        List.of()
                )
        );
    }

    @Test
    void clipsMovementAgainstAProjectionShape()
    {
        AABB cameraBounds = new AABB(0.0D, 0.0D, 0.0D, 0.6D, 1.8D, 0.6D);
        VoxelShape wall = Shapes.box(1.0D, -1.0D, -1.0D, 2.0D, 2.0D, 1.0D);

        Vec3 resolved = CreatorCameraCollisionResolver.collideWithShapes(
                new Vec3(2.0D, 0.0D, 0.0D),
                cameraBounds,
                List.of(wall)
        );

        assertEquals(0.4D, resolved.x, 1.0E-9D);
        assertEquals(0.0D, resolved.y, 1.0E-9D);
        assertEquals(0.0D, resolved.z, 1.0E-9D);
    }

    @Test
    void deduplicatesAndSortsCandidateStepHeights()
    {
        AABB cameraBounds = new AABB(0.0D, 0.0D, 0.0D, 0.6D, 1.8D, 0.6D);
        VoxelShape lowerSlab = Shapes.box(0.5D, 0.0D, 0.0D, 1.5D, 0.5D, 1.0D);
        VoxelShape duplicateSlab = Shapes.box(-1.0D, 0.0D, 0.0D, 0.0D, 0.5D, 1.0D);
        VoxelShape higherStep = Shapes.box(0.5D, 0.0D, 1.0D, 1.5D, 0.6D, 2.0D);

        float[] candidates = CreatorCameraCollisionResolver.collectCandidateStepUpHeights(
                cameraBounds,
                List.of(higherStep, lowerSlab, duplicateSlab),
                0.6F,
                0.0F
        );

        assertArrayEquals(new float[] { 0.5F, 0.6F }, candidates);
    }
}
