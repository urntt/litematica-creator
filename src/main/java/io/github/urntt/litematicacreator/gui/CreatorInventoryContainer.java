package io.github.urntt.litematicacreator.gui;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import io.github.urntt.litematicacreator.creator.CreatorInventory;

final class CreatorInventoryContainer implements Container
{
    private final CreatorInventory inventory;

    CreatorInventoryContainer(CreatorInventory inventory)
    {
        this.inventory = inventory;
    }

    @Override
    public int getContainerSize()
    {
        return CreatorInventory.DISCARD_SLOT;
    }

    @Override
    public boolean isEmpty()
    {
        for (int slot = 0; slot < this.getContainerSize(); ++slot)
        {
            if (!this.inventory.getStack(slot).isEmpty())
            {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getItem(int slot)
    {
        return this.inventory.getStack(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count)
    {
        ItemStack stack = this.inventory.getStack(slot);

        if (stack.isEmpty() || count <= 0)
        {
            return ItemStack.EMPTY;
        }

        ItemStack removed = stack.split(count);
        this.inventory.setStack(slot, stack);
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot)
    {
        ItemStack stack = this.inventory.getStack(slot);
        this.inventory.setStack(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack)
    {
        this.inventory.setStack(slot, stack);
    }

    @Override
    public void setChanged()
    {
    }

    @Override
    public boolean stillValid(Player player)
    {
        return true;
    }

    @Override
    public void clearContent()
    {
        this.inventory.runTransaction(() ->
        {
            for (int slot = 0; slot < this.getContainerSize(); ++slot)
            {
                this.inventory.setStack(slot, ItemStack.EMPTY);
            }
        });
    }
}
