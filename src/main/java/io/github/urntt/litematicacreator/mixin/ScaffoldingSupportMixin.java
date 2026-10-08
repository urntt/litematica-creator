package io.github.urntt.litematicacreator.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.ScaffoldingBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.urntt.litematicacreator.creator.CreatorPlacementWorld;

@Mixin(ScaffoldingBlock.class)
public abstract class ScaffoldingSupportMixin
{
    // Scaffolding that reaches no support has the maximum distance and cannot be placed; in a projection it counts as
    // standing on the ground instead.
    @Inject(method = "getDistance", at = @At("RETURN"), cancellable = true)
    private static void litematicacreator$supportInPlacementWorld(
            BlockGetter level,
            BlockPos pos,
            CallbackInfoReturnable<Integer> cir)
    {
        if (level instanceof CreatorPlacementWorld && cir.getReturnValueI() == ScaffoldingBlock.STABILITY_MAX_DISTANCE)
        {
            cir.setReturnValue(0);
        }
    }
}
