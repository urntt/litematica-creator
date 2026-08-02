package io.github.urntt.litematicacreator.creator;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import io.github.urntt.litematicacreator.config.Configs;

public final class CreatorTargeting
{
    private CreatorTargeting()
    {
    }

    @Nullable
    public static RayTraceWrapper trace(Minecraft mc)
    {
        Entity camera = CreatorCameraCompat.getCameraEntity();

        if (camera == null || mc.level == null)
        {
            return null;
        }

        return RayTraceUtils.getGenericTrace(
                mc.level,
                camera,
                Configs.Generic.CREATOR_EDIT_RANGE.getIntegerValue(),
                true,
                false,
                false
        );
    }
}
