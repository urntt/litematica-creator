package io.github.urntt.litematicacreator.export;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;

final class CreatorCuboidPartitioner
{
    private static final Comparator<BlockPos> ORDER = Comparator
            .comparingInt((BlockPos pos) -> pos.getY())
            .thenComparingInt(pos -> pos.getZ())
            .thenComparingInt(pos -> pos.getX());

    private CreatorCuboidPartitioner()
    {
    }

    static List<CreatorCuboid> partition(Set<BlockPos> occupied)
    {
        Set<BlockPos> remaining = new HashSet<>(occupied);
        List<CreatorCuboid> result = new ArrayList<>();

        while (!remaining.isEmpty())
        {
            BlockPos min = remaining.stream().min(ORDER).orElseThrow();
            int maxX = min.getX();

            while (remaining.contains(new BlockPos(maxX + 1, min.getY(), min.getZ())))
            {
                maxX++;
            }

            int maxZ = min.getZ();

            while (hasFullPlane(remaining, min.getX(), maxX, min.getY(), min.getY(), maxZ + 1))
            {
                maxZ++;
            }

            int maxY = min.getY();

            while (hasFullLayer(remaining, min.getX(), maxX, maxY + 1, min.getZ(), maxZ))
            {
                maxY++;
            }

            BlockPos max = new BlockPos(maxX, maxY, maxZ);
            removeCuboid(remaining, min, max);
            result.add(new CreatorCuboid(min, max));
        }

        return List.copyOf(result);
    }

    private static boolean hasFullPlane(Set<BlockPos> occupied, int minX, int maxX, int minY, int maxY, int z)
    {
        for (int y = minY; y <= maxY; y++)
        {
            for (int x = minX; x <= maxX; x++)
            {
                if (!occupied.contains(new BlockPos(x, y, z)))
                {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean hasFullLayer(Set<BlockPos> occupied, int minX, int maxX, int y, int minZ, int maxZ)
    {
        for (int z = minZ; z <= maxZ; z++)
        {
            for (int x = minX; x <= maxX; x++)
            {
                if (!occupied.contains(new BlockPos(x, y, z)))
                {
                    return false;
                }
            }
        }

        return true;
    }

    private static void removeCuboid(Set<BlockPos> occupied, BlockPos min, BlockPos max)
    {
        for (int y = min.getY(); y <= max.getY(); y++)
        {
            for (int z = min.getZ(); z <= max.getZ(); z++)
            {
                for (int x = min.getX(); x <= max.getX(); x++)
                {
                    occupied.remove(new BlockPos(x, y, z));
                }
            }
        }
    }
}
