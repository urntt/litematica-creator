package io.github.urntt.litematicacreator.render;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import org.junit.jupiter.api.Test;

import io.github.urntt.litematicacreator.creator.CreatorInventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatorVirtualLoadoutTest
{
    @Test
    void mapsRenderedArmsAgainstThePlayersMainArm()
    {
        assertTrue(CreatorVirtualLoadout.armUsesMainHand(HumanoidArm.RIGHT, HumanoidArm.RIGHT));
        assertTrue(CreatorVirtualLoadout.armUsesMainHand(HumanoidArm.LEFT, HumanoidArm.LEFT));
        assertFalse(CreatorVirtualLoadout.armUsesMainHand(HumanoidArm.RIGHT, HumanoidArm.LEFT));
        assertFalse(CreatorVirtualLoadout.armUsesMainHand(HumanoidArm.LEFT, HumanoidArm.RIGHT));
    }

    @Test
    void mapsAllHumanoidArmorSlots()
    {
        assertEquals(CreatorInventory.ARMOR_HEAD_SLOT, CreatorVirtualLoadout.inventorySlotForEquipment(EquipmentSlot.HEAD));
        assertEquals(CreatorInventory.ARMOR_CHEST_SLOT, CreatorVirtualLoadout.inventorySlotForEquipment(EquipmentSlot.CHEST));
        assertEquals(CreatorInventory.ARMOR_LEGS_SLOT, CreatorVirtualLoadout.inventorySlotForEquipment(EquipmentSlot.LEGS));
        assertEquals(CreatorInventory.ARMOR_FEET_SLOT, CreatorVirtualLoadout.inventorySlotForEquipment(EquipmentSlot.FEET));
        assertEquals(-1, CreatorVirtualLoadout.inventorySlotForEquipment(EquipmentSlot.MAINHAND));
    }
}
