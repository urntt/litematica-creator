package io.github.urntt.litematicacreator.export;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.ScheduledTick;

import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;

record CreatorRegionSnapshot(
        String name,
        BlockPos position,
        BlockPos signedSize,
        LitematicaBlockStateContainer blocks,
        Map<BlockPos, CompoundTag> blockEntities,
        List<CreatorEntitySnapshot> entities,
        Map<BlockPos, ScheduledTick<Block>> blockTicks,
        Map<BlockPos, ScheduledTick<Fluid>> fluidTicks)
{
    CreatorRegionSnapshot copy()
    {
        return new CreatorRegionSnapshot(
                this.name,
                this.position,
                this.signedSize,
                copyContainer(this.blocks),
                copyBlockEntities(this.blockEntities),
                copyEntities(this.entities),
                new HashMap<>(this.blockTicks),
                new HashMap<>(this.fluidTicks)
        );
    }

    BlockPos min()
    {
        BlockPos end = this.position.offset(relativeEnd(this.signedSize));
        return new BlockPos(
                Math.min(this.position.getX(), end.getX()),
                Math.min(this.position.getY(), end.getY()),
                Math.min(this.position.getZ(), end.getZ())
        );
    }

    BlockPos max()
    {
        BlockPos min = this.min();
        Vec3i size = this.blocks.getSize();
        return min.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1);
    }

    boolean isUnitCell()
    {
        Vec3i size = this.blocks.getSize();
        return size.getX() == 1 && size.getY() == 1 && size.getZ() == 1;
    }

    boolean hasAttachedData()
    {
        return !this.blockEntities.isEmpty() ||
               !this.entities.isEmpty() ||
               !this.blockTicks.isEmpty() ||
               !this.fluidTicks.isEmpty();
    }

    static LitematicaBlockStateContainer copyContainer(LitematicaBlockStateContainer source)
    {
        Vec3i size = source.getSize();
        LitematicaBlockStateContainer copy = new LitematicaBlockStateContainer(size.getX(), size.getY(), size.getZ());

        for (int y = 0; y < size.getY(); y++)
        {
            for (int z = 0; z < size.getZ(); z++)
            {
                for (int x = 0; x < size.getX(); x++)
                {
                    copy.set(x, y, z, source.get(x, y, z));
                }
            }
        }

        return copy;
    }

    static Map<BlockPos, CompoundTag> copyBlockEntities(Map<BlockPos, CompoundTag> source)
    {
        Map<BlockPos, CompoundTag> copy = new HashMap<>();
        source.forEach((pos, tag) -> copy.put(pos, tag.copy()));
        return copy;
    }

    static List<CreatorEntitySnapshot> copyEntities(List<CreatorEntitySnapshot> source)
    {
        List<CreatorEntitySnapshot> copy = new ArrayList<>(source.size());

        for (CreatorEntitySnapshot info : source)
        {
            copy.add(info.copy());
        }

        return copy;
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
