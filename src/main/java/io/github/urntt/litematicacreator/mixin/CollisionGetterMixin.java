package io.github.urntt.litematicacreator.mixin;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.urntt.litematicacreator.camera.CreatorCameraEntity;
import io.github.urntt.litematicacreator.camera.CreatorProjectionCollisions;

@Mixin(CollisionGetter.class)
public abstract class CollisionGetterMixin
{
    @Inject(method = "getBlockCollisions", at = @At("RETURN"), cancellable = true)
    private void litematicacreator$appendProjectionCollisions(
            Entity entity,
            AABB bounds,
            CallbackInfoReturnable<Iterable<VoxelShape>> cir)
    {
        Minecraft minecraft = Minecraft.getInstance();

        if (!(entity instanceof CreatorCameraEntity camera) ||
            camera.isCreatorFlying() ||
            minecraft.level == null ||
            (Object) this != minecraft.level)
        {
            return;
        }

        List<VoxelShape> projectionShapes = CreatorProjectionCollisions.collect(camera, minecraft.level, bounds);

        if (projectionShapes.isEmpty())
        {
            return;
        }

        List<VoxelShape> combined = new ArrayList<>();

        for (VoxelShape shape : cir.getReturnValue())
        {
            combined.add(shape);
        }

        combined.addAll(projectionShapes);
        cir.setReturnValue(combined);
    }
}
