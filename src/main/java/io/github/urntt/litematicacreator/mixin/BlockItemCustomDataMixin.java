package io.github.urntt.litematicacreator.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import io.github.urntt.litematicacreator.creator.CreatorPlacementWorld;

@Mixin(BlockItem.class)
public abstract class BlockItemCustomDataMixin
{
    // Vanilla loads an item's block entity data only on the server. Projection placement runs on the client but keeps
    // that data, in vanilla's order: custom data first, then the item's components.
    @WrapOperation(
            method = "updateCustomBlockEntityTag",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;isClientSide()Z")
    )
    private static boolean litematicacreator$loadInPlacementWorld(Level level, Operation<Boolean> original)
    {
        return !(level instanceof CreatorPlacementWorld) && original.call(level);
    }

    // Some block entity data needs a creative operator to load for real; like other projection edits, it does not ask
    // for the real player's permissions.
    @WrapOperation(
            method = "updateCustomBlockEntityTag",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;canUseGameMasterBlocks()Z")
    )
    private static boolean litematicacreator$loadWithoutPermissions(
            Player player,
            Operation<Boolean> original,
            @Local(argsOnly = true) Level level)
    {
        return level instanceof CreatorPlacementWorld || original.call(player);
    }
}
