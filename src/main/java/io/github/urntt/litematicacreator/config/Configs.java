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
import fi.dy.masa.malilib.config.options.ConfigDouble;
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
        public static final ConfigBoolean ENABLE_AIR_PLACEMENT = new ConfigBoolean(
                "enableAirPlacement", CreatorConfigDefaults.ENABLE_AIR_PLACEMENT
        ).apply(GENERIC_KEY);
        public static final ConfigInteger AIR_PLACEMENT_DISTANCE = new ConfigInteger(
                "airPlacementDistance",
                CreatorConfigDefaults.AIR_PLACEMENT_DISTANCE,
                CreatorConfigDefaults.AIR_PLACEMENT_DISTANCE_MIN,
                CreatorConfigDefaults.AIR_PLACEMENT_DISTANCE_MAX
        ).apply(GENERIC_KEY);
        public static final ConfigInteger CONTINUOUS_PLACE_INTERVAL_TICKS = new ConfigInteger(
                "continuousPlaceIntervalTicks",
                CreatorConfigDefaults.CONTINUOUS_PLACE_INTERVAL_TICKS,
                CreatorConfigDefaults.CONTINUOUS_PLACE_INTERVAL_TICKS_MIN,
                CreatorConfigDefaults.CONTINUOUS_PLACE_INTERVAL_TICKS_MAX
        ).apply(GENERIC_KEY);
        public static final ConfigInteger CONTINUOUS_BREAK_INTERVAL_TICKS = new ConfigInteger(
                "continuousBreakIntervalTicks",
                CreatorConfigDefaults.CONTINUOUS_BREAK_INTERVAL_TICKS,
                CreatorConfigDefaults.CONTINUOUS_BREAK_INTERVAL_TICKS_MIN,
                CreatorConfigDefaults.CONTINUOUS_BREAK_INTERVAL_TICKS_MAX
        ).apply(GENERIC_KEY);
        public static final ConfigDouble CREATOR_CAMERA_GROUND_SPEED_MULTIPLIER = new ConfigDouble(
                "creatorCameraGroundSpeedMultiplier",
                CreatorConfigDefaults.CREATOR_CAMERA_GROUND_SPEED_MULTIPLIER,
                CreatorConfigDefaults.CREATOR_CAMERA_SPEED_MULTIPLIER_MIN,
                CreatorConfigDefaults.CREATOR_CAMERA_SPEED_MULTIPLIER_MAX,
                true
        ).apply(GENERIC_KEY);
        public static final ConfigDouble CREATOR_CAMERA_FLIGHT_SPEED_MULTIPLIER = new ConfigDouble(
                "creatorCameraFlightSpeedMultiplier",
                CreatorConfigDefaults.CREATOR_CAMERA_FLIGHT_SPEED_MULTIPLIER,
                CreatorConfigDefaults.CREATOR_CAMERA_SPEED_MULTIPLIER_MIN,
                CreatorConfigDefaults.CREATOR_CAMERA_SPEED_MULTIPLIER_MAX,
                true
        ).apply(GENERIC_KEY);
        public static final ConfigBoolean ENABLE_CREATOR_CAMERA_WITH_CREATOR_MODE = new ConfigBoolean(
                "enableCreatorCameraWithCreatorMode",
                CreatorConfigDefaults.ENABLE_CREATOR_CAMERA_WITH_CREATOR_MODE
        ).apply(GENERIC_KEY);
        public static final ConfigBoolean DISABLE_CREATOR_CAMERA_WITH_CREATOR_MODE = new ConfigBoolean(
                "disableCreatorCameraWithCreatorMode",
                CreatorConfigDefaults.DISABLE_CREATOR_CAMERA_WITH_CREATOR_MODE
        ).apply(GENERIC_KEY);
        public static final ConfigBoolean CREATOR_CAMERA_PROJECTION_COLLISION = new ConfigBoolean(
                "creatorCameraProjectionCollision",
                CreatorConfigDefaults.CREATOR_CAMERA_PROJECTION_COLLISION
        ).apply(GENERIC_KEY);
        public static final ConfigBoolean IGNORE_CREATOR_CAMERA_ENTITY_PLACEMENT_COLLISION = new ConfigBoolean(
                "ignoreCreatorCameraEntityPlacementCollision",
                CreatorConfigDefaults.IGNORE_CREATOR_CAMERA_ENTITY_PLACEMENT_COLLISION
        ).apply(GENERIC_KEY);
        public static final ConfigOptionList CREATOR_EXPORT_REGION_MODE = new ConfigOptionList(
                "creatorExportRegionMode", CreatorConfigDefaults.CREATOR_EXPORT_REGION_MODE
        ).apply(GENERIC_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                ENABLE_CREATOR_MODE,
                DEBUG_LOGGING,
                SELECT_NEW_DRAFT_PLACEMENT,
                HIDE_SUBREGION_BOXES_IN_CREATOR_MODE,
                OPEN_CREATOR_INVENTORY_WITH_INVENTORY_KEY,
                CREATOR_EDIT_RANGE,
                ENABLE_AIR_PLACEMENT,
                AIR_PLACEMENT_DISTANCE,
                CONTINUOUS_PLACE_INTERVAL_TICKS,
                CONTINUOUS_BREAK_INTERVAL_TICKS,
                ENABLE_CREATOR_CAMERA_WITH_CREATOR_MODE,
                DISABLE_CREATOR_CAMERA_WITH_CREATOR_MODE,
                CREATOR_CAMERA_PROJECTION_COLLISION,
                IGNORE_CREATOR_CAMERA_ENTITY_PLACEMENT_COLLISION,
                CREATOR_EXPORT_REGION_MODE,
                CREATOR_CAMERA_GROUND_SPEED_MULTIPLIER,
                CREATOR_CAMERA_FLIGHT_SPEED_MULTIPLIER
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
