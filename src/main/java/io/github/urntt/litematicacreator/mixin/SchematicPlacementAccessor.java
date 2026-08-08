package io.github.urntt.litematicacreator.mixin;

import java.nio.file.Path;
import java.util.Map;
import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.selection.Box;

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

    @Mutable
    @Accessor("enclosingBox")
    void litematicacreator$setEnclosingBox(@Nullable Box enclosingBox);

    @Invoker("updateEnclosingBox")
    void litematicacreator$updateEnclosingBox();

    @Invoker("checkAreSubRegionsModified")
    void litematicacreator$checkAreSubRegionsModified();
}
