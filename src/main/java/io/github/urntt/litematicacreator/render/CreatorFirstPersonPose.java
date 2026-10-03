package io.github.urntt.litematicacreator.render;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;

import io.github.urntt.litematicacreator.camera.CreatorCameraController;
import io.github.urntt.litematicacreator.camera.CreatorCameraEntity;
import io.github.urntt.litematicacreator.creator.CreatorManager;

/**
 * Keeps first-person hands posed by the Creator Camera while the real player still supplies skin, light and the
 * virtual-loadout hand animation state ticked by {@code FirstPersonHandsAndItems}.
 */
public final class CreatorFirstPersonPose
{
    private CreatorFirstPersonPose()
    {
    }

    public static LocalPlayer handsPoseSource(LocalPlayer player)
    {
        CreatorCameraEntity camera = CreatorCameraController.getInstance().getCamera();
        return camera != null ? camera : player;
    }

    public static void apply(PlayerRenderState state, DeltaTracker deltaTracker)
    {
        AvatarRenderState avatarState = state.avatarRenderState;

        if (avatarState == null)
        {
            return;
        }

        if (CreatorManager.getInstance().isCreatorModeEnabled())
        {
            avatarState.isUsingItem = false;
        }

        CreatorCameraEntity camera = CreatorCameraController.getInstance().getCamera();

        if (camera != null)
        {
            float partialTicks = CreatorCameraRenderStates.partialTicks(camera, deltaTracker);
            avatarState.swingAnimation = camera.getSwingAnimation(partialTicks);
            avatarState.xRot = camera.getXRot(partialTicks);
        }
    }
}
