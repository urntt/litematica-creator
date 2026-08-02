package io.github.urntt.litematicacreator.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.urntt.litematicacreator.render.CreatorCameraRenderStates;
import io.github.urntt.litematicacreator.render.CreatorProjectionOutline;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorMixin
{
    @Inject(method = "extractVisibleEntities", at = @At("RETURN"))
    private void litematicacreator$appendCreatorCameraAvatars(
            Camera camera,
            Frustum frustum,
            DeltaTracker deltaTracker,
            LevelRenderState output,
            CallbackInfo ci)
    {
        CreatorCameraRenderStates.append((LevelExtractor) (Object) this, camera, frustum, deltaTracker, output);
    }

    @Inject(method = "extractBlockOutline", at = @At("RETURN"))
    private void litematicacreator$replaceProjectionOutline(
            Camera camera,
            LevelRenderState output,
            CallbackInfo ci)
    {
        CreatorProjectionOutline.replaceIfNeeded(Minecraft.getInstance(), camera, output);
    }
}
