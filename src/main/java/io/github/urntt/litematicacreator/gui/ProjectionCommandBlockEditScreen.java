package io.github.urntt.litematicacreator.gui;

import java.util.Map;

import net.minecraft.client.gui.screens.inventory.CommandBlockEditScreen;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import io.github.urntt.litematicacreator.creator.CreatorBlockEntityEditSession;
import io.github.urntt.litematicacreator.creator.CreatorBlockEntityEditorPolicy;
import io.github.urntt.litematicacreator.mixin.CommandBlockEditScreenAccessor;

/**
 * The vanilla command block editor for a projection. Confirming applies the edit the way vanilla's server does and
 * commits it to the schematic; cancelling discards it. Command suggestions keep vanilla's client behavior.
 */
public final class ProjectionCommandBlockEditScreen extends CommandBlockEditScreen
{
    private final CreatorBlockEntityEditSession session;
    private final CommandBlockEntity commandBlock;

    public ProjectionCommandBlockEditScreen(CreatorBlockEntityEditSession session, CommandBlockEntity commandBlock)
    {
        super(commandBlock);
        this.session = session;
        this.commandBlock = commandBlock;
    }

    @Override
    protected void populateAndSendPacket()
    {
        CommandBlockEditScreenAccessor controls = (CommandBlockEditScreenAccessor) (Object) this;
        BlockState current = this.commandBlock.getBlockState();
        BlockState edited = CreatorBlockEntityEditorPolicy.editedCommandBlock(
                current,
                controls.litematicacreator$getMode(),
                controls.litematicacreator$isConditional()
        );
        BaseCommandBlock logic = this.commandBlock.getCommandBlock();
        logic.setCommand(this.commandEdit.getValue());

        if (!logic.isTrackOutput())
        {
            logic.setLastOutput(null);
        }

        this.commandBlock.setAutomatic(controls.litematicacreator$isAutoexec());
        this.commandBlock.setBlockState(edited);
        this.session.commit(edited.equals(current) ? Map.of() : Map.of(this.commandBlock.getBlockPos(), edited));
    }

    // Confirming has already committed; closing any other way cancels the edit, as in vanilla.
    @Override
    public void removed()
    {
        super.removed();
        this.session.cancel();
    }
}
