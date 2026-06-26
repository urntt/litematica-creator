package io.github.urntt.litematicacreator.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;

@Mixin(SchematicPlacement.class)
public interface SchematicPlacementAccessor
{
    @Mutable
    @Accessor("subRegionCount")
    void litematicacreator$setSubRegionCount(int subRegionCount);
}
