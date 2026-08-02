package io.github.urntt.litematicacreator.creator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

record CreatorEditTarget(
        BlockPos blockPos,
        BlockPos clickedBlockPos,
        Direction side,
        Vec3 hitVec,
        boolean schematicBlock,
        boolean airTarget)
{
    CreatorEditTarget
    {
        blockPos = blockPos.immutable();
        clickedBlockPos = clickedBlockPos.immutable();
    }

    CreatorEditTargetKey key()
    {
        return new CreatorEditTargetKey(this.blockPos, this.clickedBlockPos, this.side, this.schematicBlock, this.airTarget);
    }

    record CreatorEditTargetKey(
            BlockPos blockPos,
            BlockPos clickedBlockPos,
            Direction side,
            boolean schematicBlock,
            boolean airTarget)
    {
    }
}
