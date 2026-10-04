package io.github.urntt.litematicacreator.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fi.dy.masa.litematica.util.EntityUtils;
import io.github.urntt.litematicacreator.creator.CreatorManager;
import io.github.urntt.litematicacreator.render.CreatorVirtualLoadout;

@Mixin(EntityUtils.class)
public abstract class LitematicaEntityUtilsMixin
{
    // Every Litematica tool check passes the real player; in Creator mode the virtual hands hold the tool.
    @WrapOperation(
            method = "hasToolItemInHand",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;"
            )
    )
    private static ItemStack litematicacreator$readCreatorHand(
            LivingEntity entity,
            InteractionHand hand,
            Operation<ItemStack> original)
    {
        if (litematicacreator$usesCreatorHands(entity))
        {
            return hand == InteractionHand.MAIN_HAND ? CreatorVirtualLoadout.getMainHand() : CreatorVirtualLoadout.getOffhand();
        }

        return original.call(entity, hand);
    }

    @WrapOperation(
            method = "hasToolItemInHand",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getMainHandItem()Lnet/minecraft/world/item/ItemStack;"
            )
    )
    private static ItemStack litematicacreator$readCreatorMainHand(LivingEntity entity, Operation<ItemStack> original)
    {
        return litematicacreator$usesCreatorHands(entity) ? CreatorVirtualLoadout.getMainHand() : original.call(entity);
    }

    // Litematica's schematic pick block traces from the real player and fills the real inventory;
    // in Creator mode the middle click belongs to the virtual pick instead.
    @Inject(method = "shouldPickBlock", at = @At("HEAD"), cancellable = true)
    private static void litematicacreator$keepSchematicPickOffRealInventory(
            Player player,
            CallbackInfoReturnable<Boolean> cir)
    {
        if (CreatorManager.getInstance().isCreatorModeEnabled())
        {
            cir.setReturnValue(false);
        }
    }

    private static boolean litematicacreator$usesCreatorHands(LivingEntity entity)
    {
        return CreatorManager.getInstance().isCreatorModeEnabled() && entity == Minecraft.getInstance().player;
    }
}
