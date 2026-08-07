package io.github.urntt.litematicacreator.mixin;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.urntt.litematicacreator.camera.CreatorCameraEntity;
import io.github.urntt.litematicacreator.render.CreatorCameraRenderStates;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin
{
    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;",
            at = @At("RETURN")
    )
    private static void litematicacreator$makeCreatorPreviewTranslucent(
            LivingEntity entity,
            CallbackInfoReturnable<EntityRenderState> cir)
    {
        if (entity instanceof CreatorCameraEntity)
        {
            CreatorCameraRenderStates.makeCameraAvatarTranslucent(cir.getReturnValue());
        }
    }
}
