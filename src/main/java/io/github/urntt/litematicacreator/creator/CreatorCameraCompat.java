package io.github.urntt.litematicacreator.creator;

import javax.annotation.Nullable;

import net.minecraft.world.entity.Entity;

import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.EntityUtils;
import fi.dy.masa.malilib.util.InfoUtils;

public class CreatorCameraCompat
{
    private static long lastFreeCameraInputsWarning;

    @Nullable
    public static Entity getCameraEntity()
    {
        return EntityUtils.getCameraEntity();
    }

    public static void warnIfTweakerooFreeCameraPlayerInputsEnabled()
    {
        if (isTweakerooFreeCameraPlayerInputsEnabled())
        {
            long now = System.currentTimeMillis();

            if (now - lastFreeCameraInputsWarning > 5000L)
            {
                lastFreeCameraInputsWarning = now;
                InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica-creator.message.compat.tweakeroo_free_camera_player_inputs");
            }
        }
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

    public static boolean isTweakerooFreeCameraPlayerInputsEnabled()
    {
        try
        {
            Object freeCameraPlayerInputs = getTweakerooFreeCameraPlayerInputsConfig();
            return getBooleanValue(freeCameraPlayerInputs);
        }
        catch (ReflectiveOperationException | LinkageError | RuntimeException ignored)
        {
            return false;
        }
    }

    public static boolean setTweakerooFreeCameraPlayerInputs(boolean value)
    {
        try
        {
            Object freeCameraPlayerInputs = getTweakerooFreeCameraPlayerInputsConfig();
            freeCameraPlayerInputs.getClass().getMethod("setBooleanValue", boolean.class).invoke(freeCameraPlayerInputs, value);
            return getBooleanValue(freeCameraPlayerInputs) == value;
        }
        catch (ReflectiveOperationException | LinkageError | RuntimeException ignored)
        {
            return false;
        }
    }

    private static Object getTweakerooFreeCameraPlayerInputsConfig() throws ReflectiveOperationException
    {
        Class<?> genericConfigsClass = Class.forName("fi.dy.masa.tweakeroo.config.Configs$Generic");
        return genericConfigsClass.getField("FREE_CAMERA_PLAYER_INPUTS").get(null);
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
}
