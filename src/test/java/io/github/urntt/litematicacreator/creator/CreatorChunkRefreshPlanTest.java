package io.github.urntt.litematicacreator.creator;

import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreatorChunkRefreshPlanTest
{
    @Test
    void ordinaryEditRefreshesOnlyActualWorldChunks()
    {
        Set<ChunkPos> chunks = CreatorChunkRefreshPlan.forWorldPositions(List.of(
                new BlockPos(0, 40, 0),
                new BlockPos(15, -20, 15),
                new BlockPos(16, 0, 0),
                new BlockPos(-1, 70, -1)
        ));

        assertEquals(Set.of(new ChunkPos(0, 0), new ChunkPos(1, 0), new ChunkPos(-1, -1)), chunks);
    }

    @Test
    void structuralEditRefreshesOnlyTheChangedRegionChunks()
    {
        ChunkPos rebuild = new ChunkPos(-2, 5);
        ChunkPos unload = new ChunkPos(3, 4);
        ChunkPos unrelatedPlacementChunk = new ChunkPos(9, -7);

        CreatorChunkRefreshPlan.StructuralRefresh refresh = CreatorChunkRefreshPlan.forStructuralChange(
                Set.of(rebuild, unload),
                Set.of(rebuild, unrelatedPlacementChunk)
        );

        assertEquals(Set.of(rebuild), refresh.rebuildChunks());
        assertEquals(Set.of(unload), refresh.unloadChunks());
    }
}
