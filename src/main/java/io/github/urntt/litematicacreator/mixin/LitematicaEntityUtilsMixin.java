package io.github.urntt.litematicacreator.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fi.dy.masa.litematica.util.EntityUtils;
import io.github.urntt.litematicacreator.creator.CreatorManager;

@Mixin(EntityUtils.class)
public abstract class LitematicaEntityUtilsMixin
{
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
}
