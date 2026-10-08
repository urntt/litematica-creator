package io.github.urntt.litematicacreator.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.urntt.litematicacreator.creator.CreatorPlacementWorld;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateSurvivalMixin
{
    // A projection is a plan, not a finished build: torches, lanterns, doors and the like follow the clicked face and
    // view direction even where the projection holds nothing to support them. Real levels are unaffected.
    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
    private void litematicacreator$surviveInPlacementWorld(
            LevelReader level,
            BlockPos pos,
            CallbackInfoReturnable<Boolean> cir)
    {
        if (level instanceof CreatorPlacementWorld)
        {
            cir.setReturnValue(true);
        }
    }
}
