package io.github.urntt.litematicacreator.config;

import java.util.List;

import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.i18n.i18nConfig;
import fi.dy.masa.malilib.util.i18n.i18nManager;
import fi.dy.masa.malilib.util.i18n.i18nOption;

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

    @Override
    public CreatorI18nConfig cycle(boolean forward)
    {
        List<i18nOption> options = this.getManager().getLanguageOptions();

        String language = CreatorTranslationPolicy.cycleLanguage(
                this.getStringValue(),
                forward,
                options.stream().map(i18nOption::getKey).toList()
        );
        return language.equalsIgnoreCase(this.getStringValue()) ? this : this.fromString(language);
    }

    @Override
    public CreatorI18nConfig fromString(String value)
    {
        this.getManager().setLang(value);
        return new CreatorI18nConfig(this.getManager());
    }
}
