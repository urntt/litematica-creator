package io.github.urntt.litematicacreator.creator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.SchematicMetadata;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.util.FileType;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import io.github.urntt.litematicacreator.config.Configs;
import io.github.urntt.litematicacreator.mixin.LitematicaSchematicAccessor;
import io.github.urntt.litematicacreator.recovery.CreatorRecoveryManager;

public class CreatorManager
{
    private static final CreatorManager INSTANCE = new CreatorManager();
    private static final DateTimeFormatter NAME_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @Nullable
    private CreatorFocus focus;
    private List<SchematicPlacement> pendingFocusChoices = List.of();

    private CreatorManager()
    {
    }

    public static CreatorManager getInstance()
    {
        return INSTANCE;
    }

    public boolean isCreatorModeEnabled()
    {
        return Configs.Generic.ENABLE_CREATOR_MODE.getBooleanValue();
    }

    public void toggleCreatorMode()
    {
        this.setCreatorModeEnabled(!this.isCreatorModeEnabled(), true);
    }

    public void setCreatorModeEnabled(boolean enabled, boolean notify)
    {
        if (Configs.Generic.ENABLE_CREATOR_MODE.getBooleanValue() != enabled)
        {
            Configs.Generic.ENABLE_CREATOR_MODE.setBooleanValue(enabled);
        }

        if (notify)
        {
            InfoUtils.showGuiOrInGameMessage(
                    MessageType.SUCCESS,
                    enabled ? "litematica-creator.message.creator_mode.enabled" : "litematica-creator.message.creator_mode.disabled"
            );
        }

        if (!enabled)
        {
            CreatorPlacementVisibility.restoreAll();
            this.pendingFocusChoices = List.of();
        }
    }

    @Nullable
    public CreatorFocus getFocus()
    {
        return this.focus;
    }

    public void focusPlacement(SchematicPlacement placement)
    {
        this.changeFocus(placement, FocusNotice.SHOW, true);
    }

    public void restoreFocus(SchematicPlacement placement)
    {
        this.changeFocus(placement, FocusNotice.SILENT, true);
    }

    public void clearFocus()
    {
        this.changeFocus(null, FocusNotice.SHOW, true);
    }

    public void clearFocusSilently()
    {
        this.changeFocus(null, FocusNotice.SILENT, true);
    }

    public void onPlacementRemoved(SchematicPlacement placement)
    {
        if (this.focus != null && this.focus.placement() == placement)
        {
            this.clearFocus();
        }
    }

    public List<SchematicPlacement> getPendingFocusChoices()
    {
        return this.pendingFocusChoices;
    }

    public void requestFocusChoice(List<SchematicPlacement> placements)
    {
        this.pendingFocusChoices = List.copyOf(placements);
    }

    public void cancelFocusChoice()
    {
        this.pendingFocusChoices = List.of();
    }

    public void focusPlacementFromOverlap(SchematicPlacement placement, List<SchematicPlacement> candidates)
    {
        this.changeFocus(placement, FocusNotice.SHOW, false);
        CreatorPlacementVisibility.suppressOverlapAlternatives(placement, candidates);
    }

    public SchematicPlacement createBlank(BlockPos origin)
    {
        String name = this.createDraftName();
        LitematicaSchematic schematic = LitematicaSchematicAccessor.litematicacreator$create(null);
        SchematicMetadata metadata = schematic.getMetadata();
        long now = System.currentTimeMillis();

        metadata.setName(name);
        metadata.setAuthor(this.getAuthorName());
        metadata.setRegionCount(0);
        metadata.setTotalVolume(0);
        metadata.setTotalBlocks(0);
        metadata.setEnclosingSize(BlockPos.ZERO);
        metadata.setTimeCreated(now);
        metadata.setTimeModified(now);
        metadata.setSchematicVersion(LitematicaSchematic.SCHEMATIC_VERSION);
        metadata.setMinecraftDataVersion(LitematicaSchematic.MINECRAFT_DATA_VERSION);
        metadata.setFileType(FileType.LITEMATICA_SCHEMATIC);

        SchematicPlacementManager placementManager = DataManager.getSchematicPlacementManager();
        SchematicPlacement previousSelection = placementManager.getSelectedSchematicPlacement();
        SchematicPlacement placement = SchematicPlacement.createFor(schematic, origin, name, true, true);
        SchematicHolder.getInstance().addSchematic(schematic, false);
        placementManager.addSchematicPlacement(placement, false);

        if (Configs.Generic.SELECT_NEW_DRAFT_PLACEMENT.getBooleanValue())
        {
            placementManager.setSelectedSchematicPlacement(placement);
        }
        else
        {
            placementManager.setSelectedSchematicPlacement(previousSelection);
        }

        this.changeFocus(placement, FocusNotice.SILENT, true);
        InfoUtils.showGuiOrInGameMessage(
                MessageType.SUCCESS,
                "litematica-creator.message.draft.created",
                placement.getName(),
                schematic.getMetadata().getName()
        );
        return placement;
    }

    /** Retains the old config action name while changing its behavior to finish editing. */
    public boolean saveCurrentDraft()
    {
        if (this.focus == null)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica-creator.message.draft.missing");
            return false;
        }

        String name = this.focus.placement().getName();
        this.clearFocusSilently();
        InfoUtils.showGuiOrInGameMessage(MessageType.SUCCESS, "litematica-creator.message.draft.saved", name);
        return true;
    }

    public boolean discardCurrentDraft()
    {
        if (this.focus == null)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica-creator.message.draft.missing");
            return false;
        }

        LitematicaSchematic schematic = this.focus.schematic();
        this.clearFocusSilently();
        CreatorRecoveryManager.getInstance().discardSchematic(schematic);
        SchematicHolder.getInstance().removeSchematic(schematic);
        InfoUtils.showGuiOrInGameMessage(MessageType.SUCCESS, "litematica-creator.message.draft.discarded");
        return true;
    }

    private boolean changeFocus(
            @Nullable SchematicPlacement placement,
            FocusNotice notice,
            boolean restoreVisibility)
    {
        CreatorFocus previous = this.focus;
        @Nullable SchematicPlacement previousPlacement = previous != null ? previous.placement() : null;
        CreatorFocusChangePolicy.Decision decision = CreatorFocusChangePolicy.decide(
                previousPlacement,
                placement,
                notice == FocusNotice.SHOW
        );

        this.pendingFocusChoices = List.of();

        if (!decision.changed())
        {
            return false;
        }

        if (restoreVisibility)
        {
            CreatorPlacementVisibility.restoreAll();
        }

        this.focus = placement != null ? new CreatorFocus(placement) : null;
        CreatorRecoveryManager.getInstance().onFocusChanged(previous, this.focus);

        if (decision.notifyPlayer())
        {
            if (this.focus != null)
            {
                InfoUtils.showGuiOrInGameMessage(
                        MessageType.SUCCESS,
                        "litematica-creator.message.focus.changed",
                        this.focus.placement().getName(),
                        this.focus.schematic().getMetadata().getName()
                );
            }
            else
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.SUCCESS, "litematica-creator.message.focus.cleared");
            }
        }

        return true;
    }

    private enum FocusNotice
    {
        SHOW,
        SILENT
    }

    private String createDraftName()
    {
        return "creator-draft-" + LocalDateTime.now().format(NAME_TIME_FORMAT);
    }

    private String getAuthorName()
    {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null ? mc.player.getGameProfile().name() : "Player";
    }
}
