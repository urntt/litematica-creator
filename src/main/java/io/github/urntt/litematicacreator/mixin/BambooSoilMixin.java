package io.github.urntt.litematicacreator.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import io.github.urntt.litematicacreator.creator.CreatorPlacementWorld;

@Mixin(BambooStalkBlock.class)
public abstract class BambooSoilMixin
{
    // Bamboo checks for soil below while choosing its placement state; in a projection it is placed as if planted.
    @WrapOperation(
            method = "getStateForPlacement",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z"
            )
    )
    private boolean litematicacreator$soilInPlacementWorld(
            BlockState below,
            TagKey<Block> tag,
            Operation<Boolean> original,
            @Local(argsOnly = true) BlockPlaceContext context)
    {
        return tag.equals(BlockTags.SUPPORTS_BAMBOO) && context.getLevel() instanceof CreatorPlacementWorld || original.call(below, tag);
    }
}
