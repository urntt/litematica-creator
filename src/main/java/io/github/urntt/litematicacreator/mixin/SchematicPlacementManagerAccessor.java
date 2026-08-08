package io.github.urntt.litematicacreator.mixin;

import java.util.Set;

import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;

@Mixin(SchematicPlacementManager.class)
public interface SchematicPlacementManagerAccessor
{
    @Accessor("chunksPreChange")
    Set<ChunkPos> litematicacreator$getChunksPreChange();

    @Invoker("onPrePlacementChange")
    void litematicacreator$onPrePlacementChange(SchematicPlacement placement);

    @Invoker("onPostPlacementChange")
    void litematicacreator$onPostPlacementChange(SchematicPlacement placement);
}
