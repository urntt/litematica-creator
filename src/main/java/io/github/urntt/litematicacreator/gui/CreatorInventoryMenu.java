package io.github.urntt.litematicacreator.gui;

import java.util.List;
import java.util.function.BooleanSupplier;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import io.github.urntt.litematicacreator.creator.CreatorInventory;

final class CreatorInventoryMenu extends AbstractContainerMenu
{
    private static final int PALETTE_SIZE = 45;
    private static final int NORMAL_HOTBAR_START = PALETTE_SIZE;
    private static final int NORMAL_HOTBAR_END = NORMAL_HOTBAR_START + CreatorInventory.HOTBAR_SIZE;

    final NonNullList<ItemStack> items = NonNullList.create();

    private final CreatorInventory inventory;
    private final CreatorInventoryContainer inventoryContainer;
    private final SimpleContainer palette = new SimpleContainer(PALETTE_SIZE);
    private final TrashContainer trash = new TrashContainer();
    private final Player player;
    private boolean inventoryLayout;
    private Slot trashSlot;

    CreatorInventoryMenu(Player player)
    {
        super(null, 0);
        this.player = player;
        this.inventory = CreatorInventory.getInstance();
        this.inventoryContainer = new CreatorInventoryContainer(this.inventory);

        for (int y = 0; y < 5; ++y)
        {
            for (int x = 0; x < 9; ++x)
            {
                this.addSlot(new PaletteSlot(this.palette, y * 9 + x, 9 + x * 18, 18 + y * 18, () -> !this.inventoryLayout));
            }
        }

        for (int slot = 0; slot < CreatorInventory.HOTBAR_SIZE; ++slot)
        {
            this.addSlot(new LayoutSlot(this.inventoryContainer, slot, 9 + slot * 18, 112, () -> !this.inventoryLayout));
        }

        this.addInventoryLayoutSlots();
        this.scrollTo(0.0F);
    }

    @Override
    public boolean stillValid(Player player)
    {
        return true;
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player player)
    {
        this.inventory.runTransaction(() -> super.clicked(slotIndex, buttonNum, input, player));
    }

    void setInventoryLayout(boolean inventoryLayout)
    {
        this.inventoryLayout = inventoryLayout;
    }

    boolean isInventoryLayout()
    {
        return this.inventoryLayout;
    }

    boolean isPaletteSlot(@Nullable Slot slot)
    {
        return slot != null && slot.container == this.palette;
    }

    boolean isTrashSlot(@Nullable Slot slot)
    {
        return slot == this.trashSlot;
    }

    void populate(List<ItemStack> stacks)
    {
        this.items.clear();
        this.items.addAll(stacks);
        this.scrollTo(0.0F);
    }

    int getRowIndexForScroll(float scroll)
    {
        return Math.max((int)(scroll * this.calculateRowCount() + 0.5F), 0);
    }

    float getScrollForRowIndex(int row)
    {
        int rows = this.calculateRowCount();
        return rows <= 0 ? 0.0F : Mth.clamp((float)row / rows, 0.0F, 1.0F);
    }

    float subtractInputFromScroll(float scroll, double input)
    {
        int rows = this.calculateRowCount();
        return rows <= 0 ? 0.0F : Mth.clamp(scroll - (float)(input / rows), 0.0F, 1.0F);
    }

    void scrollTo(float scroll)
    {
        int row = this.getRowIndexForScroll(scroll);

        for (int y = 0; y < 5; ++y)
        {
            for (int x = 0; x < 9; ++x)
            {
                int source = x + (y + row) * 9;
                ItemStack stack = source >= 0 && source < this.items.size() ? this.items.get(source) : ItemStack.EMPTY;
                this.palette.setItem(x + y * 9, stack);
            }
        }
    }

    boolean canScroll()
    {
        return this.items.size() > PALETTE_SIZE;
    }

    void copyPaletteToHotbar(Slot source, int hotbarSlot)
    {
        if (hotbarSlot < 0 || hotbarSlot >= CreatorInventory.HOTBAR_SIZE || source.getItem().isEmpty())
        {
            return;
        }

        ItemStack copy = source.getItem().copyWithCount(source.getItem().getMaxStackSize());
        this.inventory.setStack(hotbarSlot, copy);
    }

    void swapVirtualSlot(Slot source, int targetInventorySlot)
    {
        if (targetInventorySlot < 0 || targetInventorySlot >= CreatorInventory.DISCARD_SLOT || source.container != this.inventoryContainer)
        {
            return;
        }

        int sourceInventorySlot = source.getContainerSlot();

        if (sourceInventorySlot == targetInventorySlot)
        {
            return;
        }

        Slot target = this.findActiveVirtualSlot(targetInventorySlot);
        ItemStack sourceStack = this.inventory.getStack(sourceInventorySlot);
        ItemStack targetStack = this.inventory.getStack(targetInventorySlot);

        if (target == null || !source.mayPlace(targetStack) || !target.mayPlace(sourceStack))
        {
            return;
        }

        this.inventory.runTransaction(() ->
        {
            this.inventory.setStack(sourceInventorySlot, targetStack);
            this.inventory.setStack(targetInventorySlot, sourceStack);
        });
    }

    void clearAllVirtualSlots()
    {
        this.inventoryContainer.clearContent();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex)
    {
        if (slotIndex < 0 || slotIndex >= this.slots.size())
        {
            return ItemStack.EMPTY;
        }

        Slot source = this.slots.get(slotIndex);

        if (!source.isActive() || source.container != this.inventoryContainer || !source.hasItem())
        {
            return ItemStack.EMPTY;
        }

        ItemStack before = source.getItem().copy();
        int sourceSlot = source.getContainerSlot();

        if (!this.inventoryLayout)
        {
            source.setByPlayer(ItemStack.EMPTY);
            return before;
        }

        ItemStack remaining = before.copy();
        EquipmentSlot equipmentSlot = player.getEquipmentSlotForItem(remaining);

        if (sourceSlot < CreatorInventory.ARMOR_START && equipmentSlot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR)
        {
            int target = inventorySlotForEquipment(equipmentSlot);

            if (this.inventory.getStack(target).isEmpty())
            {
                this.moveIntoSlot(remaining, target);
            }
        }
        else if (sourceSlot < CreatorInventory.ARMOR_START && equipmentSlot == EquipmentSlot.OFFHAND && this.inventory.getStack(CreatorInventory.OFFHAND_SLOT).isEmpty())
        {
            this.moveIntoSlot(remaining, CreatorInventory.OFFHAND_SLOT);
        }

        if (!remaining.isEmpty())
        {
            if (sourceSlot >= CreatorInventory.HOTBAR_SIZE && sourceSlot < CreatorInventory.OFFHAND_SLOT)
            {
                this.moveIntoRange(remaining, 0, CreatorInventory.HOTBAR_SIZE);
            }
            else if (sourceSlot < CreatorInventory.HOTBAR_SIZE)
            {
                this.moveIntoRange(remaining, CreatorInventory.HOTBAR_SIZE, CreatorInventory.OFFHAND_SLOT);
            }
            else
            {
                this.moveIntoRange(remaining, CreatorInventory.HOTBAR_SIZE, CreatorInventory.OFFHAND_SLOT);
                this.moveIntoRange(remaining, 0, CreatorInventory.HOTBAR_SIZE);
            }
        }

        if (ItemStack.matches(before, remaining))
        {
            return ItemStack.EMPTY;
        }

        source.setByPlayer(remaining);
        return before;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot target)
    {
        return target.container == this.inventoryContainer && target.isActive();
    }

    @Override
    public boolean canDragTo(Slot slot)
    {
        return slot.container == this.inventoryContainer && slot.isActive();
    }

    private void addInventoryLayoutSlots()
    {
        EquipmentSlot[] armorSlots = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
        int[] inventorySlots = {
                CreatorInventory.ARMOR_HEAD_SLOT,
                CreatorInventory.ARMOR_CHEST_SLOT,
                CreatorInventory.ARMOR_LEGS_SLOT,
                CreatorInventory.ARMOR_FEET_SLOT
        };
        Identifier[] icons = {
                InventoryMenu.EMPTY_ARMOR_SLOT_HELMET,
                InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
                InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS,
                InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS
        };

        for (int position = 0; position < armorSlots.length; ++position)
        {
            int column = position / 2;
            int row = position % 2;
            this.addSlot(new CreatorArmorSlot(
                    this.inventoryContainer,
                    inventorySlots[position],
                    54 + column * 54,
                    6 + row * 27,
                    armorSlots[position],
                    icons[position],
                    () -> this.inventoryLayout,
                    this.player
            ));
        }

        this.addSlot(new LayoutSlot(this.inventoryContainer, CreatorInventory.OFFHAND_SLOT, 35, 20, () -> this.inventoryLayout)
        {
            @Override
            public @Nullable Identifier getNoItemIcon()
            {
                return InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD;
            }
        });

        for (int slot = CreatorInventory.HOTBAR_SIZE; slot < CreatorInventory.OFFHAND_SLOT; ++slot)
        {
            int position = slot - CreatorInventory.HOTBAR_SIZE;
            this.addSlot(new LayoutSlot(
                    this.inventoryContainer,
                    slot,
                    9 + position % 9 * 18,
                    54 + position / 9 * 18,
                    () -> this.inventoryLayout
            ));
        }

        for (int slot = 0; slot < CreatorInventory.HOTBAR_SIZE; ++slot)
        {
            this.addSlot(new LayoutSlot(this.inventoryContainer, slot, 9 + slot * 18, 112, () -> this.inventoryLayout));
        }

        this.trashSlot = this.addSlot(new LayoutSlot(this.trash, 0, 173, 112, () -> this.inventoryLayout));
    }

    private int calculateRowCount()
    {
        return Math.max(Mth.positiveCeilDiv(this.items.size(), 9) - 5, 0);
    }

    private Slot findActiveVirtualSlot(int inventorySlot)
    {
        for (Slot slot : this.slots)
        {
            if (slot.container == this.inventoryContainer && slot.getContainerSlot() == inventorySlot && slot.isActive())
            {
                return slot;
            }
        }

        return null;
    }

    private void moveIntoRange(ItemStack source, int start, int end)
    {
        for (int slot = start; slot < end && !source.isEmpty(); ++slot)
        {
            ItemStack target = this.inventory.getStack(slot);

            if (ItemStack.isSameItemSameComponents(source, target) && target.getCount() < target.getMaxStackSize())
            {
                int moved = Math.min(source.getCount(), target.getMaxStackSize() - target.getCount());
                target.grow(moved);
                source.shrink(moved);
                this.inventory.setStack(slot, target);
            }
        }

        for (int slot = start; slot < end && !source.isEmpty(); ++slot)
        {
            if (this.inventory.getStack(slot).isEmpty())
            {
                this.moveIntoSlot(source, slot);
            }
        }
    }

    private void moveIntoSlot(ItemStack source, int slot)
    {
        Slot targetSlot = this.findActiveVirtualSlot(slot);

        if (targetSlot == null || !targetSlot.mayPlace(source))
        {
            return;
        }

        int count = Math.min(source.getCount(), targetSlot.getMaxStackSize(source));
        this.inventory.setStack(slot, source.copyWithCount(count));
        source.shrink(count);
    }

    private static int inventorySlotForEquipment(EquipmentSlot equipmentSlot)
    {
        return switch (equipmentSlot)
        {
            case HEAD -> CreatorInventory.ARMOR_HEAD_SLOT;
            case CHEST -> CreatorInventory.ARMOR_CHEST_SLOT;
            case LEGS -> CreatorInventory.ARMOR_LEGS_SLOT;
            case FEET -> CreatorInventory.ARMOR_FEET_SLOT;
            default -> -1;
        };
    }

    private static class LayoutSlot extends Slot
    {
        private final BooleanSupplier active;

        LayoutSlot(Container container, int slot, int x, int y, BooleanSupplier active)
        {
            super(container, slot, x, y);
            this.active = active;
        }

        @Override
        public boolean isActive()
        {
            return this.active.getAsBoolean();
        }
    }

    private static final class PaletteSlot extends LayoutSlot
    {
        PaletteSlot(Container container, int slot, int x, int y, BooleanSupplier active)
        {
            super(container, slot, x, y, active);
        }

        @Override
        public boolean mayPickup(Player player)
        {
            ItemStack stack = this.getItem();
            return stack.isEmpty() || stack.isItemEnabled(player.level().enabledFeatures()) && !stack.has(DataComponents.CREATIVE_SLOT_LOCK);
        }

        @Override
        public boolean mayPlace(ItemStack stack)
        {
            return false;
        }
    }

    private static final class CreatorArmorSlot extends LayoutSlot
    {
        private final EquipmentSlot equipmentSlot;
        private final Identifier icon;
        private final Player player;

        CreatorArmorSlot(Container container,
                         int slot,
                         int x,
                         int y,
                         EquipmentSlot equipmentSlot,
                         Identifier icon,
                         BooleanSupplier active,
                         Player player)
        {
            super(container, slot, x, y, active);
            this.equipmentSlot = equipmentSlot;
            this.icon = icon;
            this.player = player;
        }

        @Override
        public int getMaxStackSize()
        {
            return 1;
        }

        @Override
        public boolean mayPlace(ItemStack stack)
        {
            return this.player.isEquippableInSlot(stack, this.equipmentSlot);
        }

        @Override
        public @Nullable Identifier getNoItemIcon()
        {
            return this.icon;
        }
    }

    private static final class TrashContainer extends SimpleContainer
    {
        TrashContainer()
        {
            super(1);
        }

        @Override
        public ItemStack getItem(int slot)
        {
            return ItemStack.EMPTY;
        }

        @Override
        public void setItem(int slot, ItemStack stack)
        {
        }
    }
}
