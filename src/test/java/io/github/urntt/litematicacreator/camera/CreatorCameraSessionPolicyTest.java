package io.github.urntt.litematicacreator.camera;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatorCameraSessionPolicyTest
{
    @Test
    void playerCameraStartsOnTheGround()
    {
        assertFalse(CreatorCameraSessionPolicy.startsFlying(true));
    }

    @Test
    void detachedCameraStartsFlying()
    {
        assertTrue(CreatorCameraSessionPolicy.startsFlying(false));
    }

    @Test
    void deathOrPlayerReplacementEndsTheSession()
    {
        assertFalse(CreatorCameraSessionPolicy.mustStopForPlayer(true, true));
        assertTrue(CreatorCameraSessionPolicy.mustStopForPlayer(true, false));
        assertTrue(CreatorCameraSessionPolicy.mustStopForPlayer(false, true));
        assertTrue(CreatorCameraSessionPolicy.mustStopForPlayer(false, false));
    }

    @Test
    void creatorModeCameraLinkHonorsBothSettings()
    {
        assertTrue(CreatorCameraSessionPolicy.shouldActivateForCreatorMode(true, true, false));
        assertFalse(CreatorCameraSessionPolicy.shouldActivateForCreatorMode(true, false, false));
        assertFalse(CreatorCameraSessionPolicy.shouldActivateForCreatorMode(true, true, true));
        assertTrue(CreatorCameraSessionPolicy.shouldDeactivateForCreatorMode(false, true, true));
        assertFalse(CreatorCameraSessionPolicy.shouldDeactivateForCreatorMode(false, false, true));
        assertFalse(CreatorCameraSessionPolicy.shouldDeactivateForCreatorMode(true, true, true));
    }
}
