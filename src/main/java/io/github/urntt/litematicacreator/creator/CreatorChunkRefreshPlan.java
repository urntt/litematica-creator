package io.github.urntt.litematicacreator.creator;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;

final class CreatorChunkRefreshPlan
{
    private CreatorChunkRefreshPlan()
    {
    }

    static Set<ChunkPos> forBlockChange(
            LitematicaSchematic schematic,
            String regionName,
            BlockPos containerPos,
            List<SchematicPlacement> placements)
    {
        List<BlockPos> worldPositions = new ArrayList<>();

        for (SchematicPlacement placement : placements)
        {
            SubRegionPlacement regionPlacement = placement.getRelativeSubRegionPlacement(regionName);

            if (!placement.matchesRequirement(RequiredEnabled.PLACEMENT_ENABLED) ||
                regionPlacement == null ||
                !regionPlacement.matchesRequirement(RequiredEnabled.PLACEMENT_ENABLED))
            {
                continue;
            }

            @Nullable BlockPos worldPos = CreatorSchematicCoordinates.toWorld(
                    schematic,
                    regionName,
                    placement,
                    containerPos
            );

            if (worldPos != null)
            {
                worldPositions.add(worldPos);
            }
        }

        return forWorldPositions(worldPositions);
    }

    static Set<ChunkPos> forWorldPositions(Iterable<BlockPos> worldPositions)
    {
        Set<ChunkPos> chunks = new LinkedHashSet<>();

        for (BlockPos worldPos : worldPositions)
        {
            chunks.add(new ChunkPos(worldPos.getX() >> 4, worldPos.getZ() >> 4));
        }

        return Set.copyOf(chunks);
    }

    static StructuralRefresh forStructuralChange(
            Set<ChunkPos> affectedChunks,
            Set<ChunkPos> chunksStillTouched)
    {
        Set<ChunkPos> rebuild = new LinkedHashSet<>(affectedChunks);
        rebuild.retainAll(chunksStillTouched);
        Set<ChunkPos> unload = new LinkedHashSet<>(affectedChunks);
        unload.removeAll(chunksStillTouched);

        return new StructuralRefresh(Set.copyOf(rebuild), Set.copyOf(unload));
    }

    record StructuralRefresh(Set<ChunkPos> rebuildChunks, Set<ChunkPos> unloadChunks)
    {
    }
}
