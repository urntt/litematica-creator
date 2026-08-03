package io.github.urntt.litematicacreator.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;

import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import io.github.urntt.litematicacreator.creator.CreatorSchematicEditGuard;

@Mixin(SchematicPlacementManager.class)
public class SchematicPlacementManagerRefreshMixin
{
    @WrapMethod(method = "markChunkForRebuild(II)V")
    private void litematicacreator$suppressFullPlacementRebuild(
            int chunkX,
            int chunkZ,
            Operation<Void> original)
    {
        if (!CreatorSchematicEditGuard.isPlacementRefreshSchedulingSuppressed())
        {
            original.call(chunkX, chunkZ);
        }
    }

    @WrapMethod(method = "markChunkForUnload(II)V")
    private void litematicacreator$suppressFullPlacementUnload(
            int chunkX,
            int chunkZ,
            Operation<Void> original)
    {
        if (!CreatorSchematicEditGuard.isPlacementRefreshSchedulingSuppressed())
        {
            original.call(chunkX, chunkZ);
        }
    }
}
