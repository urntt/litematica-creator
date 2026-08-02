package io.github.urntt.litematicacreator.creator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

final class CreatorAirPlacementTarget
{
    private static final double MIN_DIRECTION_LENGTH_SQUARED = 1.0E-8D;

    private CreatorAirPlacementTarget()
    {
    }

    static Target resolve(Vec3 eyePosition, Vec3 viewVector, int distance)
    {
        Vec3 direction = viewVector.lengthSqr() >= MIN_DIRECTION_LENGTH_SQUARED ?
                viewVector.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
        Vec3 targetPoint = eyePosition.add(direction.scale(distance));
        BlockPos blockPos = BlockPos.containing(targetPoint);
        Vec3 hitPosition = new Vec3(
                blockPos.getX() + 0.5D,
                blockPos.getY() + 0.5D,
                blockPos.getZ() + 0.5D
        );
        return new Target(blockPos, oppositeDominantDirection(direction), hitPosition);
    }

    static int effectiveDistance(int configuredDistance, int editRange)
    {
        return Math.max(1, Math.min(configuredDistance, editRange));
    }

    private static Direction oppositeDominantDirection(Vec3 direction)
    {
        double x = Math.abs(direction.x);
        double y = Math.abs(direction.y);
        double z = Math.abs(direction.z);

        if (y >= x && y >= z)
        {
            return direction.y >= 0.0D ? Direction.DOWN : Direction.UP;
        }

        if (x >= z)
        {
            return direction.x >= 0.0D ? Direction.WEST : Direction.EAST;
        }

        return direction.z >= 0.0D ? Direction.NORTH : Direction.SOUTH;
    }

    record Target(BlockPos blockPos, Direction side, Vec3 hitPosition)
    {
    }
}
