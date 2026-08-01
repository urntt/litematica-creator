package io.github.urntt.litematicacreator.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;

@Mixin(SchematicPlacement.class)
public interface SchematicPlacementAccessor
{
    @Accessor("relativeSubRegionPlacements")
    Map<String, SubRegionPlacement> litematicacreator$getRelativeSubRegionPlacements();

    @Mutable
    @Accessor("subRegionCount")
    void litematicacreator$setSubRegionCount(int subRegionCount);
}
