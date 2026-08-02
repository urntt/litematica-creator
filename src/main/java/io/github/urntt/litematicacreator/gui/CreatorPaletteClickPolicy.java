package io.github.urntt.litematicacreator.gui;

import net.minecraft.world.item.ItemStack;

final class CreatorPaletteClickPolicy
{
    private CreatorPaletteClickPolicy()
    {
    }

    static ItemStack applyClick(ItemStack carried, ItemStack clicked, int button, boolean quickMove)
    {
        ItemStack result = carried.copy();

        if (!result.isEmpty() && !clicked.isEmpty() && ItemStack.isSameItemSameComponents(result, clicked))
        {
            if (button == 0)
            {
                if (quickMove)
                {
                    result.setCount(result.getMaxStackSize());
                }
                else if (result.getCount() < result.getMaxStackSize())
                {
                    result.grow(1);
                }
            }
            else
            {
                result.shrink(1);
            }
        }
        else if (!clicked.isEmpty() && result.isEmpty())
        {
            int count = quickMove ? clicked.getMaxStackSize() : clicked.getCount();
            result = clicked.copyWithCount(count);
        }
        else if (button == 0)
        {
            result = ItemStack.EMPTY;
        }
        else if (!result.isEmpty())
        {
            result.shrink(1);
        }

        return result.isEmpty() ? ItemStack.EMPTY : result;
    }
}
