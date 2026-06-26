package io.github.urntt.litematicacreator.creator;

import net.minecraft.core.BlockPos;

public record CreatorTilePos(int x, int y, int z)
{
    public static final int TILE_SIZE = 16;

    public static CreatorTilePos fromWorldPos(BlockPos pos)
    {
        return new CreatorTilePos(
                Math.floorDiv(pos.getX(), TILE_SIZE),
                Math.floorDiv(pos.getY(), TILE_SIZE),
                Math.floorDiv(pos.getZ(), TILE_SIZE)
        );
    }

    public BlockPos minBlockPos()
    {
        return new BlockPos(this.x * TILE_SIZE, this.y * TILE_SIZE, this.z * TILE_SIZE);
    }

    public String regionName()
    {
        return "tile_" + this.x + "_" + this.y + "_" + this.z;
    }
}
