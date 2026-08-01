package io.github.urntt.litematicacreator.mixin;

import java.nio.file.Path;
import java.util.Map;
import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;

@Mixin(SchematicPlacement.class)
public interface SchematicPlacementAccessor
{
    @Mutable
    @Accessor("schematicFile")
    void litematicacreator$setSchematicFile(@Nullable Path file);

    @Accessor("relativeSubRegionPlacements")
    Map<String, SubRegionPlacement> litematicacreator$getRelativeSubRegionPlacements();

    @Mutable
    @Accessor("subRegionCount")
    void litematicacreator$setSubRegionCount(int subRegionCount);
}
