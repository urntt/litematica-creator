package io.github.urntt.litematicacreator.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;

import fi.dy.masa.litematica.schematic.placement.PlacementManagerTaskRebuild;
import io.github.urntt.litematicacreator.creator.CreatorSchematicEditGuard;

@Mixin(PlacementManagerTaskRebuild.class)
public class PlacementManagerTaskRebuildMixin
{
    @WrapMethod(method = "run")
    private void litematicacreator$guardCreatorSchematicEdits(Operation<Void> original)
    {
        CreatorSchematicEditGuard.runRebuild(() -> original.call());
    }
}
