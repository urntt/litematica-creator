package io.github.urntt.litematicacreator.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.urntt.litematicacreator.gui.CreatorThumbnailCapture;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin
{
    @Inject(method = "render", at = @At("RETURN"))
    private void litematicacreator$captureThumbnailAfterWorldFrame(
            DeltaTracker deltaTracker,
            boolean renderLevel,
            CallbackInfo ci)
    {
        if (renderLevel)
        {
            CreatorThumbnailCapture.afterFrameRendered(Minecraft.getInstance());
        }
    }
}
