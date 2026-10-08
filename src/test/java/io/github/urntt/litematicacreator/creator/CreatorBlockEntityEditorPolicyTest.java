package io.github.urntt.litematicacreator.creator;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatorBlockEntityEditorPolicyTest
{
    @BeforeAll
    static void bootstrapMinecraft()
    {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void mapsSupportedBlocksToVanillaEditors()
    {
        assertEquals(CreatorBlockEntityEditorPolicy.Editor.SIGN, CreatorBlockEntityEditorPolicy.editorFor(Blocks.OAK_SIGN.defaultBlockState()));
        assertEquals(CreatorBlockEntityEditorPolicy.Editor.HANGING_SIGN,
                CreatorBlockEntityEditorPolicy.editorFor(Blocks.OAK_HANGING_SIGN.defaultBlockState()));
        assertEquals(CreatorBlockEntityEditorPolicy.Editor.COMMAND_BLOCK,
                CreatorBlockEntityEditorPolicy.editorFor(Blocks.CHAIN_COMMAND_BLOCK.defaultBlockState()));
        assertEquals(CreatorBlockEntityEditorPolicy.Editor.CHEST, CreatorBlockEntityEditorPolicy.editorFor(Blocks.TRAPPED_CHEST.defaultBlockState()));
        assertEquals(CreatorBlockEntityEditorPolicy.Editor.BARREL, CreatorBlockEntityEditorPolicy.editorFor(Blocks.BARREL.defaultBlockState()));
        assertEquals(CreatorBlockEntityEditorPolicy.Editor.SHULKER_BOX,
                CreatorBlockEntityEditorPolicy.editorFor(Blocks.SHULKER_BOX.defaultBlockState()));
        assertEquals(CreatorBlockEntityEditorPolicy.Editor.HOPPER, CreatorBlockEntityEditorPolicy.editorFor(Blocks.HOPPER.defaultBlockState()));
        assertEquals(CreatorBlockEntityEditorPolicy.Editor.DISPENSER, CreatorBlockEntityEditorPolicy.editorFor(Blocks.DROPPER.defaultBlockState()));
    }

    @Test
    void leavesOtherBlocksToPlacement()
    {
        assertNull(CreatorBlockEntityEditorPolicy.editorFor(Blocks.STONE.defaultBlockState()));
        assertNull(CreatorBlockEntityEditorPolicy.editorFor(Blocks.FURNACE.defaultBlockState()));
        assertNull(CreatorBlockEntityEditorPolicy.editorFor(Blocks.LECTERN.defaultBlockState()));
    }

    @Test
    void sneakingPlacesInsteadOfOpening()
    {
        assertTrue(CreatorBlockEntityEditorPolicy.opens(CreatorBlockEntityEditorPolicy.Editor.CHEST, false, true));
        assertFalse(CreatorBlockEntityEditorPolicy.opens(CreatorBlockEntityEditorPolicy.Editor.CHEST, true, true));
        // As in vanilla, sneaking with empty hands still opens the block.
        assertTrue(CreatorBlockEntityEditorPolicy.opens(CreatorBlockEntityEditorPolicy.Editor.CHEST, true, false));
        assertFalse(CreatorBlockEntityEditorPolicy.opens(null, false, true));
    }

    @Test
    void commandBlockModeChoosesTheBlockAndKeepsFacing()
    {
        BlockState east = Blocks.COMMAND_BLOCK.defaultBlockState().setValue(CommandBlock.FACING, Direction.EAST);

        BlockState chain = CreatorBlockEntityEditorPolicy.editedCommandBlock(east, CommandBlockEntity.Mode.SEQUENCE, true);
        BlockState repeating = CreatorBlockEntityEditorPolicy.editedCommandBlock(east, CommandBlockEntity.Mode.AUTO, false);
        BlockState impulse = CreatorBlockEntityEditorPolicy.editedCommandBlock(chain, CommandBlockEntity.Mode.REDSTONE, false);

        assertTrue(chain.is(Blocks.CHAIN_COMMAND_BLOCK) && chain.getValue(CommandBlock.CONDITIONAL));
        assertTrue(repeating.is(Blocks.REPEATING_COMMAND_BLOCK) && !repeating.getValue(CommandBlock.CONDITIONAL));
        assertEquals(east, impulse);
        assertEquals(Direction.EAST, chain.getValue(CommandBlock.FACING));
    }
}
