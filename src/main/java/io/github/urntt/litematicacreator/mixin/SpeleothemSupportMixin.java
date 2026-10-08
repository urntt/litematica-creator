package io.github.urntt.litematicacreator.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SpeleothemBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.urntt.litematicacreator.creator.CreatorPlacementWorld;

@Mixin(SpeleothemBlock.class)
public abstract class SpeleothemSupportMixin
{
    // Pointed dripstone and sulfur spikes check their support directly while choosing a tip direction; in a projection
    // the tip follows the view, as for blocks whose survival is relaxed.
    @Inject(method = "isValidSpeleothemPlacement", at = @At("HEAD"), cancellable = true)
    private void litematicacreator$supportInPlacementWorld(
            LevelReader level,
            BlockPos pos,
            Direction tipDirection,
            CallbackInfoReturnable<Boolean> cir)
    {
        if (level instanceof CreatorPlacementWorld)
        {
            cir.setReturnValue(true);
        }
    }
}
