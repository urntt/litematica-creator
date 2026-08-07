package io.github.urntt.litematicacreator.render;

import net.minecraft.world.entity.Pose;

public final class CreatorCameraAvatarRenderPolicy
{
    private CreatorCameraAvatarRenderPolicy()
    {
    }

    public static boolean renderAsFallFlying(boolean fallFlying, Pose pose)
    {
        return fallFlying || pose == Pose.FALL_FLYING;
    }

    public static boolean showCapeWithVirtualChest(boolean originalShowCape, boolean virtualChestHasGlider)
    {
        return originalShowCape && !virtualChestHasGlider;
    }
}
