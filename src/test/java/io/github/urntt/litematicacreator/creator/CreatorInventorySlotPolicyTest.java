package io.github.urntt.litematicacreator.creator;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatorInventorySlotPolicyTest
{
    @BeforeAll
    static void bootstrapMinecraft()
    {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        DataComponentMap defaults = DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build();
        BuiltInRegistries.ITEM.listElements().forEach(holder ->
        {
            if (!holder.areComponentsBound())
            {
                holder.bindComponents(defaults);
            }
        });
    }

    @Test
    void selectsMatchingHotbarStackWithoutReplacingIt()
    {
        ItemStack[] stacks = emptyStacks();
        stacks[6] = new ItemStack(Items.STONE, 37);

        CreatorInventorySlotPolicy.PickPlan plan = CreatorInventorySlotPolicy.planPick(
                stacks,
                2,
                new ItemStack(Items.STONE)
        );

        assertEquals(CreatorInventorySlotPolicy.PickAction.SELECT, plan.action());
        assertEquals(6, plan.targetSlot());
    }

    @Test
    void swapsMatchingMainStackIntoSuitableHotbarSlot()
    {
        ItemStack[] stacks = emptyStacks();
        stacks[2] = new ItemStack(Items.DIRT, 12);
        stacks[14] = new ItemStack(Items.STONE, 37);

        CreatorInventorySlotPolicy.PickPlan plan = CreatorInventorySlotPolicy.planPick(
                stacks,
                2,
                new ItemStack(Items.STONE)
        );

        assertEquals(CreatorInventorySlotPolicy.PickAction.SWAP, plan.action());
        assertEquals(14, plan.sourceSlot());
        assertEquals(3, plan.targetSlot());
    }

    @Test
    void insertsNewStackAndSpillsOccupiedTargetIntoMainInventory()
    {
        ItemStack[] stacks = fullHotbar();
        stacks[CreatorInventory.HOTBAR_SIZE] = ItemStack.EMPTY;

        CreatorInventorySlotPolicy.PickPlan plan = CreatorInventorySlotPolicy.planPick(
                stacks,
                4,
                new ItemStack(Items.DIAMOND)
        );

        assertEquals(CreatorInventorySlotPolicy.PickAction.INSERT, plan.action());
        assertEquals(4, plan.targetSlot());
        assertEquals(CreatorInventory.HOTBAR_SIZE, plan.displacedSlot());
    }

    @Test
    void overwritesSuitableSlotWhenMainInventoryIsAlsoFull()
    {
        ItemStack[] stacks = fullStorage();

        CreatorInventorySlotPolicy.PickPlan plan = CreatorInventorySlotPolicy.planPick(
                stacks,
                7,
                new ItemStack(Items.DIAMOND)
        );

        assertEquals(CreatorInventorySlotPolicy.PickAction.INSERT, plan.action());
        assertEquals(7, plan.targetSlot());
        assertEquals(-1, plan.displacedSlot());
    }

    @Test
    void componentMismatchDoesNotCountAsAnExistingStack()
    {
        ItemStack[] stacks = emptyStacks();
        ItemStack named = new ItemStack(Items.STONE);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Named"));
        stacks[0] = named;

        CreatorInventorySlotPolicy.PickPlan plan = CreatorInventorySlotPolicy.planPick(
                stacks,
                0,
                new ItemStack(Items.STONE)
        );

        assertEquals(CreatorInventorySlotPolicy.PickAction.INSERT, plan.action());
        assertEquals(1, plan.targetSlot());
    }

    @Test
    void suitableSlotStartsAtSelectedAndWrapsForEmptySlots()
    {
        ItemStack[] stacks = fullHotbar();
        stacks[1] = ItemStack.EMPTY;

        assertEquals(1, CreatorInventorySlotPolicy.findSuitableHotbarSlot(stacks, 7));
    }

    @Test
    void suitableSlotFallsBackToFirstUnenchantedStackThenSelected()
    {
        ItemStack[] stacks = fullHotbar();
        enchantHotbar(stacks);
        stacks[2] = new ItemStack(Items.DIRT);

        assertEquals(2, CreatorInventorySlotPolicy.findSuitableHotbarSlot(stacks, 8));

        enchantHotbar(stacks);
        assertEquals(8, CreatorInventorySlotPolicy.findSuitableHotbarSlot(stacks, 8));
    }

    @Test
    void emptyHotbarSearchStartsAtSelectedWithoutChangingSelection()
    {
        ItemStack[] stacks = fullHotbar();
        stacks[1] = ItemStack.EMPTY;
        stacks[6] = ItemStack.EMPTY;

        assertEquals(6, CreatorInventorySlotPolicy.findFirstEmptyHotbarSlot(stacks, 6));
        assertEquals(-1, CreatorInventorySlotPolicy.findFirstEmptyHotbarSlot(fullHotbar(), 6));
    }

    @Test
    void swapsSelectedHotbarSlotWithOffhand()
    {
        ItemStack[] stacks = emptyStacks();
        ItemStack hotbarStack = new ItemStack(Items.STONE, 12);
        ItemStack offhandStack = new ItemStack(Items.DIRT, 37);
        stacks[4] = hotbarStack;
        stacks[CreatorInventory.OFFHAND_SLOT] = offhandStack;

        assertTrue(CreatorInventorySlotPolicy.swapSelectedWithOffhand(stacks, 13));
        assertSame(offhandStack, stacks[4]);
        assertSame(hotbarStack, stacks[CreatorInventory.OFFHAND_SLOT]);
    }

    @Test
    void identicalHandsDoNotCreateAChange()
    {
        ItemStack[] stacks = emptyStacks();

        assertFalse(CreatorInventorySlotPolicy.swapSelectedWithOffhand(stacks, 0));
    }

    private static ItemStack[] emptyStacks()
    {
        ItemStack[] stacks = new ItemStack[CreatorInventory.SLOT_COUNT];

        for (int slot = 0; slot < stacks.length; ++slot)
        {
            stacks[slot] = ItemStack.EMPTY;
        }

        return stacks;
    }

    private static ItemStack[] fullHotbar()
    {
        ItemStack[] stacks = emptyStacks();

        for (int slot = 0; slot < CreatorInventory.HOTBAR_SIZE; ++slot)
        {
            stacks[slot] = new ItemStack(Items.DIRT, slot + 1);
        }

        return stacks;
    }

    private static ItemStack[] fullStorage()
    {
        ItemStack[] stacks = emptyStacks();

        for (int slot = 0; slot < CreatorInventory.OFFHAND_SLOT; ++slot)
        {
            stacks[slot] = new ItemStack(Items.DIRT, 1);
        }

        return stacks;
    }

    private static void enchantHotbar(ItemStack[] stacks)
    {
        Enchantment enchantment = Enchantment.enchantment(Enchantment.definition(
                HolderSet.empty(),
                1,
                1,
                new Enchantment.Cost(1, 0),
                new Enchantment.Cost(1, 0),
                1,
                EquipmentSlotGroup.ANY
        )).build(Identifier.fromNamespaceAndPath("litematica_creator_test", "slot_policy"));
        Holder<Enchantment> holder = Holder.direct(enchantment);

        for (int slot = 0; slot < CreatorInventory.HOTBAR_SIZE; ++slot)
        {
            ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            enchantments.set(holder, 1);
            stacks[slot].set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
        }
    }
}
