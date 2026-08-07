package io.github.urntt.litematicacreator.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityInvoker
{
    @Accessor("fallFlyTicks")
    void litematicacreator$setFallFlyTicks(int fallFlyTicks);

    @Invoker("updateSwimAmount")
    void litematicacreator$updateSwimAmount();
}
