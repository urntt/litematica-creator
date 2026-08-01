package io.github.urntt.litematicacreator.creator;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.interfaces.ISchematicPlacementEventListener;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementEventFlag;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementEventHandler;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.selection.Box;

public class CreatorPlacementIndex implements ISchematicPlacementEventListener
{
    public static final CreatorPlacementIndex INSTANCE = new CreatorPlacementIndex();

    private final Map<Long, List<CreatorPlacementTarget>> targetsByChunk = new LinkedHashMap<>();
    private final Map<SchematicPlacement, Set<Long>> chunksByPlacement = new IdentityHashMap<>();
    private boolean registered;

    private CreatorPlacementIndex()
    {
    }

    public void register()
    {
        if (this.registered)
        {
            return;
        }

        this.registered = true;
        SchematicPlacementEventHandler.getInstance().registerSchematicPlacementEventListener(
                this,
                List.of(SchematicPlacementEventFlag.ALL_EVENTS)
        );
        this.rebuild();
    }

    public void rebuild()
    {
        this.targetsByChunk.clear();
        this.chunksByPlacement.clear();

        for (SchematicPlacement placement : DataManager.getSchematicPlacementManager().getAllSchematicsPlacements())
        {
            this.indexPlacement(placement);
        }
    }

    public List<CreatorPlacementTarget> findAt(BlockPos worldPos)
    {
        List<CreatorPlacementTarget> chunkTargets = this.targetsByChunk.get(ChunkPos.pack(worldPos.getX() >> 4, worldPos.getZ() >> 4));

        if (chunkTargets == null)
        {
            return List.of();
        }

        Set<SchematicPlacement> seen = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        List<CreatorPlacementTarget> matches = new ArrayList<>();

        for (CreatorPlacementTarget target : chunkTargets)
        {
            if (contains(target.box(), worldPos))
            {
                if (seen.add(target.placement()))
                {
                    matches.add(target);
                }
            }
        }

        return List.copyOf(matches);
    }

    @Override
    public void onPlacementAdded(SchematicPlacement placement)
    {
        this.indexPlacement(placement);
    }

    @Override
    public void onPlacementRemoved(SchematicPlacement placement)
    {
        this.removePlacement(placement);
        CreatorPlacementVisibility.onPlacementRemoved(placement);
        CreatorManager.getInstance().onPlacementRemoved(placement);
    }

    @Override
    public void onPlacementUpdated(SchematicPlacement placement)
    {
        this.removePlacement(placement);
        this.indexPlacement(placement);
    }

    private void indexPlacement(SchematicPlacement placement)
    {
        if (CreatorPlacementVisibility.isSuppressed(placement))
        {
            this.chunksByPlacement.put(placement, Set.of());
            return;
        }

        Map<String, Box> boxes = placement.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED);
        Set<Long> touchedChunks = new LinkedHashSet<>();

        for (Map.Entry<String, Box> entry : boxes.entrySet())
        {
            Box box = entry.getValue();
            int minChunkX = Math.min(box.getPos1().getX(), box.getPos2().getX()) >> 4;
            int maxChunkX = Math.max(box.getPos1().getX(), box.getPos2().getX()) >> 4;
            int minChunkZ = Math.min(box.getPos1().getZ(), box.getPos2().getZ()) >> 4;
            int maxChunkZ = Math.max(box.getPos1().getZ(), box.getPos2().getZ()) >> 4;
            CreatorPlacementTarget target = new CreatorPlacementTarget(placement, entry.getKey(), box);

            for (int chunkX = minChunkX; chunkX <= maxChunkX; ++chunkX)
            {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; ++chunkZ)
                {
                    long chunkPos = ChunkPos.pack(chunkX, chunkZ);
                    this.targetsByChunk.computeIfAbsent(chunkPos, key -> new ArrayList<>()).add(target);
                    touchedChunks.add(chunkPos);
                }
            }
        }

        this.chunksByPlacement.put(placement, touchedChunks);
    }

    private void removePlacement(SchematicPlacement placement)
    {
        Set<Long> chunks = this.chunksByPlacement.remove(placement);

        if (chunks == null)
        {
            return;
        }

        for (long chunk : chunks)
        {
            List<CreatorPlacementTarget> targets = this.targetsByChunk.get(chunk);

            if (targets != null)
            {
                targets.removeIf(target -> target.placement() == placement);

                if (targets.isEmpty())
                {
                    this.targetsByChunk.remove(chunk);
                }
            }
        }
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
