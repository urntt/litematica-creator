package io.github.urntt.litematicacreator.creator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import io.github.urntt.litematicacreator.gui.CreatorProjectionContainerScreens;
import io.github.urntt.litematicacreator.gui.ProjectionCommandBlockEditScreen;
import io.github.urntt.litematicacreator.gui.ProjectionHangingSignEditScreen;
import io.github.urntt.litematicacreator.gui.ProjectionSignEditScreen;

/**
 * Edits the block entities of a projection block through a vanilla screen. The screen works on detached block entities
 * loaded from the projection, nothing is sent to the server, and the result is committed to the schematic once, when the
 * screen closes. Focus moves only when that commit changes something.
 */
public final class CreatorBlockEntityEditSession
{
    private final SchematicPlacement placement;
    private final CreatorPlacementWorld world;
    private final List<Cell> cells = new ArrayList<>();
    private boolean finished;

    private record Cell(BlockPos pos, BlockState state, BlockEntity blockEntity)
    {
    }

    private CreatorBlockEntityEditSession(Minecraft mc, SchematicPlacement placement)
    {
        this.placement = placement;
        // Detached block entities still need a level, for example to count chest openers; it is never the real one.
        this.world = CreatorPlacementWorld.begin(mc.level, pos -> CreatorSchematicEditor.getBlockState(placement, pos), Map.of());
    }

    /**
     * Opens the vanilla editor for a projection block.
     *
     * @param player the player whose position decides which side of a sign is edited
     * @return whether a screen opened
     */
    static boolean open(Minecraft mc, SchematicPlacement placement, BlockPos pos, BlockState state, Player player)
    {
        @Nullable CreatorBlockEntityEditorPolicy.Editor editor = CreatorBlockEntityEditorPolicy.editorFor(state);

        if (editor == null)
        {
            return false;
        }

        CreatorBlockEntityEditSession session = new CreatorBlockEntityEditSession(mc, placement);
        @Nullable BlockEntity blockEntity = session.load(pos, state);
        @Nullable Screen screen = switch (editor)
        {
            case SIGN -> blockEntity instanceof SignBlockEntity sign ?
                    new ProjectionSignEditScreen(session, sign, sign.getSlotPlayerIsFacing(player)) : null;
            case HANGING_SIGN -> blockEntity instanceof SignBlockEntity sign ?
                    new ProjectionHangingSignEditScreen(session, sign, sign.getSlotPlayerIsFacing(player)) : null;
            case COMMAND_BLOCK -> blockEntity instanceof CommandBlockEntity commandBlock ?
                    new ProjectionCommandBlockEditScreen(session, commandBlock) : null;
            default -> blockEntity instanceof BaseContainerBlockEntity container ?
                    session.containerScreen(mc, editor, container) : null;
        };

        if (screen == null)
        {
            session.world.end();
            return false;
        }

        mc.gui.setScreen(screen);
        return true;
    }

    /**
     * Commits the edited block entities, and any block state the editor changed, to the projection. Only the first call
     * has an effect; data that matches a fresh block entity is removed rather than stored.
     */
    public void commit(Map<BlockPos, BlockState> stateChanges)
    {
        if (this.finished)
        {
            return;
        }

        this.finished = true;
        this.world.end();

        if (!DataManager.getSchematicPlacementManager().getAllSchematicsPlacements().contains(this.placement))
        {
            return;
        }

        RegistryAccess registries = this.world.registryAccess();
        Map<BlockPos, Optional<CompoundTag>> blockEntities = new LinkedHashMap<>();

        for (Cell cell : this.cells)
        {
            if (!CreatorSchematicEditor.getBlockState(this.placement, cell.pos()).is(cell.state().getBlock()))
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica-creator.message.edit.block_entity_changed");
                return;
            }

            blockEntities.put(cell.pos(), CreatorBlockEntityData.persistent(cell.blockEntity(), registries));
        }

        if (CreatorSchematicEditor.setBlockStates(this.placement, stateChanges, blockEntities))
        {
            CreatorManager.getInstance().focusPlacement(this.placement);
        }
    }

    /** Ends the session without committing, for example when an editor is cancelled. */
    public void cancel()
    {
        if (!this.finished)
        {
            this.finished = true;
            this.world.end();
        }
    }

    @Nullable
    private BlockEntity load(BlockPos pos, BlockState state)
    {
        RegistryAccess registries = this.world.registryAccess();
        @Nullable BlockEntity blockEntity = CreatorSchematicEditor.getBlockEntityData(this.placement, pos)
                .map(data -> BlockEntity.loadStatic(pos, state, data, registries))
                .orElse(null);

        if (blockEntity == null)
        {
            blockEntity = CreatorBlockEntityData.create(pos, state);
        }

        if (blockEntity != null)
        {
            blockEntity.setLevel(this.world);
            this.cells.add(new Cell(pos.immutable(), state, blockEntity));
        }

        return blockEntity;
    }

    @Nullable
    private Screen containerScreen(Minecraft mc, CreatorBlockEntityEditorPolicy.Editor editor, BaseContainerBlockEntity blockEntity)
    {
        Container container = blockEntity;
        Component title = blockEntity.getDisplayName();
        @Nullable BaseContainerBlockEntity partner = editor == CreatorBlockEntityEditorPolicy.Editor.CHEST ?
                this.chestPartner(blockEntity) : null;

        if (partner != null)
        {
            // Like vanilla, the right half comes first and a custom name on either half titles the large chest.
            boolean right = blockEntity.getBlockState().getValue(ChestBlock.TYPE) == ChestType.RIGHT;
            BaseContainerBlockEntity first = right ? blockEntity : partner;
            BaseContainerBlockEntity second = right ? partner : blockEntity;
            container = new CompoundContainer(first, second);
            title = first.hasCustomName() ? first.getDisplayName() :
                    second.hasCustomName() ? second.getDisplayName() : Component.translatable("container.chestDouble");
        }

        return CreatorProjectionContainerScreens.create(mc, this, editor, container, partner != null, title);
    }

    @Nullable
    private BaseContainerBlockEntity chestPartner(BaseContainerBlockEntity blockEntity)
    {
        BlockState state = blockEntity.getBlockState();
        ChestType type = state.getValue(ChestBlock.TYPE);

        if (type == ChestType.SINGLE)
        {
            return null;
        }

        BlockPos partnerPos = blockEntity.getBlockPos().relative(ChestBlock.getConnectedDirection(state));
        BlockState partnerState = CreatorSchematicEditor.getBlockState(this.placement, partnerPos);

        if (!partnerState.is(state.getBlock()) ||
            partnerState.getValue(ChestBlock.TYPE) != type.getOpposite() ||
            partnerState.getValue(ChestBlock.FACING) != state.getValue(ChestBlock.FACING))
        {
            return null;
        }

        return this.load(partnerPos, partnerState) instanceof BaseContainerBlockEntity partner ? partner : null;
    }
}
