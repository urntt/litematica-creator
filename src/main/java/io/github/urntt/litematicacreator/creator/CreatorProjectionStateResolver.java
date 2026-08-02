package io.github.urntt.litematicacreator.creator;

import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.SchematicUtils;
import fi.dy.masa.litematica.util.WorldUtils;

public final class CreatorProjectionStateResolver
{
    private CreatorProjectionStateResolver()
    {
    }

    public static Optional<BlockState> resolve(ClientLevel level, BlockPos worldPos)
    {
        if (!CreatorProjectionCollisionPolicy.globallyEnabled(
                Configs.Visuals.ENABLE_RENDERING.getBooleanValue(),
                Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue()
        ))
        {
            return Optional.empty();
        }

        if (!CreatorProjectionCollisionPolicy.chunkEligible(
                Configs.Generic.LOAD_ENTIRE_SCHEMATICS.getBooleanValue(),
                WorldUtils.isClientChunkLoaded(level, worldPos.getX() >> 4, worldPos.getZ() >> 4)
        ))
        {
            return Optional.empty();
        }

        @Nullable BlockState resolved = null;

        for (SchematicPlacement placement : DataManager.getSchematicPlacementManager().getAllSchematicsPlacements())
        {
            for (Map.Entry<String, Box> entry : placement.getSubRegionBoxes(RequiredEnabled.RENDERING_ENABLED).entrySet())
            {
                boolean covered = contains(entry.getValue(), worldPos);

                if (!covered)
                {
                    continue;
                }

                @Nullable BlockState state = getRegionState(placement, entry.getKey(), worldPos);

                if (state != null)
                {
                    resolved = CreatorProjectionCollisionPolicy.applyContribution(
                            resolved,
                            true,
                            state.is(Blocks.STRUCTURE_VOID),
                            state
                    );
                }
            }
        }

        return Optional.ofNullable(resolved);
    }

    @Nullable
    private static BlockState getRegionState(SchematicPlacement placement, String regionName, BlockPos worldPos)
    {
        LitematicaSchematic schematic = placement.getSchematic();
        LitematicaBlockStateContainer container = schematic.getSubRegionContainer(regionName);
        SubRegionPlacement regionPlacement = placement.getRelativeSubRegionPlacement(regionName);

        if (container == null || regionPlacement == null)
        {
            return null;
        }

        BlockPos containerPos = SchematicUtils.getSchematicContainerPositionFromWorldPosition(
                worldPos,
                schematic,
                regionName,
                placement,
                regionPlacement,
                container
        );

        if (containerPos == null)
        {
            return null;
        }

        BlockState state = container.get(containerPos.getX(), containerPos.getY(), containerPos.getZ());
        return CreatorSchematicEditor.toWorldBlockState(state, placement, regionPlacement);
    }

    private static boolean contains(Box box, BlockPos pos)
    {
        return pos.getX() >= Math.min(box.getPos1().getX(), box.getPos2().getX()) &&
               pos.getX() <= Math.max(box.getPos1().getX(), box.getPos2().getX()) &&
               pos.getY() >= Math.min(box.getPos1().getY(), box.getPos2().getY()) &&
               pos.getY() <= Math.max(box.getPos1().getY(), box.getPos2().getY()) &&
               pos.getZ() >= Math.min(box.getPos1().getZ(), box.getPos2().getZ()) &&
               pos.getZ() <= Math.max(box.getPos1().getZ(), box.getPos2().getZ());
    }
}
