package io.github.urntt.litematicacreator.render;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

import io.github.urntt.litematicacreator.creator.CreatorInventory;

public final class CreatorVirtualLoadout
{
    private static ItemStack cachedMainHand = ItemStack.EMPTY;
    private static ItemStack cachedOffhand = ItemStack.EMPTY;

    private CreatorVirtualLoadout()
    {
    }

    public static ItemStack getMainHand()
    {
        ItemStack current = CreatorInventory.getInstance().getSelectedStack();
        cachedMainHand = stabilize(cachedMainHand, current);
        return cachedMainHand;
    }

    public static ItemStack getOffhand()
    {
        ItemStack current = CreatorInventory.getInstance().getStack(CreatorInventory.OFFHAND_SLOT);
        cachedOffhand = stabilize(cachedOffhand, current);
        return cachedOffhand;
    }

    public static ItemStack getForArm(HumanoidArm mainArm, HumanoidArm renderedArm)
    {
        return armUsesMainHand(mainArm, renderedArm) ? getMainHand() : getOffhand();
    }

    public static ItemStack getEquipment(EquipmentSlot equipmentSlot)
    {
        int slot = inventorySlotForEquipment(equipmentSlot);
        return slot >= 0 ? CreatorInventory.getInstance().getStack(slot) : ItemStack.EMPTY;
    }

    static boolean armUsesMainHand(HumanoidArm mainArm, HumanoidArm renderedArm)
    {
        return mainArm == renderedArm;
    }

    static int inventorySlotForEquipment(EquipmentSlot equipmentSlot)
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

    private static ItemStack stabilize(ItemStack cached, ItemStack current)
    {
        return ItemStack.matches(cached, current) ? cached : current;
    }
}
