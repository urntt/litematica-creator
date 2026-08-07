package io.github.urntt.litematicacreator.camera;

import net.minecraft.world.phys.Vec3;

final class CreatorSneakEdgeMovement
{
    private static final double EDGE_STEP = 0.05D;

    private CreatorSneakEdgeMovement()
    {
    }

    static Vec3 backOff(Vec3 delta, float maxDownStep, FallCheck fallCheck)
    {
        double deltaX = delta.x;
        double deltaZ = delta.z;
        double stepX = Math.signum(deltaX) * EDGE_STEP;
        double stepZ = Math.signum(deltaZ) * EDGE_STEP;

        while (deltaX != 0.0D && fallCheck.canFallAtLeast(deltaX, 0.0D, maxDownStep))
        {
            if (Math.abs(deltaX) <= EDGE_STEP)
            {
                deltaX = 0.0D;
                break;
            }

            deltaX -= stepX;
        }

        while (deltaZ != 0.0D && fallCheck.canFallAtLeast(0.0D, deltaZ, maxDownStep))
        {
            if (Math.abs(deltaZ) <= EDGE_STEP)
            {
                deltaZ = 0.0D;
                break;
            }

            deltaZ -= stepZ;
        }

        while (deltaX != 0.0D && deltaZ != 0.0D && fallCheck.canFallAtLeast(deltaX, deltaZ, maxDownStep))
        {
            deltaX = Math.abs(deltaX) <= EDGE_STEP ? 0.0D : deltaX - stepX;
            deltaZ = Math.abs(deltaZ) <= EDGE_STEP ? 0.0D : deltaZ - stepZ;
        }

        return new Vec3(deltaX, delta.y, deltaZ);
    }

    @FunctionalInterface
    interface FallCheck
    {
        boolean canFallAtLeast(double deltaX, double deltaZ, double minHeight);
    }
}
