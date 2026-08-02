package io.github.urntt.litematicacreator.gui;

import java.util.function.IntPredicate;

import net.minecraft.world.item.ItemStack;

import io.github.urntt.litematicacreator.creator.CreatorInventory;

final class CreatorPaletteClickPolicy
{
    private CreatorPaletteClickPolicy()
    {
    }

    static ItemStack applyNormalClick(ItemStack carried, ItemStack clicked, int button)
    {
        ItemStack result = carried.copy();

        if (!result.isEmpty() && !clicked.isEmpty() && ItemStack.isSameItemSameComponents(result, clicked))
        {
            if (button == 0)
            {
                if (result.getCount() < result.getMaxStackSize())
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
            result = clicked.copy();
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

    static int findFirstEmptyHotbarSlot(int selectedHotbarSlot, IntPredicate isEmpty)
    {
        int selected = Math.floorMod(selectedHotbarSlot, CreatorInventory.HOTBAR_SIZE);

        for (int offset = 0; offset < CreatorInventory.HOTBAR_SIZE; ++offset)
        {
            int slot = (selected + offset) % CreatorInventory.HOTBAR_SIZE;

            if (isEmpty.test(slot))
            {
                return slot;
            }
        }

        return -1;
    }
}
