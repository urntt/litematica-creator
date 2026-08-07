package io.github.urntt.litematicacreator.render;

import net.minecraft.world.entity.Pose;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatorCameraAvatarRenderPolicyTest
{
    @Test
    void fallFlyingPoseKeepsTheAvatarInTheFlightRenderPath()
    {
        assertTrue(CreatorCameraAvatarRenderPolicy.renderAsFallFlying(true, Pose.STANDING));
        assertTrue(CreatorCameraAvatarRenderPolicy.renderAsFallFlying(false, Pose.FALL_FLYING));
        assertFalse(CreatorCameraAvatarRenderPolicy.renderAsFallFlying(false, Pose.STANDING));
    }

    @Test
    void virtualGliderDoesNotReuseThePlayerCapeTexture()
    {
        assertFalse(CreatorCameraAvatarRenderPolicy.showCapeWithVirtualChest(true, true));
        assertTrue(CreatorCameraAvatarRenderPolicy.showCapeWithVirtualChest(true, false));
        assertFalse(CreatorCameraAvatarRenderPolicy.showCapeWithVirtualChest(false, false));
    }
}
