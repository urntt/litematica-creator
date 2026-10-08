package io.github.urntt.litematicacreator.gui;

import java.util.Map;
import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import io.github.urntt.litematicacreator.creator.CreatorBlockEntityEditSession;
import io.github.urntt.litematicacreator.creator.CreatorInventory;

/**
 * Handles clicks in a vanilla container screen opened on a projection. The player slots show the Creator virtual
 * inventory through a detached {@link Inventory}, and every click runs locally, so neither the real inventory nor the
 * server is involved. Clicks that vanilla would apply to the real inventory or turn into a real drop follow the virtual
 * inventory screen instead: swaps use the virtual hotbar and offhand, and thrown or discarded stacks disappear.
 */
public final class CreatorProjectionContainerInteraction
{
    private static final int PLAYER_INVENTORY_SIZE = CreatorInventory.HOTBAR_SIZE + CreatorInventory.MAIN_SIZE;
    private static final int OFFHAND_SWAP_BUTTON = 40;

    private final Minecraft minecraft;
    private final CreatorBlockEntityEditSession session;
    private final Inventory inventory;
    @Nullable private AbstractContainerMenu menu;
    private boolean closed;

    CreatorProjectionContainerInteraction(Minecraft minecraft, CreatorBlockEntityEditSession session)
    {
        this.minecraft = minecraft;
        this.session = session;
        this.inventory = new Inventory(minecraft.player, new EntityEquipment());
        CreatorInventory virtual = CreatorInventory.getInstance();

        for (int slot = 0; slot < PLAYER_INVENTORY_SIZE; ++slot)
        {
            this.inventory.setItem(slot, virtual.getStack(slot).copy());
        }
    }

    Inventory inventory()
    {
        return this.inventory;
    }

    void bind(AbstractContainerMenu menu)
    {
        this.menu = menu;
    }

    public AbstractContainerMenu menu()
    {
        return this.menu;
    }

    public void slotClicked(@Nullable Slot slot, int slotId, int buttonNum, ContainerInput input)
    {
        if (this.menu == null || this.closed)
        {
            return;
        }

        if (slot == null || slotId == -999)
        {
            if (input == ContainerInput.QUICK_CRAFT)
            {
                this.menu.clicked(slotId, buttonNum, input, this.minecraft.player);
            }
            else if (input == ContainerInput.THROW || input == ContainerInput.PICKUP)
            {
                this.discardCarried(buttonNum == 0);
            }
        }
        else
        {
            switch (input)
            {
                case SWAP -> this.swap(slot, buttonNum);
                case CLONE ->
                {
                    if (this.menu.getCarried().isEmpty() && slot.hasItem())
                    {
                        ItemStack stack = slot.getItem();
                        this.menu.setCarried(stack.copyWithCount(stack.getMaxStackSize()));
                    }
                }
                case THROW ->
                {
                    if (this.menu.getCarried().isEmpty() && slot.hasItem())
                    {
                        slot.remove(buttonNum == 0 ? 1 : slot.getItem().getCount());
                        slot.setChanged();
                    }
                }
                default -> this.menu.clicked(slot.index, buttonNum, input, this.minecraft.player);
            }
        }

        this.syncVirtualInventory();
    }

    /** Ends the interaction: a stack still on the cursor goes back to the virtual inventory, then the edit is committed. */
    public void close()
    {
        if (this.closed)
        {
            return;
        }

        this.closed = true;

        if (this.menu != null)
        {
            ItemStack carried = this.menu.getCarried();
            this.menu.setCarried(ItemStack.EMPTY);

            if (!carried.isEmpty())
            {
                this.inventory.add(carried);
            }
        }

        this.syncVirtualInventory();
        this.session.commit(Map.of());
    }

    private void swap(Slot slot, int buttonNum)
    {
        boolean offhand = buttonNum == OFFHAND_SWAP_BUTTON;

        if (!offhand && (buttonNum < 0 || buttonNum >= CreatorInventory.HOTBAR_SIZE) ||
            slot.container == this.inventory && slot.getContainerSlot() == buttonNum)
        {
            return;
        }

        CreatorInventory virtual = CreatorInventory.getInstance();
        ItemStack slotStack = slot.getItem().copy();
        ItemStack target = offhand ? virtual.getStack(CreatorInventory.OFFHAND_SLOT).copy() : this.inventory.getItem(buttonNum).copy();

        if (!target.isEmpty() && !slot.mayPlace(target) || !slotStack.isEmpty() && !slot.mayPickup(this.minecraft.player))
        {
            return;
        }

        slot.set(target);

        if (offhand)
        {
            virtual.runTransaction(() -> virtual.setStack(CreatorInventory.OFFHAND_SLOT, slotStack));
        }
        else
        {
            this.inventory.setItem(buttonNum, slotStack);
        }
    }

    private void discardCarried(boolean all)
    {
        ItemStack carried = this.menu.getCarried();

        if (all)
        {
            this.menu.setCarried(ItemStack.EMPTY);
        }
        else if (!carried.isEmpty())
        {
            carried.shrink(1);
        }
    }

    private void syncVirtualInventory()
    {
        CreatorInventory virtual = CreatorInventory.getInstance();
        virtual.runTransaction(() ->
        {
            for (int slot = 0; slot < PLAYER_INVENTORY_SIZE; ++slot)
            {
                virtual.setStack(slot, this.inventory.getItem(slot).copy());
            }
        });
    }
}
