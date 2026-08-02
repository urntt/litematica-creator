package io.github.urntt.litematicacreator.creator;

import net.minecraft.world.item.ItemStack;

final class CreatorInventorySlotPolicy
{
    enum PickAction
    {
        SELECT,
        SWAP,
        INSERT
    }

    record PickPlan(PickAction action, int sourceSlot, int targetSlot, int displacedSlot)
    {
    }

    private CreatorInventorySlotPolicy()
    {
    }

    static PickPlan planPick(ItemStack[] stacks, int selectedHotbarSlot, ItemStack picked)
    {
        int matchingSlot = findMatchingStorageSlot(stacks, picked);

        if (matchingSlot >= 0 && matchingSlot < CreatorInventory.HOTBAR_SIZE)
        {
            return new PickPlan(PickAction.SELECT, matchingSlot, matchingSlot, -1);
        }

        int targetSlot = findSuitableHotbarSlot(stacks, selectedHotbarSlot);

        if (matchingSlot >= CreatorInventory.HOTBAR_SIZE)
        {
            return new PickPlan(PickAction.SWAP, matchingSlot, targetSlot, -1);
        }

        int displacedSlot = stacks[targetSlot].isEmpty() ? -1 : findFirstEmptyMainSlot(stacks);
        return new PickPlan(PickAction.INSERT, -1, targetSlot, displacedSlot);
    }

    static int findFirstEmptyHotbarSlot(ItemStack[] stacks, int selectedHotbarSlot)
    {
        int selected = Math.floorMod(selectedHotbarSlot, CreatorInventory.HOTBAR_SIZE);

        for (int offset = 0; offset < CreatorInventory.HOTBAR_SIZE; ++offset)
        {
            int slot = (selected + offset) % CreatorInventory.HOTBAR_SIZE;

            if (stacks[slot].isEmpty())
            {
                return slot;
            }
        }

        return -1;
    }

    static int findSuitableHotbarSlot(ItemStack[] stacks, int selectedHotbarSlot)
    {
        int selected = Math.floorMod(selectedHotbarSlot, CreatorInventory.HOTBAR_SIZE);
        int emptySlot = findFirstEmptyHotbarSlot(stacks, selected);

        if (emptySlot >= 0)
        {
            return emptySlot;
        }

        for (int offset = 0; offset < CreatorInventory.HOTBAR_SIZE; ++offset)
        {
            int slot = (selected + offset) % CreatorInventory.HOTBAR_SIZE;

            if (!stacks[slot].isEnchanted())
            {
                return slot;
            }
        }

        return selected;
    }

    private static int findMatchingStorageSlot(ItemStack[] stacks, ItemStack picked)
    {
        for (int slot = 0; slot < CreatorInventory.OFFHAND_SLOT; ++slot)
        {
            if (!stacks[slot].isEmpty() && ItemStack.isSameItemSameComponents(picked, stacks[slot]))
            {
                return slot;
            }
        }

        return -1;
    }

    private static int findFirstEmptyMainSlot(ItemStack[] stacks)
    {
        for (int slot = CreatorInventory.HOTBAR_SIZE; slot < CreatorInventory.OFFHAND_SLOT; ++slot)
        {
            if (stacks[slot].isEmpty())
            {
                return slot;
            }
        }

        return -1;
    }
}
