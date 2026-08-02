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
            "toggleCreatorMode", CreatorConfigDefaults.TOGGLE_CREATOR_MODE, KeybindSettings.PRESS_ALLOWEXTRA
    ).apply(HOTKEYS_KEY);
    public static final ConfigHotkey TOGGLE_CREATOR_CAMERA = new ConfigHotkey(
            "toggleCreatorCamera", CreatorConfigDefaults.TOGGLE_CREATOR_CAMERA, KeybindSettings.PRESS_ALLOWEXTRA
    ).apply(HOTKEYS_KEY);
    public static final ConfigHotkey OPEN_CREATOR_INVENTORY = new ConfigHotkey(
            "openCreatorInventory", CreatorConfigDefaults.OPEN_CREATOR_INVENTORY, KeybindSettings.PRESS_ALLOWEXTRA
    ).apply(HOTKEYS_KEY);
    public static final ConfigHotkey OPEN_CONFIG_GUI = new ConfigHotkey(
            "openConfigGui", CreatorConfigDefaults.OPEN_CONFIG_GUI, KeybindSettings.PRESS_ALLOWEXTRA
    ).apply(HOTKEYS_KEY);
    public static final ConfigHotkey SAVE_DRAFT = new ConfigHotkey(
            "saveDraft", CreatorConfigDefaults.SAVE_DRAFT, KeybindSettings.PRESS_ALLOWEXTRA
    ).apply(HOTKEYS_KEY);
    public static final ConfigHotkey DISCARD_DRAFT = new ConfigHotkey(
            "discardDraft", CreatorConfigDefaults.DISCARD_DRAFT, KeybindSettings.PRESS_ALLOWEXTRA
    ).apply(HOTKEYS_KEY);
    public static final ConfigHotkey OPEN_FOCUS_SWITCHER = new ConfigHotkey(
            "openFocusSwitcher", CreatorConfigDefaults.OPEN_FOCUS_SWITCHER, KeybindSettings.PRESS_ALLOWEXTRA
    ).apply(HOTKEYS_KEY);
    public static final ConfigHotkey NEW_BLANK = new ConfigHotkey(
            "newBlank", CreatorConfigDefaults.NEW_BLANK, KeybindSettings.PRESS_ALLOWEXTRA
    ).apply(HOTKEYS_KEY);

    public static final List<ConfigHotkey> HOTKEY_LIST = ImmutableList.of(
            TOGGLE_CREATOR_MODE,
            TOGGLE_CREATOR_CAMERA,
            OPEN_CREATOR_INVENTORY,
            OPEN_CONFIG_GUI,
            SAVE_DRAFT,
            DISCARD_DRAFT,
            OPEN_FOCUS_SWITCHER,
            NEW_BLANK
    );
}
