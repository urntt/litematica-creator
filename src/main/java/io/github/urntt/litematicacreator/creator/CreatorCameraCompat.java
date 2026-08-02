package io.github.urntt.litematicacreator.creator;

import java.lang.reflect.Field;
import javax.annotation.Nullable;

import net.minecraft.world.entity.Entity;

import fi.dy.masa.malilib.util.EntityUtils;
import io.github.urntt.litematicacreator.LitematicaCreator;

public final class CreatorCameraCompat
{
    private static final String PLAYER_INPUTS_FIELD = "FREE_CAMERA_PLAYER_INPUTS";
    private static final String PLAYER_MOVEMENT_FIELD = "FREE_CAMERA_PLAYER_MOVEMENT";

    private CreatorCameraCompat()
    {
    }

    @Nullable
    public static Entity getCameraEntity()
    {
        return EntityUtils.getCameraEntity();
    }

    public static boolean isTweakerooFreeCameraActive()
    {
        try
        {
            Class<?> featureToggleClass = Class.forName("fi.dy.masa.tweakeroo.config.FeatureToggle");
            Object freeCameraToggle = getEnumConstant(featureToggleClass, "TWEAK_FREE_CAMERA");
            return getBooleanValue(freeCameraToggle);
        }
        catch (ReflectiveOperationException | LinkageError | RuntimeException ignored)
        {
            return false;
        }
    }

    @Nullable
    public static TweakerooConfigSnapshot captureAndSuspendTweakeroo()
    {
        if (!isTweakerooFreeCameraActive())
        {
            return null;
        }

        try
        {
            boolean playerInputs = getBooleanValue(getTweakerooConfig(PLAYER_INPUTS_FIELD));
            boolean playerMovement = getBooleanValue(getTweakerooConfig(PLAYER_MOVEMENT_FIELD));
            TweakerooConfigSnapshot snapshot = new TweakerooConfigSnapshot(playerInputs, playerMovement);
            enforceTweakerooCreatorState();
            return snapshot;
        }
        catch (ReflectiveOperationException | LinkageError | RuntimeException error)
        {
            LitematicaCreator.LOGGER.warn("Failed to snapshot Tweakeroo Free Camera settings", error);
            return null;
        }
    }

    public static void enforceTweakerooCreatorState()
    {
        if (!isTweakerooFreeCameraActive())
        {
            return;
        }

        setTweakerooConfig(PLAYER_INPUTS_FIELD, false);
        setTweakerooConfig(PLAYER_MOVEMENT_FIELD, true);
    }

    public static void restoreTweakeroo(@Nullable TweakerooConfigSnapshot snapshot)
    {
        if (snapshot != null)
        {
            setTweakerooConfig(PLAYER_INPUTS_FIELD, snapshot.playerInputs());
            setTweakerooConfig(PLAYER_MOVEMENT_FIELD, snapshot.playerMovement());
        }
    }

    public static boolean rebaseTweakerooOriginalCamera(Entity player)
    {
        try
        {
            Class<?> cameraClass = Class.forName("fi.dy.masa.tweakeroo.util.CameraEntity");
            Field originalCameraEntity = cameraClass.getDeclaredField("originalCameraEntity");
            Field originalCameraWasPlayer = cameraClass.getDeclaredField("originalCameraWasPlayer");
            originalCameraEntity.setAccessible(true);
            originalCameraWasPlayer.setAccessible(true);
            originalCameraEntity.set(null, player);
            originalCameraWasPlayer.setBoolean(null, true);
            return true;
        }
        catch (ReflectiveOperationException | LinkageError | RuntimeException error)
        {
            LitematicaCreator.LOGGER.warn("Failed to rebase Tweakeroo Free Camera after Creator Camera ownership", error);
            return false;
        }
    }

    private static boolean setTweakerooConfig(String fieldName, boolean value)
    {
        try
        {
            Object config = getTweakerooConfig(fieldName);
            config.getClass().getMethod("setBooleanValue", boolean.class).invoke(config, value);
            return getBooleanValue(config) == value;
        }
        catch (ReflectiveOperationException | LinkageError | RuntimeException error)
        {
            LitematicaCreator.debugLog("Unable to set Tweakeroo config '{}' to {}: {}", fieldName, value, error.getMessage());
            return false;
        }
    }

    private static Object getTweakerooConfig(String fieldName) throws ReflectiveOperationException
    {
        Class<?> genericConfigsClass = Class.forName("fi.dy.masa.tweakeroo.config.Configs$Generic");
        return genericConfigsClass.getField(fieldName).get(null);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Object getEnumConstant(Class<?> enumClass, String name)
    {
        return Enum.valueOf((Class<Enum>) enumClass.asSubclass(Enum.class), name);
    }

    private static boolean getBooleanValue(Object config) throws ReflectiveOperationException
    {
        Object value = config.getClass().getMethod("getBooleanValue").invoke(config);
        return value instanceof Boolean booleanValue && booleanValue;
    }

    public record TweakerooConfigSnapshot(boolean playerInputs, boolean playerMovement)
    {
    }
}
