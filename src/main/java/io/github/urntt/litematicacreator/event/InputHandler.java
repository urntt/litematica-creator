package io.github.urntt.litematicacreator.event;

import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;
import fi.dy.masa.malilib.hotkeys.IKeyboardInputHandler;
import fi.dy.masa.malilib.hotkeys.IMouseInputHandler;
import fi.dy.masa.malilib.util.InfoUtils;
import io.github.urntt.litematicacreator.Reference;
import io.github.urntt.litematicacreator.config.Configs;
import io.github.urntt.litematicacreator.config.Hotkeys;
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
            toggleCreatorMode();
            return true;
        }

        return false;
    }

    private static void toggleCreatorMode()
    {
        boolean enabled = !Configs.Generic.ENABLE_CREATOR_MODE.getBooleanValue();
        Configs.Generic.ENABLE_CREATOR_MODE.setBooleanValue(enabled);

        InfoUtils.showGuiOrInGameMessage(
                MessageType.SUCCESS,
                enabled ? "litematica-creator.message.creator_mode.enabled" : "litematica-creator.message.creator_mode.disabled"
        );
    }
}
