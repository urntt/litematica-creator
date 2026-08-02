package io.github.urntt.litematicacreator.creator;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
        Set<ChunkPos> chunks = new LinkedHashSet<>();

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
                chunks.add(new ChunkPos(worldPos.getX() >> 4, worldPos.getZ() >> 4));
            }
        }

        return Set.copyOf(chunks);
    }

    static Set<ChunkPos> unionTouchedChunks(
            Map<SchematicPlacement, Set<ChunkPos>> oldChunks,
            Map<SchematicPlacement, Set<ChunkPos>> newChunks)
    {
        Set<ChunkPos> chunks = new LinkedHashSet<>();
        oldChunks.values().forEach(chunks::addAll);
        newChunks.values().forEach(chunks::addAll);
        return Set.copyOf(chunks);
    }
}
