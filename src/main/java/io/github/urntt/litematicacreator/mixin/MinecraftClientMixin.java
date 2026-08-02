package io.github.urntt.litematicacreator.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.urntt.litematicacreator.creator.CreatorManager;
import io.github.urntt.litematicacreator.camera.CreatorCameraController;
import io.github.urntt.litematicacreator.recovery.CreatorRecoveryManager;

@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin
{
    @Inject(method = "close", at = @At("HEAD"))
    private void litematicacreator$flushRecoveryOnClose(CallbackInfo ci)
    {
        CreatorCameraController.getInstance().deactivate((Minecraft) (Object) this);
        CreatorRecoveryManager.getInstance().onClientShutdown();
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void litematicacreator$onStartUseItem(CallbackInfo ci)
    {
        if (CreatorManager.getInstance().isCreatorModeEnabled())
        {
            ci.cancel();
        }
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void litematicacreator$onStartAttack(CallbackInfoReturnable<Boolean> cir)
    {
        if (CreatorManager.getInstance().isCreatorModeEnabled())
        {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void litematicacreator$onContinueAttack(boolean leftClick, CallbackInfo ci)
    {
        if (CreatorManager.getInstance().isCreatorModeEnabled())
        {
            ci.cancel();
        }
    }
}
