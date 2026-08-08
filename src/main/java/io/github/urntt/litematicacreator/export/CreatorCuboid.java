package io.github.urntt.litematicacreator.export;

import net.minecraft.core.BlockPos;

record CreatorCuboid(BlockPos min, BlockPos max)
{
    int sizeX()
    {
        return this.max.getX() - this.min.getX() + 1;
    }

    int sizeY()
    {
        return this.max.getY() - this.min.getY() + 1;
    }

    int sizeZ()
    {
        return this.max.getZ() - this.min.getZ() + 1;
    }

    int volume()
    {
        return this.sizeX() * this.sizeY() * this.sizeZ();
    }
}
