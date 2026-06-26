package io.github.urntt.litematicacreator.data;

import io.github.urntt.litematicacreator.gui.GuiConfigs.ConfigGuiTab;

public class DataManager
{
    private static ConfigGuiTab configGuiTab = ConfigGuiTab.GENERIC;

    public static ConfigGuiTab getConfigGuiTab()
    {
        return configGuiTab;
    }

    public static void setConfigGuiTab(ConfigGuiTab tab)
    {
        configGuiTab = tab;
    }
}
