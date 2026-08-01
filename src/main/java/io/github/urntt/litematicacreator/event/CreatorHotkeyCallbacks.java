package io.github.urntt.litematicacreator.event;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import io.github.urntt.litematicacreator.config.Hotkeys;
import io.github.urntt.litematicacreator.creator.CreatorManager;
import io.github.urntt.litematicacreator.creator.CreatorCameraCompat;
import io.github.urntt.litematicacreator.data.DataManager;
import io.github.urntt.litematicacreator.gui.GuiConfigs;
import io.github.urntt.litematicacreator.gui.GuiCreatorInventory;
import io.github.urntt.litematicacreator.gui.GuiFocusSwitcher;

public class CreatorHotkeyCallbacks implements IHotkeyCallback
{
    public static final CreatorHotkeyCallbacks INSTANCE = new CreatorHotkeyCallbacks();

    private CreatorHotkeyCallbacks()
    {
    }

    public static void register()
    {
        Hotkeys.TOGGLE_CREATOR_MODE.getKeybind().setCallback(INSTANCE);
        Hotkeys.OPEN_CREATOR_INVENTORY.getKeybind().setCallback(INSTANCE);
        Hotkeys.OPEN_CONFIG_GUI.getKeybind().setCallback(INSTANCE);
        Hotkeys.SAVE_DRAFT.getKeybind().setCallback(INSTANCE);
        Hotkeys.DISCARD_DRAFT.getKeybind().setCallback(INSTANCE);
        Hotkeys.OPEN_FOCUS_SWITCHER.getKeybind().setCallback(INSTANCE);
        Hotkeys.NEW_BLANK.getKeybind().setCallback(INSTANCE);
    }

    @Override
    public boolean onKeyAction(KeyAction action, IKeybind key)
    {
        if (key == Hotkeys.TOGGLE_CREATOR_MODE.getKeybind())
        {
            CreatorManager.getInstance().toggleCreatorMode();
        }
        else if (key == Hotkeys.OPEN_CREATOR_INVENTORY.getKeybind())
        {
            GuiCreatorInventory.openFromHotkey();
        }
        else if (key == Hotkeys.OPEN_CONFIG_GUI.getKeybind())
        {
            DataManager.setConfigGuiTab(GuiConfigs.ConfigGuiTab.ALL);
            GuiBase.openGui(new GuiConfigs());
        }
        else if (key == Hotkeys.SAVE_DRAFT.getKeybind())
        {
            CreatorManager.getInstance().saveCurrentDraft();
        }
        else if (key == Hotkeys.DISCARD_DRAFT.getKeybind())
        {
            CreatorManager.getInstance().discardCurrentDraft();
        }
        else if (key == Hotkeys.OPEN_FOCUS_SWITCHER.getKeybind())
        {
            if (CreatorManager.getInstance().isCreatorModeEnabled())
            {
                GuiFocusSwitcher.open();
            }
        }
        else if (key == Hotkeys.NEW_BLANK.getKeybind())
        {
            if (CreatorManager.getInstance().isCreatorModeEnabled() && CreatorCameraCompat.getCameraEntity() != null)
            {
                CreatorManager.getInstance().createBlank(CreatorCameraCompat.getCameraEntity().blockPosition());
            }
        }
        else
        {
            return false;
        }

        return true;
    }
}
