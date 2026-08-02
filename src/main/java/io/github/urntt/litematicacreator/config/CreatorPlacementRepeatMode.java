package io.github.urntt.litematicacreator.config;

import net.minecraft.util.StringRepresentable;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum CreatorPlacementRepeatMode implements IConfigOptionListEntry, StringRepresentable
{
    FIXED("fixed", "litematica-creator.label.placement_repeat_mode.fixed"),
    ACCURATE("accurate", "litematica-creator.label.placement_repeat_mode.accurate");

    private final String configValue;
    private final String translationKey;

    CreatorPlacementRepeatMode(String configValue, String translationKey)
    {
        this.configValue = configValue;
        this.translationKey = translationKey;
    }

    @Override
    public String getStringValue()
    {
        return this.configValue;
    }

    @Override
    public String getDisplayName()
    {
        return StringUtils.translate(this.translationKey);
    }

    @Override
    public IConfigOptionListEntry cycle(boolean forward)
    {
        int offset = forward ? 1 : values().length - 1;
        return values()[(this.ordinal() + offset) % values().length];
    }

    @Override
    public CreatorPlacementRepeatMode fromString(String value)
    {
        for (CreatorPlacementRepeatMode mode : values())
        {
            if (mode.configValue.equalsIgnoreCase(value) || mode.name().equalsIgnoreCase(value))
            {
                return mode;
            }
        }

        return FIXED;
    }

    @Override
    public String getSerializedName()
    {
        return this.configValue;
    }
}
