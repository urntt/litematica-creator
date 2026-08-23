package io.github.urntt.litematicacreator.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.urntt.litematicacreator.camera.CreatorCameraCollisionResolver;
import io.github.urntt.litematicacreator.camera.CreatorCameraEntity;

@Mixin(Entity.class)
public abstract class EntityCollisionMixin
{
    @Inject(method = "collide", at = @At("HEAD"), cancellable = true, require = 0)
    private void litematicacreator$collideWithProjections(
            Vec3 movement,
            CallbackInfoReturnable<Vec3> cir)
    {
        if ((Object) this instanceof CreatorCameraEntity camera)
        {
            Vec3 resolvedMovement = CreatorCameraCollisionResolver.resolveIfProjectionCollision(camera, movement);

            if (resolvedMovement != null)
            {
                cir.setReturnValue(resolvedMovement);
            }
        }
    }
}
