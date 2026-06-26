package io.github.urntt.litematicacreator.render;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import fi.dy.masa.litematica.render.infohud.IInfoHudRenderer;
import fi.dy.masa.litematica.render.infohud.RenderPhase;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.urntt.litematicacreator.creator.CreatorDraft;
import io.github.urntt.litematicacreator.creator.CreatorInventory;
import io.github.urntt.litematicacreator.creator.CreatorManager;

public class CreatorStatusHud implements IInfoHudRenderer
{
    public static final CreatorStatusHud INSTANCE = new CreatorStatusHud();

    private CreatorStatusHud()
    {
    }

    @Override
    public boolean getShouldRenderText(RenderPhase phase)
    {
        Minecraft mc = Minecraft.getInstance();
        return phase == RenderPhase.POST && mc.level != null && mc.player != null && CreatorManager.getInstance().isCreatorModeEnabled();
    }

    @Override
    public List<String> getText(RenderPhase phase)
    {
        return this.getShouldRenderText(phase) ? this.getLines() : List.of();
    }

    private List<String> getLines()
    {
        List<String> lines = new ArrayList<>();
        CreatorInventory inventory = CreatorInventory.getInstance();
        CreatorDraft draft = CreatorManager.getInstance().getCurrentDraft();
        ItemStack selectedStack = inventory.getSelectedStack();
        String green = GuiBase.TXT_GREEN;
        String red = GuiBase.TXT_RED;
        String yellow = GuiBase.TXT_GOLD;
        String reset = GuiBase.TXT_RST;
        String blockName = selectedStack.isEmpty() ?
                StringUtils.translate("litematica-creator.hud.empty") :
                BuiltInRegistries.ITEM.getKey(selectedStack.getItem()).toString();

        lines.add(StringUtils.translate("litematica-creator.hud.title", green + StringUtils.translate("litematica-creator.hud.enabled") + reset));
        lines.add(StringUtils.translate("litematica-creator.hud.selected_slot", green + (inventory.getSelectedHotbarSlot() + 1) + reset));
        lines.add(StringUtils.translate("litematica-creator.hud.selected_block", yellow + blockName + reset));

        if (draft == null)
        {
            lines.add(StringUtils.translate("litematica-creator.hud.draft_missing", red + StringUtils.translate("litematica-creator.hud.none") + reset));
        }
        else
        {
            String dirty = draft.isDirty() ? red + StringUtils.translate("litematica-creator.hud.dirty") + reset : green + StringUtils.translate("litematica-creator.hud.saved") + reset;
            lines.add(StringUtils.translate("litematica-creator.hud.draft_name", green + draft.getPlacement().getName() + reset));
            lines.add(StringUtils.translate("litematica-creator.hud.draft_state", dirty));
            lines.add(StringUtils.translate("litematica-creator.hud.draft_counts", green + draft.getTileCount() + reset, green + draft.getTotalBlocks() + reset));
        }

        return lines;
    }
}
