package io.github.urntt.litematicacreator.creator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatorCameraCompatContractTest
{
    @BeforeEach
    void resetFakes()
    {
        FakeFeatureToggle.TWEAK_FREE_CAMERA.setBooleanValue(true);
        FakeGenericConfigs.FREE_CAMERA_PLAYER_INPUTS.setBooleanValue(true);
        FakeGenericConfigs.FREE_CAMERA_PLAYER_MOVEMENT.setBooleanValue(false);
        FakeCameraEntity.originalCameraEntity = null;
        FakeCameraEntity.originalCameraWasPlayer = false;
    }

    @Test
    void bridgeSnapshotsEnforcesRestoresAndRebases() throws ReflectiveOperationException
    {
        CreatorCameraCompat.TweakerooBridge bridge = CreatorCameraCompat.createBridge(
                FakeFeatureToggle.class,
                FakeGenericConfigs.class,
                FakeCameraEntity.class
        );

        assertTrue(bridge.isFreeCameraActive());
        CreatorCameraCompat.TweakerooConfigSnapshot snapshot = bridge.captureConfig();
        bridge.enforceCreatorState();

        assertFalse(FakeGenericConfigs.FREE_CAMERA_PLAYER_INPUTS.getBooleanValue());
        assertTrue(FakeGenericConfigs.FREE_CAMERA_PLAYER_MOVEMENT.getBooleanValue());

        bridge.restoreConfig(snapshot);
        assertTrue(FakeGenericConfigs.FREE_CAMERA_PLAYER_INPUTS.getBooleanValue());
        assertFalse(FakeGenericConfigs.FREE_CAMERA_PLAYER_MOVEMENT.getBooleanValue());

        Object player = new Object();
        bridge.rebaseOriginalCamera(player);
        assertSame(player, FakeCameraEntity.originalCameraEntity);
        assertTrue(FakeCameraEntity.originalCameraWasPlayer);
    }

    @Test
    void missingContractMemberRejectsTheBridge()
    {
        assertThrows(
                NoSuchFieldException.class,
                () -> CreatorCameraCompat.createBridge(
                        FakeFeatureToggle.class,
                        IncompleteGenericConfigs.class,
                        FakeCameraEntity.class
                )
        );
    }

    public enum FakeFeatureToggle
    {
        TWEAK_FREE_CAMERA;

        private boolean value;

        public boolean getBooleanValue()
        {
            return this.value;
        }

        public void setBooleanValue(boolean value)
        {
            this.value = value;
        }
    }

    public static final class FakeBooleanConfig
    {
        private boolean value;

        public boolean getBooleanValue()
        {
            return this.value;
        }

        public void setBooleanValue(boolean value)
        {
            this.value = value;
        }
    }

    public static final class FakeGenericConfigs
    {
        public static final FakeBooleanConfig FREE_CAMERA_PLAYER_INPUTS = new FakeBooleanConfig();
        public static final FakeMovementConfig FREE_CAMERA_PLAYER_MOVEMENT = new FakeMovementConfig();

        private FakeGenericConfigs()
        {
        }
    }

    public static final class FakeMovementConfig
    {
        private boolean value;

        public boolean getBooleanValue()
        {
            return this.value;
        }

        public void setBooleanValue(boolean value)
        {
            this.value = value;
        }
    }

    public static final class IncompleteGenericConfigs
    {
        public static final FakeBooleanConfig FREE_CAMERA_PLAYER_INPUTS = new FakeBooleanConfig();

        private IncompleteGenericConfigs()
        {
        }
    }

    public static final class FakeCameraEntity
    {
        private static Object originalCameraEntity;
        private static boolean originalCameraWasPlayer;

        private FakeCameraEntity()
        {
        }
    }
}
