package io.github.urntt.litematicacreator.creator;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatorShapeUpdatePolicyTest
{
    @BeforeAll
    static void bootstrapMinecraft()
    {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void onlyProjectionBlocksThePlacementLeftAloneFollowIt()
    {
        BlockState fence = Blocks.OAK_FENCE.defaultBlockState();

        assertTrue(CreatorShapeUpdatePolicy.updates(false, fence));
        assertFalse(CreatorShapeUpdatePolicy.updates(true, fence));
        assertFalse(CreatorShapeUpdatePolicy.updates(false, Blocks.AIR.defaultBlockState()));
    }

    @Test
    void keepsChangedShapes()
    {
        BlockState fence = Blocks.OAK_FENCE.defaultBlockState();

        assertTrue(CreatorShapeUpdatePolicy.keeps(fence, fence.setValue(CrossCollisionBlock.EAST, true)));
        assertFalse(CreatorShapeUpdatePolicy.keeps(fence, fence));
    }

    @Test
    void neverRemovesTheNeighbour()
    {
        assertFalse(CreatorShapeUpdatePolicy.keeps(Blocks.TORCH.defaultBlockState(), Blocks.AIR.defaultBlockState()));
    }
}
