package io.github.urntt.litematicacreator.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.data.json.JsonUtils;
import fi.dy.masa.malilib.util.i18n.i18nManager;
import fi.dy.masa.malilib.util.i18n.i18nMode;
import fi.dy.masa.malilib.util.i18n.i18nOption;
import io.github.urntt.litematicacreator.LitematicaCreator;
import io.github.urntt.litematicacreator.Reference;

public class Configs implements IConfigHandler
{
    private static final String CONFIG_FILE_NAME = Reference.MOD_ID + ".json";
    public static final Optional<i18nManager> LANG = Optional.ofNullable(i18nManager.create(Reference.MOD_ID));

    private static final String GENERIC_KEY = Reference.MOD_ID + ".config.generic";

    public static class Generic
    {
        public static final ConfigBoolean ENABLE_CREATOR_MODE = new ConfigBoolean(
                "enableCreatorMode", false
        ).apply(GENERIC_KEY);
        public static final ConfigBoolean DEBUG_LOGGING = new ConfigBoolean(
                "debugLogging", false
        ).apply(GENERIC_KEY);
        public static final ConfigOptionList TRANSLATION_LANGUAGE = new ConfigOptionList(
                "translationLanguage", new CreatorI18nConfig(LANG.orElseThrow())
        ).apply(GENERIC_KEY);
        public static final ConfigOptionList TRANSLATION_MODE = new ConfigOptionList(
                "translationMode", i18nMode.FOLLOW_VANILLA
        ).apply(GENERIC_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                ENABLE_CREATOR_MODE,
                DEBUG_LOGGING,
                TRANSLATION_LANGUAGE,
                TRANSLATION_MODE
        );
    }

    public static void loadFromFile()
    {
        Path configFile = FileUtils.getConfigDirectory().resolve(CONFIG_FILE_NAME);

        if (Files.exists(configFile) && Files.isReadable(configFile))
        {
            JsonElement element = JsonUtils.parseJsonFile(configFile);

            if (element != null && element.isJsonObject())
            {
                JsonObject root = element.getAsJsonObject();
                ConfigUtils.readConfigBase(root, "Generic", Generic.OPTIONS);
                ConfigUtils.readConfigBase(root, "Hotkeys", Hotkeys.HOTKEY_LIST);
                LitematicaCreator.debugLog("Loaded config file '{}'.", configFile.toAbsolutePath());
            }
            else
            {
                LitematicaCreator.LOGGER.error("Failed to load config file '{}'.", configFile.toAbsolutePath());
            }
        }

        checkBaseLanguage();
    }

    public static void saveToFile()
    {
        Path dir = FileUtils.getConfigDirectory();

        if (!Files.exists(dir))
        {
            FileUtils.createDirectoriesIfMissing(dir);
        }

        if (Files.isDirectory(dir))
        {
            JsonObject root = new JsonObject();
            ConfigUtils.writeConfigBase(root, "Generic", Generic.OPTIONS);
            ConfigUtils.writeConfigBase(root, "Hotkeys", Hotkeys.HOTKEY_LIST);
            JsonUtils.writeJsonToFile(root, dir.resolve(CONFIG_FILE_NAME));
        }
        else
        {
            LitematicaCreator.LOGGER.error("Config directory '{}' does not exist.", dir.toAbsolutePath());
        }
    }

    @Override
    public void load()
    {
        loadFromFile();
    }

    @Override
    public void save()
    {
        saveToFile();
    }

    @Override
    public void onLanguageChanged(String newLang)
    {
        checkBaseLanguage();
    }

    public static void checkBaseLanguage()
    {
        i18nMode mode = (i18nMode) Generic.TRANSLATION_MODE.getOptionListValue();

        if (mode == i18nMode.FOLLOW_MALILIB)
        {
            LANG.ifPresent(i18nManager -> setLanguageIfAvailable(i18nManager, Registry.TRANSLATION_OVERRIDE_MANAGER.getBaseLanguageCode()));
        }
        else if (mode == i18nMode.FOLLOW_VANILLA)
        {
            LANG.ifPresent(i18nManager -> setLanguageIfAvailable(i18nManager, Registry.TRANSLATION_OVERRIDE_MANAGER.getVanillaLanguageCode()));
        }
    }

    private static void setLanguageIfAvailable(i18nManager manager, String languageCode)
    {
        if (manager.getLang().getLangCode().equalsIgnoreCase(languageCode))
        {
            return;
        }

        List<i18nOption> list = manager.getLanguageOptions();

        for (i18nOption entry : list)
        {
            if (entry.getKey().equalsIgnoreCase(languageCode))
            {
                manager.setLang(languageCode);
                Generic.TRANSLATION_LANGUAGE.setOptionListValue(new CreatorI18nConfig(manager).fromString(languageCode));
                return;
            }
        }

        manager.resetLangToDefault();
        Generic.TRANSLATION_LANGUAGE.resetToDefault();
    }
}
