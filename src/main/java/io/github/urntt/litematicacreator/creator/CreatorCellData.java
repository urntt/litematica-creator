package io.github.urntt.litematicacreator.creator;

import java.util.Map;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity NBT and scheduled ticks belong to the block stored at the same container position.
 * They stay valid while only that block's state changes, and become stale once a different block
 * (including air) takes the cell.
 */
final class CreatorCellData
{
    private CreatorCellData()
    {
    }

    static boolean isInvalidatedBy(BlockState oldState, BlockState newState)
    {
        return oldState.getBlock() != newState.getBlock();
    }

    static boolean clear(
            BlockPos containerPos,
            @Nullable Map<BlockPos, ?> blockEntities,
            @Nullable Map<BlockPos, ?> pendingBlockTicks,
            @Nullable Map<BlockPos, ?> pendingFluidTicks)
    {
        boolean removedBlockEntity = remove(blockEntities, containerPos);
        boolean removedBlockTick = remove(pendingBlockTicks, containerPos);
        boolean removedFluidTick = remove(pendingFluidTicks, containerPos);
        return removedBlockEntity || removedBlockTick || removedFluidTick;
    }

    private static boolean remove(@Nullable Map<BlockPos, ?> values, BlockPos containerPos)
    {
        return values != null && values.remove(containerPos) != null;
    }
}
