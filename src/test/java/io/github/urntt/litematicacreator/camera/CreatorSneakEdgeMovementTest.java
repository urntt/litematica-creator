package io.github.urntt.litematicacreator.camera;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreatorSneakEdgeMovementTest
{
    @Test
    void keepsMovementWhenProjectionSupportContinuesUnderTheTarget()
    {
        Vec3 movement = new Vec3(0.2D, 0.0D, -0.15D);

        Vec3 adjusted = CreatorSneakEdgeMovement.backOff(movement, 0.6F, (x, z, height) -> false);

        assertEquals(movement, adjusted);
    }

    @Test
    void removesHorizontalMovementWhenNoSupportExists()
    {
        Vec3 adjusted = CreatorSneakEdgeMovement.backOff(
                new Vec3(0.2D, -0.1D, -0.15D),
                0.6F,
                (x, z, height) -> true
        );

        assertEquals(0.0D, adjusted.x, 1.0E-9D);
        assertEquals(-0.1D, adjusted.y, 1.0E-9D);
        assertEquals(0.0D, adjusted.z, 1.0E-9D);
    }

    @Test
    void trimsMovementBackToTheLastSupportedOffset()
    {
        Vec3 adjusted = CreatorSneakEdgeMovement.backOff(
                new Vec3(0.2D, 0.0D, 0.0D),
                0.6F,
                (x, z, height) -> x > 0.11D
        );

        assertEquals(0.1D, adjusted.x, 1.0E-9D);
    }
}
