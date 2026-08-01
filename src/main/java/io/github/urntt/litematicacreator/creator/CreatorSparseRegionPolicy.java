package io.github.urntt.litematicacreator.creator;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;

public final class CreatorSparseRegionPolicy
{
    private CreatorSparseRegionPolicy()
    {
    }

    public static boolean isRemovableCell(String regionName, @Nullable BlockPos size)
    {
        return regionName.startsWith(CreatorSchematicEditor.CELL_REGION_PREFIX) && size != null &&
               Math.abs(size.getX()) == 1 && Math.abs(size.getY()) == 1 && Math.abs(size.getZ()) == 1;
    }
}
