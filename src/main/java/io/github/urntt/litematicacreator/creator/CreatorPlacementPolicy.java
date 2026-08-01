package io.github.urntt.litematicacreator.creator;

final class CreatorPlacementPolicy
{
    private CreatorPlacementPolicy()
    {
    }

    static boolean canWrite(boolean targetIsAir, boolean targetIsReplaceable)
    {
        return targetIsAir || targetIsReplaceable;
    }
}
