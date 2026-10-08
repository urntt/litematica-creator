package io.github.urntt.litematicacreator.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.urntt.litematicacreator.creator.CreatorPlacementWorld;

@Mixin(MultifaceBlock.class)
public abstract class MultifaceAttachMixin
{
    // Glow lichen, sculk veins and vines pick their faces by attachment instead of survival; in a projection they
    // attach to the clicked face like any other block that needs no support there.
    @Inject(
            method = "canAttachTo(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void litematicacreator$attachInPlacementWorld(
            BlockGetter level,
            Direction directionTowardsNeighbour,
            BlockPos neighbourPos,
            BlockState neighbourState,
            CallbackInfoReturnable<Boolean> cir)
    {
        if (level instanceof CreatorPlacementWorld)
        {
            cir.setReturnValue(true);
        }
    }
}
