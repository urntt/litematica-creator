package io.github.urntt.litematicacreator.config;

import java.nio.file.Files;
import java.nio.file.Path;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.data.json.JsonUtils;
import io.github.urntt.litematicacreator.LitematicaCreator;
import io.github.urntt.litematicacreator.Reference;

public class Configs implements IConfigHandler
{
    private static final String CONFIG_FILE_NAME = Reference.MOD_ID + ".json";

    private static final String GENERIC_KEY = Reference.MOD_ID + ".config.generic";

    public static class Generic
    {
        public static final ConfigBoolean ENABLE_CREATOR_MODE = new ConfigBoolean(
                "enableCreatorMode", false
        ).apply(GENERIC_KEY);
        public static final ConfigBoolean DEBUG_LOGGING = new ConfigBoolean(
                "debugLogging", false
        ).apply(GENERIC_KEY);
        public static final ConfigBoolean SELECT_NEW_DRAFT_PLACEMENT = new ConfigBoolean(
                "selectNewDraftPlacement", false
        ).apply(GENERIC_KEY);
        public static final ConfigBoolean HIDE_SUBREGION_BOXES_IN_CREATOR_MODE = new ConfigBoolean(
                "hideSubregionBoxesInCreatorMode", true
        ).apply(GENERIC_KEY);
        public static final ConfigBoolean OPEN_CREATOR_INVENTORY_WITH_INVENTORY_KEY = new ConfigBoolean(
                "openCreatorInventoryWithInventoryKey", CreatorConfigDefaults.OPEN_CREATOR_INVENTORY_WITH_INVENTORY_KEY
        ).apply(GENERIC_KEY);
        public static final ConfigInteger CREATOR_EDIT_RANGE = new ConfigInteger(
                "creatorEditRange",
                CreatorConfigDefaults.CREATOR_EDIT_RANGE,
                CreatorConfigDefaults.CREATOR_EDIT_RANGE_MIN,
                CreatorConfigDefaults.CREATOR_EDIT_RANGE_MAX
        ).apply(GENERIC_KEY);
        public static final ConfigOptionList PLACEMENT_REPEAT_MODE = new ConfigOptionList(
                "placementRepeatMode", CreatorConfigDefaults.PLACEMENT_REPEAT_MODE
        ).apply(GENERIC_KEY);
        public static final ConfigInteger PLACEMENT_REPEAT_INTERVAL_TICKS = new ConfigInteger(
                "placementRepeatIntervalTicks",
                CreatorConfigDefaults.PLACEMENT_REPEAT_INTERVAL_TICKS,
                CreatorConfigDefaults.PLACEMENT_REPEAT_INTERVAL_TICKS_MIN,
                CreatorConfigDefaults.PLACEMENT_REPEAT_INTERVAL_TICKS_MAX
        ).apply(GENERIC_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                ENABLE_CREATOR_MODE,
                DEBUG_LOGGING,
                SELECT_NEW_DRAFT_PLACEMENT,
                HIDE_SUBREGION_BOXES_IN_CREATOR_MODE,
                OPEN_CREATOR_INVENTORY_WITH_INVENTORY_KEY,
                CREATOR_EDIT_RANGE,
                PLACEMENT_REPEAT_MODE,
                PLACEMENT_REPEAT_INTERVAL_TICKS
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
}
