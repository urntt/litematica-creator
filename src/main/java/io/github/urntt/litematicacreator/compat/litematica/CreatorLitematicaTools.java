package io.github.urntt.litematicacreator.compat.litematica;

import net.minecraft.client.Minecraft;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.tool.ToolMode;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.util.input.KeyCodes;

/**
 * Reads Litematica's tool state for Creator input handling. Litematica sees the Creator virtual hands through
 * {@code LitematicaEntityUtilsMixin}, so these checks follow the virtual tool while Creator mode is on.
 */
public final class CreatorLitematicaTools
{
    private CreatorLitematicaTools()
    {
    }

    public static boolean claimsMouseButton(int button)
    {
        return claimsInput(button - KeyCodes.OFFSET_MOUSE);
    }

    public static boolean claimsKey(int keyCode)
    {
        return claimsInput(keyCode);
    }

    public static boolean claimsScroll()
    {
        return CreatorToolInputPolicy.claimsScroll(
                isToolEnabled(),
                holdsTool(),
                isHeld(Hotkeys.SELECTION_GRAB_MODIFIER) ||
                isHeld(Hotkeys.SELECTION_GROW_MODIFIER) ||
                isHeld(Hotkeys.SELECTION_NUDGE_MODIFIER) ||
                isHeld(Hotkeys.OPERATION_MODE_CHANGE_MODIFIER) ||
                isHeld(Hotkeys.SCHEMATIC_VERSION_CYCLE_MODIFIER)
        );
    }

    private static boolean claimsInput(int keyCode)
    {
        ToolMode mode = DataManager.getToolMode();
        boolean selectPressed = owns(Hotkeys.TOOL_SELECT_ELEMENTS, keyCode);
        boolean blockSelectPressed = selectPressed &&
                                     (mode.getUsesBlockPrimary() && isHeld(Hotkeys.TOOL_SELECT_MODIFIER_BLOCK_1) ||
                                      mode.getUsesBlockSecondary() && isHeld(Hotkeys.TOOL_SELECT_MODIFIER_BLOCK_2));

        return CreatorToolInputPolicy.claimsPress(
                isToolEnabled(),
                holdsTool(),
                owns(Hotkeys.TOOL_PLACE_CORNER_1, keyCode) || owns(Hotkeys.TOOL_PLACE_CORNER_2, keyCode) || selectPressed,
                blockSelectPressed
        );
    }

    private static boolean isToolEnabled()
    {
        return Configs.Visuals.ENABLE_RENDERING.getBooleanValue() && Configs.Generic.TOOL_ITEM_ENABLED.getBooleanValue();
    }

    private static boolean holdsTool()
    {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && EntityUtils.hasToolItem(mc.player);
    }

    private static boolean owns(ConfigHotkey hotkey, int keyCode)
    {
        return isHeld(hotkey) && hotkey.getKeybind().getKeys().contains(keyCode);
    }

    private static boolean isHeld(ConfigHotkey hotkey)
    {
        return hotkey.getKeybind().isKeybindHeld();
    }
}
