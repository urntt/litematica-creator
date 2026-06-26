package io.github.urntt.litematicacreator.event;

import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;
import fi.dy.masa.malilib.hotkeys.IKeyboardInputHandler;
import fi.dy.masa.malilib.hotkeys.IMouseInputHandler;
import io.github.urntt.litematicacreator.Reference;
import io.github.urntt.litematicacreator.config.Hotkeys;
import io.github.urntt.litematicacreator.creator.CreatorManager;
import net.minecraft.client.input.KeyEvent;

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
        if (eventKeyState && Hotkeys.TOGGLE_CREATOR_MODE.getKeybind().matches(input.key()))
        {
            CreatorManager.getInstance().toggleCreatorMode();
            return true;
        }

        return false;
    }
}
