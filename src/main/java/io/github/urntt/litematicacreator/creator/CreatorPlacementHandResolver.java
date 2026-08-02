package io.github.urntt.litematicacreator.creator;

import javax.annotation.Nullable;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

final class CreatorPlacementHandResolver
{
    private CreatorPlacementHandResolver()
    {
    }

    @Nullable
    static Selection resolve(ItemStack mainHand, ItemStack offhand)
    {
        if (mainHand.getItem() instanceof BlockItem blockItem)
        {
            return new Selection(InteractionHand.MAIN_HAND, mainHand, blockItem);
        }

        if (offhand.getItem() instanceof BlockItem blockItem)
        {
            return new Selection(InteractionHand.OFF_HAND, offhand, blockItem);
        }

        return null;
    }

    record Selection(InteractionHand hand, ItemStack stack, BlockItem blockItem)
    {
    }
}
