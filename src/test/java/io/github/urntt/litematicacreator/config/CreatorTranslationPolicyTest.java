package io.github.urntt.litematicacreator.config;

import java.util.List;
import org.junit.jupiter.api.Test;

import fi.dy.masa.malilib.util.i18n.i18nMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatorTranslationPolicyTest
{
    @Test
    void modeSelectsTheExpectedLanguageSource()
    {
        assertEquals("configured", CreatorTranslationPolicy.requestedLanguage(
                i18nMode.INDEPENDENT, "configured", "vanilla", "malilib").orElseThrow());
        assertEquals("vanilla", CreatorTranslationPolicy.requestedLanguage(
                i18nMode.FOLLOW_VANILLA, "configured", "vanilla", "malilib").orElseThrow());
        assertEquals("malilib", CreatorTranslationPolicy.requestedLanguage(
                i18nMode.FOLLOW_MALILIB, "configured", "vanilla", "malilib").orElseThrow());
        assertTrue(CreatorTranslationPolicy.requestedLanguage(
                i18nMode.OFF, "configured", "vanilla", "malilib").isEmpty());
    }

    @Test
    void unavailableLanguageFallsBackToDefault()
    {
        assertEquals("zh_cn", CreatorTranslationPolicy.resolveAvailableLanguage(
                "ZH_CN", "en_us", List.of("en_us", "zh_cn")));
        assertEquals("en_us", CreatorTranslationPolicy.resolveAvailableLanguage(
                "missing", "en_us", List.of("en_us", "zh_cn")));
    }

    @Test
    void languageCyclingWrapsInBothDirections()
    {
        List<String> languages = List.of("en_us", "zh_cn", "ja_jp");

        assertEquals("zh_cn", CreatorTranslationPolicy.cycleLanguage("en_us", true, languages));
        assertEquals("en_us", CreatorTranslationPolicy.cycleLanguage("ja_jp", true, languages));
        assertEquals("ja_jp", CreatorTranslationPolicy.cycleLanguage("en_us", false, languages));
        assertEquals("en_us", CreatorTranslationPolicy.cycleLanguage("en_us", true, List.of("en_us")));
    }
}
