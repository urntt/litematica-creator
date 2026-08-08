package io.github.urntt.litematicacreator.export;

import net.minecraft.core.BlockPos;

public record CreatorExportPreview(int regionCount, int totalBlocks, long totalVolume, BlockPos enclosingSize)
{
}
