package io.github.urntt.litematicacreator.camera;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatorCameraFlightPolicyTest
{
    @Test
    void startsLocalGlidingOnlyOnAnAirborneFreshJump()
    {
        assertTrue(CreatorCameraFlightPolicy.shouldStartFallFlying(
                true, false, false, false, false, true, false
        ));
        assertFalse(CreatorCameraFlightPolicy.shouldStartFallFlying(
                true, true, false, false, false, true, false
        ));
        assertFalse(CreatorCameraFlightPolicy.shouldStartFallFlying(
                true, false, true, false, false, true, false
        ));
    }

    @Test
    void creativeFlightWaterAndMissingWingsBlockGliding()
    {
        assertFalse(CreatorCameraFlightPolicy.shouldStartFallFlying(
                true, false, false, true, false, true, false
        ));
        assertFalse(CreatorCameraFlightPolicy.shouldStartFallFlying(
                true, false, false, false, false, false, false
        ));
        assertFalse(CreatorCameraFlightPolicy.shouldStartFallFlying(
                true, false, false, false, false, true, true
        ));
    }

    @Test
    void stopsLocalGlidingWhenCreativeFlightStartsOrWingsDisappear()
    {
        assertTrue(CreatorCameraFlightPolicy.shouldStopFallFlying(true, true, true));
        assertTrue(CreatorCameraFlightPolicy.shouldStopFallFlying(false, true, false));
        assertFalse(CreatorCameraFlightPolicy.shouldStopFallFlying(false, true, true));
        assertFalse(CreatorCameraFlightPolicy.shouldStopFallFlying(true, false, false));
    }

    @Test
    void fallFlyingAnimationTicksAdvanceAndResetLikeVanilla()
    {
        assertEquals(1, CreatorCameraFlightPolicy.nextFallFlyingTicks(true, 0));
        assertEquals(12, CreatorCameraFlightPolicy.nextFallFlyingTicks(true, 11));
        assertEquals(0, CreatorCameraFlightPolicy.nextFallFlyingTicks(false, 11));
    }
}
