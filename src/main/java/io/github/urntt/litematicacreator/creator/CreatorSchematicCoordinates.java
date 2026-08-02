package io.github.urntt.litematicacreator.creator;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.util.PositionUtils;

final class CreatorSchematicCoordinates
{
    private CreatorSchematicCoordinates()
    {
    }

    @Nullable
    static BlockPos toWorld(
            LitematicaSchematic schematic,
            String regionName,
            SchematicPlacement placement,
            BlockPos containerPos)
    {
        SubRegionPlacement regionPlacement = placement.getRelativeSubRegionPlacement(regionName);
        BlockPos regionSize = schematic.getAreaSize(regionName);

        if (regionPlacement == null || regionSize == null)
        {
            return null;
        }

        return toWorld(
                containerPos,
                regionPlacement.getPos(),
                regionSize,
                placement.getOrigin(),
                placement.getMirror(),
                placement.getRotation(),
                regionPlacement.getMirror(),
                regionPlacement.getRotation()
        );
    }

    static BlockPos toWorld(
            BlockPos containerPos,
            BlockPos regionPos,
            BlockPos regionSize,
            BlockPos placementOrigin,
            Mirror placementMirror,
            Rotation placementRotation,
            Mirror regionMirror,
            Rotation regionRotation)
    {
        BlockPos regionMin = regionMin(regionPos, regionSize);
        BlockPos withinRegion = containerPos.offset(regionMin.subtract(regionPos));
        BlockPos transformed = PositionUtils.getTransformedBlockPos(withinRegion, placementMirror, placementRotation);
        transformed = PositionUtils.getTransformedBlockPos(transformed, regionMirror, regionRotation);
        BlockPos transformedRegionPos = PositionUtils.getTransformedBlockPos(regionPos, placementMirror, placementRotation);
        return transformed.offset(transformedRegionPos).offset(placementOrigin);
    }

    static BlockPos toContainer(
            BlockPos worldPos,
            BlockPos regionPos,
            BlockPos regionSize,
            BlockPos placementOrigin,
            Mirror placementMirror,
            Rotation placementRotation,
            Mirror regionMirror,
            Rotation regionRotation)
    {
        BlockPos transformedRegionPos = PositionUtils.getTransformedBlockPos(regionPos, placementMirror, placementRotation);
        BlockPos relative = worldPos.subtract(placementOrigin).subtract(transformedRegionPos);
        relative = PositionUtils.getReverseTransformedBlockPos(relative, regionMirror, regionRotation);
        relative = PositionUtils.getReverseTransformedBlockPos(relative, placementMirror, placementRotation);
        return relative.subtract(regionMin(regionPos, regionSize).subtract(regionPos));
    }

    private static BlockPos regionMin(BlockPos regionPos, BlockPos regionSize)
    {
        BlockPos regionEnd = regionPos.offset(relativeEnd(regionSize));
        return new BlockPos(
                Math.min(regionPos.getX(), regionEnd.getX()),
                Math.min(regionPos.getY(), regionEnd.getY()),
                Math.min(regionPos.getZ(), regionEnd.getZ())
        );
    }

    private static BlockPos relativeEnd(BlockPos size)
    {
        return new BlockPos(relativeEnd(size.getX()), relativeEnd(size.getY()), relativeEnd(size.getZ()));
    }

    private static int relativeEnd(int size)
    {
        return size > 0 ? size - 1 : size + 1;
    }
}
