package io.github.urntt.litematicacreator.config;

import java.util.List;
import com.google.common.collect.ImmutableList;

import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import io.github.urntt.litematicacreator.Reference;

public class Hotkeys
{
    private static final String HOTKEYS_KEY = Reference.MOD_ID + ".config.hotkeys";

    public static final ConfigHotkey TOGGLE_CREATOR_MODE = new ConfigHotkey(
            "toggleCreatorMode", "", KeybindSettings.PRESS_ALLOWEXTRA
    ).apply(HOTKEYS_KEY);
    public static final ConfigHotkey OPEN_CREATOR_INVENTORY = new ConfigHotkey(
            "openCreatorInventory", "", KeybindSettings.PRESS_ALLOWEXTRA
    ).apply(HOTKEYS_KEY);
    public static final ConfigHotkey OPEN_CONFIG_GUI = new ConfigHotkey(
            "openConfigGui", "M,K", KeybindSettings.PRESS_ALLOWEXTRA
    ).apply(HOTKEYS_KEY);
    public static final ConfigHotkey SAVE_DRAFT = new ConfigHotkey(
            "saveDraft", "", KeybindSettings.PRESS_ALLOWEXTRA
    ).apply(HOTKEYS_KEY);
    public static final ConfigHotkey DISCARD_DRAFT = new ConfigHotkey(
            "discardDraft", "", KeybindSettings.PRESS_ALLOWEXTRA
    ).apply(HOTKEYS_KEY);
    public static final ConfigHotkey OPEN_FOCUS_SWITCHER = new ConfigHotkey(
            "openFocusSwitcher", "M,F", KeybindSettings.PRESS_ALLOWEXTRA
    ).apply(HOTKEYS_KEY);
    public static final ConfigHotkey NEW_BLANK = new ConfigHotkey(
            "newBlank", "M,N", KeybindSettings.PRESS_ALLOWEXTRA
    ).apply(HOTKEYS_KEY);

    public static final List<ConfigHotkey> HOTKEY_LIST = ImmutableList.of(
            TOGGLE_CREATOR_MODE,
            OPEN_CREATOR_INVENTORY,
            OPEN_CONFIG_GUI,
            SAVE_DRAFT,
            DISCARD_DRAFT,
            OPEN_FOCUS_SWITCHER,
            NEW_BLANK
    );
}
