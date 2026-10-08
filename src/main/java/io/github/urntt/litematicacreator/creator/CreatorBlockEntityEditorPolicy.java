package io.github.urntt.litematicacreator.creator;

import java.util.Map;
import javax.annotation.Nullable;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Which projection blocks open a vanilla editor when used. Furnaces, brewing stands, crafters, lecterns and similar
 * screens depend on recipe books or further server interaction and are not covered.
 */
public final class CreatorBlockEntityEditorPolicy
{
    private static final Map<BlockEntityType<?>, Editor> EDITORS = Map.of(
            BlockEntityTypes.SIGN, Editor.SIGN,
            BlockEntityTypes.HANGING_SIGN, Editor.HANGING_SIGN,
            BlockEntityTypes.COMMAND_BLOCK, Editor.COMMAND_BLOCK,
            BlockEntityTypes.CHEST, Editor.CHEST,
            BlockEntityTypes.TRAPPED_CHEST, Editor.CHEST,
            BlockEntityTypes.BARREL, Editor.BARREL,
            BlockEntityTypes.SHULKER_BOX, Editor.SHULKER_BOX,
            BlockEntityTypes.HOPPER, Editor.HOPPER,
            BlockEntityTypes.DISPENSER, Editor.DISPENSER,
            BlockEntityTypes.DROPPER, Editor.DISPENSER
    );

    private CreatorBlockEntityEditorPolicy()
    {
    }

    public enum Editor
    {
        SIGN,
        HANGING_SIGN,
        COMMAND_BLOCK,
        CHEST,
        BARREL,
        SHULKER_BOX,
        HOPPER,
        DISPENSER
    }

    @Nullable
    public static Editor editorFor(BlockState state)
    {
        for (Map.Entry<BlockEntityType<?>, Editor> entry : EDITORS.entrySet())
        {
            if (entry.getKey().isValid(state))
            {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Like vanilla, using a block opens its screen unless the player sneaks with something in either hand, which places
     * against it instead.
     */
    static boolean opens(@Nullable Editor editor, boolean secondaryUse, boolean holdsItem)
    {
        return editor != null && !(secondaryUse && holdsItem);
    }

    /** The command block that runs in a mode, as vanilla's server applies a command block edit. */
    public static Block commandBlockFor(CommandBlockEntity.Mode mode)
    {
        return switch (mode)
        {
            case SEQUENCE -> Blocks.CHAIN_COMMAND_BLOCK;
            case AUTO -> Blocks.REPEATING_COMMAND_BLOCK;
            case REDSTONE -> Blocks.COMMAND_BLOCK;
        };
    }

    /** A command block's state after an edit: the mode picks the block, while the facing stays. */
    public static BlockState editedCommandBlock(BlockState current, CommandBlockEntity.Mode mode, boolean conditional)
    {
        return commandBlockFor(mode).defaultBlockState()
                .setValue(CommandBlock.FACING, current.getValue(CommandBlock.FACING))
                .setValue(CommandBlock.CONDITIONAL, conditional);
    }
}
