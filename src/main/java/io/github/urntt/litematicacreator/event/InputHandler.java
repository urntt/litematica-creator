package io.github.urntt.litematicacreator.event;

import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;
import fi.dy.masa.malilib.hotkeys.IKeyboardInputHandler;
import fi.dy.masa.malilib.hotkeys.IMouseInputHandler;
import fi.dy.masa.malilib.util.GuiUtils;
import io.github.urntt.litematicacreator.Reference;
import io.github.urntt.litematicacreator.config.Hotkeys;
import io.github.urntt.litematicacreator.creator.CreatorEditService;
import io.github.urntt.litematicacreator.creator.CreatorInventory;
import io.github.urntt.litematicacreator.creator.CreatorManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

public class InputHandler implements IKeybindProvider, IKeyboardInputHandler, IMouseInputHandler
{
    private static final InputHandler INSTANCE = new InputHandler();

    private InputHandler()
    {
    }

    public static InputHandler getInstance()
    {
        return INSTANCE;
    }

    @Override
    public void addKeysToMap(IKeybindManager manager)
    {
        for (IHotkey hotkey : Hotkeys.HOTKEY_LIST)
        {
            manager.addKeybindToMap(hotkey.getKeybind());
        }
    }

    @Override
    public void addHotkeys(IKeybindManager manager)
    {
        manager.addHotkeysForCategory(
                Reference.MOD_NAME,
                Reference.MOD_ID + ".hotkeys.category.generic_hotkeys",
                Hotkeys.HOTKEY_LIST
        );
    }

    @Override
    public boolean onKeyInput(KeyEvent input, boolean eventKeyState)
    {
        Minecraft mc = Minecraft.getInstance();

        if (eventKeyState && CreatorManager.getInstance().isCreatorModeEnabled() && GuiUtils.getCurrentScreen() == null)
        {
            for (int i = 0; i < mc.options.keyHotbarSlots.length; ++i)
            {
                if (mc.options.keyHotbarSlots[i].matches(input))
                {
                    CreatorInventory.getInstance().setSelectedHotbarSlot(i);
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean onMouseClick(MouseButtonEvent click, boolean eventButtonState)
    {
        Minecraft mc = Minecraft.getInstance();

        if (!eventButtonState || !CreatorManager.getInstance().isCreatorModeEnabled() || GuiUtils.getCurrentScreen() != null)
        {
            return false;
        }

        if (mc.options.keyPickItem.matchesMouse(click))
        {
            return CreatorEditService.getInstance().pickBlock();
        }

        return false;
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double amount)
    {
        if (CreatorManager.getInstance().isCreatorModeEnabled() && GuiUtils.getCurrentScreen() == null)
        {
            CreatorInventory.getInstance().scrollHotbar(amount);
            return true;
        }

        return false;
    }
}
