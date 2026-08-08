package io.github.urntt.litematicacreator.export;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.util.PositionUtils;

record CreatorWorldSamplingTransform(BlockPos origin, Mirror mirror, Rotation rotation)
{
    static CreatorWorldSamplingTransform capture(SchematicPlacement placement)
    {
        return new CreatorWorldSamplingTransform(
                placement.getOrigin(),
                placement.getMirror(),
                placement.getRotation()
        );
    }

    BlockPos toWorld(BlockPos schematicPos)
    {
        return PositionUtils.getTransformedBlockPos(schematicPos, this.mirror, this.rotation).offset(this.origin);
    }
}
