package io.github.urntt.litematicacreator.mixin;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import io.github.urntt.litematicacreator.camera.CreatorCameraController;
import io.github.urntt.litematicacreator.camera.CreatorCameraEntity;
import io.github.urntt.litematicacreator.creator.CreatorManager;
import io.github.urntt.litematicacreator.render.CreatorVirtualLoadout;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin
{
    @ModifyVariable(method = "submitHandsWithItems", at = @At("HEAD"), argsOnly = true)
    private LocalPlayer litematicacreator$useCreatorCameraForFirstPersonHands(LocalPlayer player)
    {
        CreatorCameraEntity camera = CreatorCameraController.getInstance().getCamera();
        return camera != null ? camera : player;
    }

    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getMainHandItem()Lnet/minecraft/world/item/ItemStack;")
    )
    private ItemStack litematicacreator$getVirtualMainHandForTick(LocalPlayer player)
    {
        return CreatorManager.getInstance().isCreatorModeEnabled() ? CreatorVirtualLoadout.getMainHand() : player.getMainHandItem();
    }

    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getOffhandItem()Lnet/minecraft/world/item/ItemStack;")
    )
    private ItemStack litematicacreator$getVirtualOffhandForTick(LocalPlayer player)
    {
        return CreatorManager.getInstance().isCreatorModeEnabled() ? CreatorVirtualLoadout.getOffhand() : player.getOffhandItem();
    }

    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isHandsBusy()Z")
    )
    private boolean litematicacreator$ignoreRealBusyHands(LocalPlayer player)
    {
        return !CreatorManager.getInstance().isCreatorModeEnabled() && player.isHandsBusy();
    }

    @Redirect(
            method = "evaluateWhichHandsToRender",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getMainHandItem()Lnet/minecraft/world/item/ItemStack;")
    )
    private static ItemStack litematicacreator$getVirtualMainHandForSelection(LocalPlayer player)
    {
        return CreatorManager.getInstance().isCreatorModeEnabled() ? CreatorVirtualLoadout.getMainHand() : player.getMainHandItem();
    }

    @Redirect(
            method = "evaluateWhichHandsToRender",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getOffhandItem()Lnet/minecraft/world/item/ItemStack;")
    )
    private static ItemStack litematicacreator$getVirtualOffhandForSelection(LocalPlayer player)
    {
        return CreatorManager.getInstance().isCreatorModeEnabled() ? CreatorVirtualLoadout.getOffhand() : player.getOffhandItem();
    }

    @Redirect(
            method = "evaluateWhichHandsToRender",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z")
    )
    private static boolean litematicacreator$ignoreRealUseForSelection(LocalPlayer player)
    {
        return !CreatorManager.getInstance().isCreatorModeEnabled() && player.isUsingItem();
    }

    @Redirect(
            method = "submitArmWithItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;isUsingItem()Z")
    )
    private boolean litematicacreator$ignoreRealUseAnimation(AbstractClientPlayer player)
    {
        return !CreatorManager.getInstance().isCreatorModeEnabled() && player.isUsingItem();
    }

    @Redirect(
            method = "submitArmWithItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;isScoping()Z")
    )
    private boolean litematicacreator$ignoreRealScoping(AbstractClientPlayer player)
    {
        return !CreatorManager.getInstance().isCreatorModeEnabled() && player.isScoping();
    }
}
