package io.github.urntt.litematicacreator.config;

import java.util.List;
import java.util.Optional;

import fi.dy.masa.malilib.util.i18n.i18nMode;

final class CreatorTranslationPolicy
{
    private CreatorTranslationPolicy()
    {
    }

    static Optional<String> requestedLanguage(
            i18nMode mode,
            String configuredLanguage,
            String vanillaLanguage,
            String malilibLanguage)
    {
        return switch (mode)
        {
            case INDEPENDENT -> Optional.of(configuredLanguage);
            case FOLLOW_VANILLA -> Optional.of(vanillaLanguage);
            case FOLLOW_MALILIB -> Optional.of(malilibLanguage);
            case OFF -> Optional.empty();
        };
    }

    static String resolveAvailableLanguage(String requestedLanguage, String defaultLanguage, List<String> availableLanguages)
    {
        for (String language : availableLanguages)
        {
            if (language.equalsIgnoreCase(requestedLanguage))
            {
                return language;
            }
        }

        return defaultLanguage;
    }

    static String cycleLanguage(String currentLanguage, boolean forward, List<String> availableLanguages)
    {
        if (availableLanguages.size() <= 1)
        {
            return currentLanguage;
        }

        int index = 0;

        for (int i = 0; i < availableLanguages.size(); ++i)
        {
            if (availableLanguages.get(i).equalsIgnoreCase(currentLanguage))
            {
                index = i;
                break;
            }
        }

        return availableLanguages.get(Math.floorMod(index + (forward ? 1 : -1), availableLanguages.size()));
    }
}
