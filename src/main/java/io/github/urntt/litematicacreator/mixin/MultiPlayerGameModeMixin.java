package io.github.urntt.litematicacreator.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.urntt.litematicacreator.creator.CreatorManager;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin
{
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void litematicacreator$preventEntityAttack(Player player, Entity target, CallbackInfo ci)
    {
        if (CreatorManager.getInstance().isCreatorModeEnabled())
        {
            ci.cancel();
        }
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void litematicacreator$preventStartDestroyBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir)
    {
        if (CreatorManager.getInstance().isCreatorModeEnabled())
        {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void litematicacreator$preventContinueDestroyBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir)
    {
        if (CreatorManager.getInstance().isCreatorModeEnabled())
        {
            cir.setReturnValue(true);
        }
    }
}
