package io.github.urntt.litematicacreator.creator;

import java.util.Optional;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity data as Creator stores it in projections. Only data that differs from a fresh block entity of the same
 * state is kept, so plain chests or signs do not fill projections with default data.
 */
final class CreatorBlockEntityData
{
    private CreatorBlockEntityData()
    {
    }

    /** The data worth storing for a block entity, or empty when it matches a fresh block entity of the same state. */
    static Optional<CompoundTag> persistent(BlockEntity blockEntity, HolderLookup.Provider registries)
    {
        CompoundTag data = blockEntity.saveWithFullMetadata(registries);
        @Nullable BlockEntity fresh = create(blockEntity.getBlockPos(), blockEntity.getBlockState());
        return fresh != null && fresh.saveWithFullMetadata(registries).equals(data) ? Optional.empty() : Optional.of(data);
    }

    @Nullable
    static BlockEntity create(BlockPos pos, BlockState state)
    {
        return state.getBlock() instanceof EntityBlock block ? block.newBlockEntity(pos, state) : null;
    }
}
