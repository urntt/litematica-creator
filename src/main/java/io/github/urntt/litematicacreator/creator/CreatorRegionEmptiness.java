package io.github.urntt.litematicacreator.creator;

import java.util.Collection;
import java.util.Map;
import javax.annotation.Nullable;

import net.minecraft.core.Vec3i;

import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;

final class CreatorRegionEmptiness
{
    private CreatorRegionEmptiness()
    {
    }

    static boolean isCompletelyEmpty(
            LitematicaBlockStateContainer container,
            @Nullable Map<?, ?> blockEntities,
            @Nullable Collection<?> entities,
            @Nullable Map<?, ?> pendingBlockTicks,
            @Nullable Map<?, ?> pendingFluidTicks)
    {
        return hasOnlyAir(container) &&
               isNullOrEmpty(blockEntities) &&
               isNullOrEmpty(entities) &&
               isNullOrEmpty(pendingBlockTicks) &&
               isNullOrEmpty(pendingFluidTicks);
    }

    static boolean hasOnlyAir(LitematicaBlockStateContainer container)
    {
        Vec3i size = container.getSize();

        for (int y = 0; y < size.getY(); ++y)
        {
            for (int z = 0; z < size.getZ(); ++z)
            {
                for (int x = 0; x < size.getX(); ++x)
                {
                    if (!container.get(x, y, z).isAir())
                    {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private static boolean isNullOrEmpty(@Nullable Map<?, ?> values)
    {
        return values == null || values.isEmpty();
    }

    private static boolean isNullOrEmpty(@Nullable Collection<?> values)
    {
        return values == null || values.isEmpty();
    }
}
