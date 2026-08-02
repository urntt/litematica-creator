package io.github.urntt.litematicacreator.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.urntt.litematicacreator.render.CreatorProjectionOutline;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorMixin
{
    @Inject(method = "extractBlockOutline", at = @At("RETURN"))
    private void litematicacreator$replaceProjectionOutline(
            Camera camera,
            LevelRenderState output,
            CallbackInfo ci)
    {
        CreatorProjectionOutline.replaceIfNeeded(Minecraft.getInstance(), camera, output);
    }
}
