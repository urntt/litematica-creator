package io.github.urntt.litematicacreator.mixin;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererAccessor
{
    @Accessor("itemModelResolver")
    ItemModelResolver litematicacreator$getItemModelResolver();
}
