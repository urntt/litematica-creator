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
}
