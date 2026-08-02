package io.github.urntt.litematicacreator.creator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreatorAirPlacementTargetTest
{
    @Test
    void resolvesCardinalCameraTargetAtFixedDistance()
    {
        CreatorAirPlacementTarget.Target target = CreatorAirPlacementTarget.resolve(
                new Vec3(0.5D, 64.5D, 0.5D),
                new Vec3(0.0D, 0.0D, 1.0D),
                5
        );

        assertEquals(new BlockPos(0, 64, 5), target.blockPos());
        assertEquals(Direction.NORTH, target.side());
        assertEquals(new Vec3(0.5D, 64.5D, 5.5D), target.hitPosition());
    }

    @Test
    void normalizesViewVectorAndUsesOppositeDominantFace()
    {
        CreatorAirPlacementTarget.Target horizontal = CreatorAirPlacementTarget.resolve(
                new Vec3(0.5D, 64.5D, 0.5D),
                new Vec3(10.0D, 0.0D, 0.0D),
                5
        );
        CreatorAirPlacementTarget.Target vertical = CreatorAirPlacementTarget.resolve(
                new Vec3(0.5D, 64.5D, 0.5D),
                new Vec3(0.0D, 1.0D, 0.0D),
                5
        );

        assertEquals(new BlockPos(5, 64, 0), horizontal.blockPos());
        assertEquals(Direction.WEST, horizontal.side());
        assertEquals(new BlockPos(0, 69, 0), vertical.blockPos());
        assertEquals(Direction.DOWN, vertical.side());
    }

    @Test
    void placementDistanceCannotExceedEditRange()
    {
        assertEquals(5, CreatorAirPlacementTarget.effectiveDistance(5, 10));
        assertEquals(10, CreatorAirPlacementTarget.effectiveDistance(20, 10));
        assertEquals(1, CreatorAirPlacementTarget.effectiveDistance(0, 10));
    }
}
