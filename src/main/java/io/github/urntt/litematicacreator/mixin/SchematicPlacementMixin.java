package io.github.urntt.litematicacreator.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import io.github.urntt.litematicacreator.creator.CreatorPlacementVisibility;

@Mixin(SchematicPlacement.class)
public class SchematicPlacementMixin
{
    @Inject(method = "matchesRequirement", at = @At("HEAD"), cancellable = true)
    private void litematicacreator$hideSuppressedPlacement(RequiredEnabled required, CallbackInfoReturnable<Boolean> cir)
    {
        if (required != RequiredEnabled.ANY && CreatorPlacementVisibility.isSuppressed((SchematicPlacement) (Object) this))
        {
            cir.setReturnValue(false);
        }
    }
}
