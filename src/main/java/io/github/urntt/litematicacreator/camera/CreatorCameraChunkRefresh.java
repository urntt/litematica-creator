package io.github.urntt.litematicacreator.camera;

import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public final class CreatorCameraChunkRefresh
{
    private CreatorCameraChunkRefresh()
    {
    }

    public static void markTransition(Minecraft minecraft, int newChunkX, int newChunkZ, int oldChunkX, int oldChunkZ)
    {
        ClientLevel level = minecraft.level;

        if (level == null || (newChunkX == oldChunkX && newChunkZ == oldChunkZ))
        {
            return;
        }

        int viewDistance = minecraft.options.getEffectiveRenderDistance();

        for (ChunkCoordinate chunk : exposedChunks(newChunkX, newChunkZ, oldChunkX, oldChunkZ, viewDistance))
        {
            if (level.getChunkSource().getChunk(chunk.x(), chunk.z(), ChunkStatus.FULL, false) == null)
            {
                continue;
            }

            for (int sectionY = level.getMinSectionY(); sectionY <= level.getMaxSectionY(); ++sectionY)
            {
                minecraft.levelExtractor.setSectionDirty(chunk.x(), sectionY, chunk.z());
            }
        }
    }

    static Set<ChunkCoordinate> exposedChunks(
            int newChunkX,
            int newChunkZ,
            int oldChunkX,
            int oldChunkZ,
            int viewDistance)
    {
        Set<ChunkCoordinate> chunks = new LinkedHashSet<>();

        for (int chunkZ = newChunkZ - viewDistance; chunkZ <= newChunkZ + viewDistance; ++chunkZ)
        {
            for (int chunkX = newChunkX - viewDistance; chunkX <= newChunkX + viewDistance; ++chunkX)
            {
                boolean insideOldView = chunkX >= oldChunkX - viewDistance &&
                                        chunkX <= oldChunkX + viewDistance &&
                                        chunkZ >= oldChunkZ - viewDistance &&
                                        chunkZ <= oldChunkZ + viewDistance;

                if (!insideOldView)
                {
                    chunks.add(new ChunkCoordinate(chunkX, chunkZ));
                }
            }
        }

        return Set.copyOf(chunks);
    }

    record ChunkCoordinate(int x, int z)
    {
    }
}
