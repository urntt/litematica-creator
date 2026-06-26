package io.github.urntt.litematicacreator.render;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;

import fi.dy.masa.malilib.config.HudAlignment;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.urntt.litematicacreator.creator.CreatorDraft;
import io.github.urntt.litematicacreator.creator.CreatorInventory;
import io.github.urntt.litematicacreator.creator.CreatorManager;

public class CreatorStatusHud implements IRenderer
{
    public static final CreatorStatusHud INSTANCE = new CreatorStatusHud();

    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int BACKGROUND_COLOR = 0xA0000000;

    private CreatorStatusHud()
    {
    }

    @Override
    public void onExtractGuiOverlayPost(GuiContext ctx, float partialTicks, ProfilerFiller profiler)
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null || !CreatorManager.getInstance().isCreatorModeEnabled())
        {
            return;
        }

        List<String> lines = this.getLines();
        RenderUtils.renderText(ctx, 4, 4, 1.0D, TEXT_COLOR, BACKGROUND_COLOR, HudAlignment.TOP_LEFT, true, true, lines);
    }

    @Override
    public Supplier<String> getProfilerSectionSupplier()
    {
        return () -> "litematica_creator_status_hud";
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
