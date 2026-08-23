package io.github.urntt.litematicacreator.creator;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.annotation.Nullable;

import net.minecraft.world.entity.Entity;

import fi.dy.masa.malilib.util.EntityUtils;
import io.github.urntt.litematicacreator.LitematicaCreator;
import io.github.urntt.litematicacreator.compat.CreatorOptionalCompatibility;

public final class CreatorCameraCompat
{
    private static final String FEATURE_TOGGLE_CLASS = "fi.dy.masa.tweakeroo.config.FeatureToggle";
    private static final String GENERIC_CONFIGS_CLASS = "fi.dy.masa.tweakeroo.config.Configs$Generic";
    private static final String CAMERA_ENTITY_CLASS = "fi.dy.masa.tweakeroo.util.CameraEntity";
    private static final String PLAYER_INPUTS_FIELD = "FREE_CAMERA_PLAYER_INPUTS";
    private static final String PLAYER_MOVEMENT_FIELD = "FREE_CAMERA_PLAYER_MOVEMENT";

    private static final Object BRIDGE_LOCK = new Object();
    @Nullable private static volatile TweakerooBridge tweakerooBridge;
    private static volatile boolean tweakerooBridgeConfigured;
    private static boolean runtimeFailureLogged;

    private CreatorCameraCompat()
    {
    }

    @Nullable
    public static Entity getCameraEntity()
    {
        return EntityUtils.getCameraEntity();
    }

    public static boolean initializeTweakerooBridge(boolean enabled, @Nullable String version)
    {
        synchronized (BRIDGE_LOCK)
        {
            tweakerooBridgeConfigured = true;
            runtimeFailureLogged = false;

            if (!enabled)
            {
                tweakerooBridge = null;
                return false;
            }

            try
            {
                tweakerooBridge = createBridge(
                        Class.forName(FEATURE_TOGGLE_CLASS),
                        Class.forName(GENERIC_CONFIGS_CLASS),
                        Class.forName(CAMERA_ENTITY_CLASS)
                );
                return true;
            }
            catch (ReflectiveOperationException | LinkageError | RuntimeException error)
            {
                tweakerooBridge = null;
                LitematicaCreator.LOGGER.warn(
                        "Tweakeroo {} does not match the audited Free Camera reflection contract",
                        version != null ? version : "<unknown>",
                        error
                );
                return false;
            }
        }
    }

    public static boolean isTweakerooFreeCameraActive()
    {
        TweakerooBridge bridge = getBridge();

        if (bridge == null)
        {
            return false;
        }

        try
        {
            return bridge.isFreeCameraActive();
        }
        catch (ReflectiveOperationException | LinkageError | RuntimeException error)
        {
            disableBridgeAfterRuntimeFailure("read Free Camera state", error);
            return false;
        }
    }

    @Nullable
    public static TweakerooConfigSnapshot captureAndSuspendTweakeroo()
    {
        TweakerooBridge bridge = getBridge();

        if (bridge == null)
        {
            return null;
        }

        try
        {
            if (!bridge.isFreeCameraActive())
            {
                return null;
            }

            TweakerooConfigSnapshot snapshot = bridge.captureConfig();

            try
            {
                bridge.enforceCreatorState();
                return snapshot;
            }
            catch (ReflectiveOperationException | RuntimeException error)
            {
                bridge.restoreConfigBestEffort(snapshot);
                throw error;
            }
        }
        catch (ReflectiveOperationException | LinkageError | RuntimeException error)
        {
            disableBridgeAfterRuntimeFailure("snapshot Free Camera settings", error);
            return null;
        }
    }

    public static void enforceTweakerooCreatorState()
    {
        TweakerooBridge bridge = getBridge();

        if (bridge == null)
        {
            return;
        }

        try
        {
            if (bridge.isFreeCameraActive())
            {
                bridge.enforceCreatorState();
            }
        }
        catch (ReflectiveOperationException | LinkageError | RuntimeException error)
        {
            disableBridgeAfterRuntimeFailure("enforce Free Camera settings", error);
        }
    }

    public static void restoreTweakeroo(@Nullable TweakerooConfigSnapshot snapshot)
    {
        TweakerooBridge bridge = getBridge();

        if (bridge == null || snapshot == null)
        {
            return;
        }

        try
        {
            bridge.restoreConfig(snapshot);
        }
        catch (ReflectiveOperationException | LinkageError | RuntimeException error)
        {
            disableBridgeAfterRuntimeFailure("restore Free Camera settings", error);
        }
    }

    public static boolean rebaseTweakerooOriginalCamera(Entity player)
    {
        TweakerooBridge bridge = getBridge();

        if (bridge == null)
        {
            return false;
        }

        try
        {
            bridge.rebaseOriginalCamera(player);
            return true;
        }
        catch (ReflectiveOperationException | LinkageError | RuntimeException error)
        {
            disableBridgeAfterRuntimeFailure("rebase the original Free Camera entity", error);
            return false;
        }
    }

    @Nullable
    private static TweakerooBridge getBridge()
    {
        if (!tweakerooBridgeConfigured)
        {
            CreatorOptionalCompatibility.ensureInitialized();
        }

        return tweakerooBridge;
    }

    private static void disableBridgeAfterRuntimeFailure(String operation, Throwable error)
    {
        synchronized (BRIDGE_LOCK)
        {
            tweakerooBridge = null;

            if (!runtimeFailureLogged)
            {
                runtimeFailureLogged = true;
                LitematicaCreator.LOGGER.warn(
                        "Failed to {}; the Tweakeroo Creator Camera bridge is disabled for this session",
                        operation,
                        error
                );
            }
        }
    }

    static TweakerooBridge createBridge(
            Class<?> featureToggleClass,
            Class<?> genericConfigsClass,
            Class<?> cameraEntityClass) throws ReflectiveOperationException
    {
        Object freeCameraToggle = featureToggleClass.getField("TWEAK_FREE_CAMERA").get(null);
        Object playerInputs = genericConfigsClass.getField(PLAYER_INPUTS_FIELD).get(null);
        Object playerMovement = genericConfigsClass.getField(PLAYER_MOVEMENT_FIELD).get(null);
        Method toggleGetter = freeCameraToggle.getClass().getMethod("getBooleanValue");
        Method playerInputsGetter = playerInputs.getClass().getMethod("getBooleanValue");
        Method playerInputsSetter = playerInputs.getClass().getMethod("setBooleanValue", boolean.class);
        Method playerMovementGetter = playerMovement.getClass().getMethod("getBooleanValue");
        Method playerMovementSetter = playerMovement.getClass().getMethod("setBooleanValue", boolean.class);

        Field originalCameraEntity = cameraEntityClass.getDeclaredField("originalCameraEntity");
        Field originalCameraWasPlayer = cameraEntityClass.getDeclaredField("originalCameraWasPlayer");
        originalCameraEntity.setAccessible(true);
        originalCameraWasPlayer.setAccessible(true);

        return new TweakerooBridge(
                freeCameraToggle,
                playerInputs,
                playerMovement,
                toggleGetter,
                playerInputsGetter,
                playerInputsSetter,
                playerMovementGetter,
                playerMovementSetter,
                originalCameraEntity,
                originalCameraWasPlayer
        );
    }

    static final class TweakerooBridge
    {
        private final Object freeCameraToggle;
        private final Object playerInputs;
        private final Object playerMovement;
        private final Method toggleGetter;
        private final Method playerInputsGetter;
        private final Method playerInputsSetter;
        private final Method playerMovementGetter;
        private final Method playerMovementSetter;
        private final Field originalCameraEntity;
        private final Field originalCameraWasPlayer;

        private TweakerooBridge(
                Object freeCameraToggle,
                Object playerInputs,
                Object playerMovement,
                Method toggleGetter,
                Method playerInputsGetter,
                Method playerInputsSetter,
                Method playerMovementGetter,
                Method playerMovementSetter,
                Field originalCameraEntity,
                Field originalCameraWasPlayer)
        {
            this.freeCameraToggle = freeCameraToggle;
            this.playerInputs = playerInputs;
            this.playerMovement = playerMovement;
            this.toggleGetter = toggleGetter;
            this.playerInputsGetter = playerInputsGetter;
            this.playerInputsSetter = playerInputsSetter;
            this.playerMovementGetter = playerMovementGetter;
            this.playerMovementSetter = playerMovementSetter;
            this.originalCameraEntity = originalCameraEntity;
            this.originalCameraWasPlayer = originalCameraWasPlayer;
        }

        boolean isFreeCameraActive() throws ReflectiveOperationException
        {
            return getBoolean(this.toggleGetter.invoke(this.freeCameraToggle));
        }

        TweakerooConfigSnapshot captureConfig() throws ReflectiveOperationException
        {
            return new TweakerooConfigSnapshot(
                    this.getConfig(this.playerInputs, this.playerInputsGetter),
                    this.getConfig(this.playerMovement, this.playerMovementGetter)
            );
        }

        void enforceCreatorState() throws ReflectiveOperationException
        {
            this.setConfig(this.playerInputs, this.playerInputsGetter, this.playerInputsSetter, false);
            this.setConfig(this.playerMovement, this.playerMovementGetter, this.playerMovementSetter, true);
        }

        void restoreConfig(TweakerooConfigSnapshot snapshot) throws ReflectiveOperationException
        {
            this.setConfig(
                    this.playerInputs,
                    this.playerInputsGetter,
                    this.playerInputsSetter,
                    snapshot.playerInputs()
            );
            this.setConfig(
                    this.playerMovement,
                    this.playerMovementGetter,
                    this.playerMovementSetter,
                    snapshot.playerMovement()
            );
        }

        void restoreConfigBestEffort(TweakerooConfigSnapshot snapshot)
        {
            try
            {
                this.restoreConfig(snapshot);
            }
            catch (ReflectiveOperationException | RuntimeException ignored)
            {
                // The original bridge failure is reported by the caller.
            }
        }

        void rebaseOriginalCamera(Object player) throws IllegalAccessException
        {
            this.originalCameraEntity.set(null, player);
            this.originalCameraWasPlayer.setBoolean(null, true);
        }

        private boolean getConfig(Object config, Method getter) throws ReflectiveOperationException
        {
            return getBoolean(getter.invoke(config));
        }

        private void setConfig(Object config, Method getter, Method setter, boolean value) throws ReflectiveOperationException
        {
            setter.invoke(config, value);

            if (this.getConfig(config, getter) != value)
            {
                throw new ReflectiveOperationException("Tweakeroo config setter did not retain the requested value");
            }
        }

        private static boolean getBoolean(Object value) throws ReflectiveOperationException
        {
            if (value instanceof Boolean booleanValue)
            {
                return booleanValue;
            }

            throw new ReflectiveOperationException("Expected a boolean Tweakeroo config value");
        }
    }

    public record TweakerooConfigSnapshot(boolean playerInputs, boolean playerMovement)
    {
    }
}
