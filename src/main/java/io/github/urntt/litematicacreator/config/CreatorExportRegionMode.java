package io.github.urntt.litematicacreator.config;

import java.util.Locale;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum CreatorExportRegionMode implements IConfigOptionListEntry
{
    RAW("raw"),
    SPARSE_COMPACT("sparse_compact"),
    ENCLOSING_PROJECTION("enclosing_projection"),
    ENCLOSING_WITH_WORLD("enclosing_with_world");

    private static final CreatorExportRegionMode[] VALUES = values();
    private final String value;

    CreatorExportRegionMode(String value)
    {
        this.value = value;
    }

    @Override
    public String getStringValue()
    {
        return this.value;
    }

    @Override
    public String getDisplayName()
    {
        return StringUtils.translate("litematica-creator.export_mode." + this.value);
    }

    @Override
    public CreatorExportRegionMode cycle(boolean forward)
    {
        int offset = forward ? 1 : -1;
        return VALUES[Math.floorMod(this.ordinal() + offset, VALUES.length)];
    }

    @Override
    public CreatorExportRegionMode fromString(String value)
    {
        if (value != null)
        {
            String normalized = value.toLowerCase(Locale.ROOT);

            for (CreatorExportRegionMode mode : VALUES)
            {
                if (mode.value.equals(normalized))
                {
                    return mode;
                }
            }
        }

        return SPARSE_COMPACT;
    }
}
