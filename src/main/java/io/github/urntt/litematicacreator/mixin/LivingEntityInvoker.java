package io.github.urntt.litematicacreator.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityInvoker
{
    @Invoker("updateSwimAmount")
    void litematicacreator$updateSwimAmount();
}
