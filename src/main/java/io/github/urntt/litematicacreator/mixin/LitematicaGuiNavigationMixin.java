package io.github.urntt.litematicacreator.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.litematica.gui.GuiMainMenu;
import fi.dy.masa.litematica.gui.GuiSchematicLoadedList;
import fi.dy.masa.litematica.gui.GuiSchematicPlacementsList;
import fi.dy.masa.litematica.selection.SelectionMode;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.urntt.litematicacreator.gui.CreatorSchematicManagerScreen;

@Mixin({ GuiMainMenu.class, GuiSchematicLoadedList.class, GuiSchematicPlacementsList.class })
public abstract class LitematicaGuiNavigationMixin extends GuiBase
{
    @Inject(method = "initGui", at = @At("RETURN"), remap = false)
    private void litematicacreator$addManagerButton(CallbackInfo ci)
    {
        String label = StringUtils.translate("litematica-creator.gui.manager.open");
        int width;
        int x;
        int y;

        if ((Object) this instanceof GuiMainMenu)
        {
            width = this.litematicacreator$getMainMenuColumnWidth();
            x = 32 + width;
            y = 52;
        }
        else if ((Object) this instanceof GuiSchematicLoadedList)
        {
            width = this.getStringWidth(label) + 20;
            x = 12 + this.litematicacreator$getMenuButtonWidth(GuiMainMenu.ButtonListenerChangeMenu.ButtonType.LOAD_SCHEMATICS) + 4;
            x += this.litematicacreator$getMenuButtonWidth(GuiMainMenu.ButtonListenerChangeMenu.ButtonType.SCHEMATIC_PLACEMENTS) + 4;
            y = this.getScreenHeight() - 26;
        }
        else
        {
            width = this.getStringWidth(label) + 20;
            x = 12 + this.litematicacreator$getMenuButtonWidth(GuiMainMenu.ButtonListenerChangeMenu.ButtonType.LOADED_SCHEMATICS) + 4;
            y = this.getScreenHeight() - 26;
        }

        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, label);
        button.setHoverStrings("litematica-creator.gui.manager.hover.open");
        this.addButton(button, (pressed, mouseButton) -> CreatorSchematicManagerScreen.openFromLitematica((GuiBase) (Object) this));
    }

    @Unique
    private int litematicacreator$getMenuButtonWidth(GuiMainMenu.ButtonListenerChangeMenu.ButtonType type)
    {
        return this.getStringWidth(StringUtils.translate(type.getLabelKey())) + 30;
    }

    @Unique
    private int litematicacreator$getMainMenuColumnWidth()
    {
        int width = 0;

        for (GuiMainMenu.ButtonListenerChangeMenu.ButtonType type : GuiMainMenu.ButtonListenerChangeMenu.ButtonType.values())
        {
            width = Math.max(width, this.getStringWidth(type.getDisplayName()) + 30);
        }

        for (SelectionMode mode : SelectionMode.values())
        {
            String label = StringUtils.translate("litematica.gui.button.area_selection_mode", mode.getDisplayName());
            width = Math.max(width, this.getStringWidth(label) + 10);
        }

        return width;
    }
}
