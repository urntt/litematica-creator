package io.github.urntt.litematicacreator.creator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

import fi.dy.masa.litematica.util.PositionUtils;

public final class CreatorCoordinateTransforms
{
    private CreatorCoordinateTransforms()
    {
    }

    public static BlockPos toSchematicRelative(BlockPos worldPos, BlockPos placementOrigin, Mirror mirror, Rotation rotation)
    {
        return PositionUtils.getReverseTransformedBlockPos(worldPos.subtract(placementOrigin), mirror, rotation);
    }

    public static BlockPos toWorld(BlockPos relativePos, BlockPos placementOrigin, Mirror mirror, Rotation rotation)
    {
        return PositionUtils.getTransformedBlockPos(relativePos, mirror, rotation).offset(placementOrigin);
    }
}
