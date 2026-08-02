package io.github.urntt.litematicacreator.camera;

import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatorCameraChunkRefreshTest
{
    @Test
    void movingOneChunkReturnsOnlyTheNewLeadingEdge()
    {
        Set<CreatorCameraChunkRefresh.ChunkCoordinate> chunks = CreatorCameraChunkRefresh.exposedChunks(1, 0, 0, 0, 1);

        assertEquals(3, chunks.size());
        assertTrue(chunks.contains(new CreatorCameraChunkRefresh.ChunkCoordinate(2, -1)));
        assertTrue(chunks.contains(new CreatorCameraChunkRefresh.ChunkCoordinate(2, 0)));
        assertTrue(chunks.contains(new CreatorCameraChunkRefresh.ChunkCoordinate(2, 1)));
    }

    @Test
    void unchangedCameraHasNoExposedChunks()
    {
        assertTrue(CreatorCameraChunkRefresh.exposedChunks(4, -2, 4, -2, 2).isEmpty());
    }

    @Test
    void diagonalMoveReturnsBothLeadingEdgesWithoutDuplicates()
    {
        Set<CreatorCameraChunkRefresh.ChunkCoordinate> chunks = CreatorCameraChunkRefresh.exposedChunks(1, 1, 0, 0, 1);

        assertEquals(5, chunks.size());
        assertTrue(chunks.contains(new CreatorCameraChunkRefresh.ChunkCoordinate(2, 2)));
        assertTrue(chunks.contains(new CreatorCameraChunkRefresh.ChunkCoordinate(0, 2)));
        assertTrue(chunks.contains(new CreatorCameraChunkRefresh.ChunkCoordinate(2, 0)));
    }

    @Test
    void jumpBeyondTheOldViewMarksTheWholeNewView()
    {
        assertEquals(9, CreatorCameraChunkRefresh.exposedChunks(10, 0, 0, 0, 1).size());
    }
}
