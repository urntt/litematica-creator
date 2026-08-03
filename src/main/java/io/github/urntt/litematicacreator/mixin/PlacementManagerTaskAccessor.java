package io.github.urntt.litematicacreator.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import fi.dy.masa.litematica.schematic.placement.PlacementManagerTask;

@Mixin(PlacementManagerTask.class)
public interface PlacementManagerTaskAccessor
{
    @Accessor("chunkLong")
    Long litematicacreator$getChunkLong();
}
