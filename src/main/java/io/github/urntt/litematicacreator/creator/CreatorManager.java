package io.github.urntt.litematicacreator.creator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import io.github.urntt.litematicacreator.config.Configs;

public class CreatorManager
{
    private static final CreatorManager INSTANCE = new CreatorManager();
    private static final DateTimeFormatter NAME_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @Nullable
    private CreatorDraft currentDraft;

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
    }

    @Nullable
    public CreatorDraft getCurrentDraft()
    {
        return this.currentDraft;
    }

    @Nullable
    public CreatorDraft getOrCreateDraft(BlockPos seedWorldPos)
    {
        if (this.currentDraft == null)
        {
            this.currentDraft = CreatorDraft.create(this.createDraftName(), seedWorldPos, this.getAuthorName());

            if (this.currentDraft != null)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.SUCCESS, "litematica-creator.message.draft.created", this.currentDraft.getPlacement().getName());
            }
        }

        return this.currentDraft;
    }

    public boolean discardCurrentDraft()
    {
        if (this.currentDraft == null)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica-creator.message.draft.missing");
            return false;
        }

        SchematicHolder.getInstance().removeSchematic(this.currentDraft.getSchematic());
        this.currentDraft = null;
        InfoUtils.showGuiOrInGameMessage(MessageType.SUCCESS, "litematica-creator.message.draft.discarded");
        return true;
    }

    public boolean saveCurrentDraft()
    {
        if (this.currentDraft == null)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica-creator.message.draft.missing");
            return false;
        }

        String fileName = this.currentDraft.getPlacement().getName();
        boolean saved = this.currentDraft.getSchematic().writeToFile(DataManager.getSchematicsBaseDirectory(), fileName, true);

        if (saved)
        {
            this.currentDraft.markSaved();
            InfoUtils.showGuiOrInGameMessage(MessageType.SUCCESS, "litematica-creator.message.draft.saved", fileName);
        }

        return saved;
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
