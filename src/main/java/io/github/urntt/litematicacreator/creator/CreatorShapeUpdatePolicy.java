package io.github.urntt.litematicacreator.creator;

import net.minecraft.world.level.block.state.BlockState;

/**
 * Which shape updates a projection placement keeps. After a placement, each directly adjacent projection block runs
 * one vanilla shape update so connections such as fences, walls and stairs follow the new block; nothing chains
 * further, and a placement never removes a projection block.
 */
final class CreatorShapeUpdatePolicy
{
    private CreatorShapeUpdatePolicy()
    {
    }

    /** Only projection blocks that the placement itself left alone follow it; air has no shape to update. */
    static boolean updates(boolean neighbourWritten, BlockState neighbour)
    {
        return !neighbourWritten && !neighbour.isAir();
    }

    /** An update is kept when it changes the neighbour, unless it would remove the neighbour altogether. */
    static boolean keeps(BlockState before, BlockState after)
    {
        return !after.equals(before) && !after.isAir();
    }
}
