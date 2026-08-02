package io.github.urntt.litematicacreator.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.urntt.litematicacreator.camera.CreatorCameraController;

@Mixin(value = Entity.class, priority = 1100)
public abstract class EntityTurnMixin
{
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void litematicacreator$turnCreatorCamera(double yawChange, double pitchChange, CallbackInfo ci)
    {
        Minecraft minecraft = Minecraft.getInstance();
        CreatorCameraController controller = CreatorCameraController.getInstance();

        if ((Object) this == minecraft.player && controller.isActive())
        {
            controller.turnCamera(yawChange, pitchChange);
            ci.cancel();
        }
    }
}
