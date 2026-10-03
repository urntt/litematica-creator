package io.github.urntt.litematicacreator.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.urntt.litematicacreator.render.CreatorCameraRenderStates;
import io.github.urntt.litematicacreator.render.CreatorFirstPersonPose;
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

    @WrapOperation(
            method = "extractPlayerState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/FirstPersonHandsAndItems;extractRenderState(Lnet/minecraft/client/player/LocalPlayer;FLnet/minecraft/client/renderer/state/level/FirstPersonHandsAndItemsRenderState;)V"
            )
    )
    private void litematicacreator$extractFirstPersonHandsFromCreatorCamera(
            FirstPersonHandsAndItems hands,
            LocalPlayer player,
            float partialTicks,
            FirstPersonHandsAndItemsRenderState state,
            Operation<Void> original)
    {
        original.call(hands, CreatorFirstPersonPose.handsPoseSource(player), partialTicks, state);
    }

    @Inject(method = "extractPlayerState", at = @At("TAIL"))
    private void litematicacreator$applyCreatorFirstPersonPose(
            Camera camera,
            DeltaTracker deltaTracker,
            float worldPartialTicks,
            PlayerRenderState state,
            CallbackInfo ci)
    {
        CreatorFirstPersonPose.apply(state, deltaTracker);
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
