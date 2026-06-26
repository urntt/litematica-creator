package io.github.urntt.litematicacreator.creator;

import javax.annotation.Nullable;

import net.minecraft.world.entity.Entity;

public class CreatorCameraCompat
{
    @Nullable
    public static Entity getCameraEntity()
    {
        return fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
    }
}
