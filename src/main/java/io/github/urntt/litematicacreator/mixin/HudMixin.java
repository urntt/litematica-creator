package io.github.urntt.litematicacreator.mixin;

import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import io.github.urntt.litematicacreator.creator.CreatorInventory;
import io.github.urntt.litematicacreator.creator.CreatorManager;
import io.github.urntt.litematicacreator.render.CreatorVirtualLoadout;

@Mixin(Hud.class)
public abstract class HudMixin
{
    @Redirect(
            method = "extractItemHotbar",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getOffhandItem()Lnet/minecraft/world/item/ItemStack;")
    )
    private ItemStack litematicacreator$getVirtualHudOffhand(Player player)
    {
        return CreatorManager.getInstance().isCreatorModeEnabled() ? CreatorVirtualLoadout.getOffhand() : player.getOffhandItem();
    }

    @Redirect(
            method = "extractItemHotbar",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;getSelectedSlot()I")
    )
    private int litematicacreator$getVirtualSelectedSlot(Inventory inventory)
    {
        return CreatorManager.getInstance().isCreatorModeEnabled()
                ? CreatorInventory.getInstance().getSelectedHotbarSlot()
                : inventory.getSelectedSlot();
    }

    @Redirect(
            method = "extractItemHotbar",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;getItem(I)Lnet/minecraft/world/item/ItemStack;")
    )
    private ItemStack litematicacreator$getVirtualHotbarStack(Inventory inventory, int slot)
    {
        return CreatorManager.getInstance().isCreatorModeEnabled()
                ? CreatorInventory.getInstance().getStack(slot)
                : inventory.getItem(slot);
    }

    @Redirect(
            method = "tick()V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;getSelectedItem()Lnet/minecraft/world/item/ItemStack;")
    )
    private ItemStack litematicacreator$getVirtualSelectedItemName(Inventory inventory)
    {
        return CreatorManager.getInstance().isCreatorModeEnabled()
                ? CreatorVirtualLoadout.getMainHand()
                : inventory.getSelectedItem();
    }
}
