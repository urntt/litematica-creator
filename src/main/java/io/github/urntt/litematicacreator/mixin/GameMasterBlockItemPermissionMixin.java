package io.github.urntt.litematicacreator.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.GameMasterBlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import io.github.urntt.litematicacreator.creator.CreatorPlacementWorld;

@Mixin(GameMasterBlockItem.class)
public abstract class GameMasterBlockItemPermissionMixin
{
    // Command, structure, jigsaw and test blocks need a creative operator to place for real. A projection edit changes
    // no real block, so like the virtual debug stick it does not ask for the real player's permissions.
    @WrapOperation(
            method = "getPlacementState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;canUseGameMasterBlocks()Z"
            )
    )
    private boolean litematicacreator$placeWithoutPermissions(
            Player player,
            Operation<Boolean> original,
            @Local(argsOnly = true) BlockPlaceContext context)
    {
        return context.getLevel() instanceof CreatorPlacementWorld || original.call(player);
    }
}
