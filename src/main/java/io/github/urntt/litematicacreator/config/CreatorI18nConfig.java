package io.github.urntt.litematicacreator.config;

import java.util.List;

import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.i18n.i18nConfig;
import fi.dy.masa.malilib.util.i18n.i18nManager;

public class CreatorI18nConfig extends i18nConfig
{
    public CreatorI18nConfig(i18nManager manager)
    {
        super(manager);
    }

    @Override
    public List<String> getHoverText()
    {
        return List.of(StringUtils.translate("litematica-creator.gui.hover.translation_language"));
    }
}
