package io.github.urntt.litematicacreator.mixin;

import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import io.github.urntt.litematicacreator.creator.CreatorManager;
import io.github.urntt.litematicacreator.render.CreatorVirtualLoadout;

@Mixin(FirstPersonHandsAndItems.class)
public abstract class FirstPersonHandsAndItemsMixin
{
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
            method = "extractRenderState",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isScoping()Z")
    )
    private boolean litematicacreator$ignoreRealScoping(LocalPlayer player)
    {
        return !CreatorManager.getInstance().isCreatorModeEnabled() && player.isScoping();
    }
}
