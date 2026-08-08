package io.github.urntt.litematicacreator.export;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class CreatorCuboidPartitionerTest
{
    @Test
    void mergesAFilledCubeIntoOneCuboid()
    {
        Set<BlockPos> occupied = new HashSet<>();

        for (int y = 0; y < 2; y++)
        {
            for (int z = 0; z < 2; z++)
            {
                for (int x = 0; x < 2; x++)
                {
                    occupied.add(new BlockPos(x, y, z));
                }
            }
        }

        assertEquals(List.of(new CreatorCuboid(BlockPos.ZERO, new BlockPos(1, 1, 1))), CreatorCuboidPartitioner.partition(occupied));
    }

    @Test
    void splitsLShapesWithoutIntroducingHoles()
    {
        Set<BlockPos> occupied = Set.of(
                BlockPos.ZERO,
                new BlockPos(1, 0, 0),
                new BlockPos(0, 0, 1)
        );

        List<CreatorCuboid> result = CreatorCuboidPartitioner.partition(occupied);

        assertEquals(2, result.size());
        assertEquals(occupied, expand(result));
    }

    @Test
    void doesNotMergeEdgeCornerOrDistantContacts()
    {
        Set<BlockPos> occupied = Set.of(
                BlockPos.ZERO,
                new BlockPos(1, 1, 0),
                new BlockPos(3, 3, 3)
        );

        List<CreatorCuboid> result = CreatorCuboidPartitioner.partition(occupied);

        assertEquals(3, result.size());
        assertEquals(occupied, expand(result));
    }

    @Test
    void hollowShapeRemainsExactlyHollow()
    {
        Set<BlockPos> occupied = new HashSet<>();

        for (int z = 0; z < 3; z++)
        {
            for (int x = 0; x < 3; x++)
            {
                if (x != 1 || z != 1)
                {
                    occupied.add(new BlockPos(x, 0, z));
                }
            }
        }

        List<CreatorCuboid> result = CreatorCuboidPartitioner.partition(occupied);

        assertEquals(occupied, expand(result));
        assertEquals(8, result.stream().mapToInt(CreatorCuboid::volume).sum());
    }

    private static Set<BlockPos> expand(List<CreatorCuboid> cuboids)
    {
        Set<BlockPos> result = new HashSet<>();

        for (CreatorCuboid cuboid : cuboids)
        {
            for (int y = cuboid.min().getY(); y <= cuboid.max().getY(); y++)
            {
                for (int z = cuboid.min().getZ(); z <= cuboid.max().getZ(); z++)
                {
                    for (int x = cuboid.min().getX(); x <= cuboid.max().getX(); x++)
                    {
                        result.add(new BlockPos(x, y, z));
                    }
                }
            }
        }

        return result;
    }
}
