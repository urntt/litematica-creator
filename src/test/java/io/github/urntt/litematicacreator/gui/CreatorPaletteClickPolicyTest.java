package io.github.urntt.litematicacreator.gui;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatorPaletteClickPolicyTest
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
    void emptyCarriedCopiesDisplayedPaletteCountAndComponents()
    {
        ItemStack clicked = new ItemStack(Items.STONE, 23);
        clicked.set(DataComponents.CUSTOM_NAME, Component.literal("Saved Stone"));

        ItemStack result = CreatorPaletteClickPolicy.applyNormalClick(ItemStack.EMPTY, clicked, 0);

        assertEquals(23, result.getCount());
        assertTrue(ItemStack.isSameItemSameComponents(clicked, result));
    }

    @Test
    void matchingLeftClickAddsOneWithoutExceedingMaximum()
    {
        ItemStack carried = new ItemStack(Items.STONE, 12);

        ItemStack result = CreatorPaletteClickPolicy.applyNormalClick(carried, new ItemStack(Items.STONE), 0);

        assertEquals(13, result.getCount());
        assertEquals(12, carried.getCount());

        ItemStack full = new ItemStack(Items.STONE, 64);
        assertEquals(64, CreatorPaletteClickPolicy.applyNormalClick(full, new ItemStack(Items.STONE), 0).getCount());
    }

    @Test
    void matchingRightClickRemovesOneAndClearsAtZero()
    {
        assertEquals(11, CreatorPaletteClickPolicy.applyNormalClick(
                new ItemStack(Items.STONE, 12),
                new ItemStack(Items.STONE),
                1
        ).getCount());
        assertTrue(CreatorPaletteClickPolicy.applyNormalClick(
                new ItemStack(Items.STONE),
                new ItemStack(Items.STONE),
                1
        ).isEmpty());
    }

    @Test
    void differentItemClearsOnLeftAndShrinksCarriedOnRight()
    {
        ItemStack carried = new ItemStack(Items.STONE, 12);

        assertTrue(CreatorPaletteClickPolicy.applyNormalClick(carried, new ItemStack(Items.DIRT), 0).isEmpty());
        assertEquals(11, CreatorPaletteClickPolicy.applyNormalClick(carried, new ItemStack(Items.DIRT), 1).getCount());
    }

    @Test
    void emptyPaletteSlotUsesTheSameCarriedRemovalRules()
    {
        ItemStack carried = new ItemStack(Items.STONE, 2);

        assertTrue(CreatorPaletteClickPolicy.applyNormalClick(carried, ItemStack.EMPTY, 0).isEmpty());
        assertEquals(1, CreatorPaletteClickPolicy.applyNormalClick(carried, ItemStack.EMPTY, 1).getCount());
    }

    @Test
    void shiftTargetStartsAtSelectedAndWrapsWithoutChangingIt()
    {
        boolean[] empty = new boolean[9];
        empty[1] = true;
        empty[6] = true;
        int selected = 6;

        int target = CreatorPaletteClickPolicy.findFirstEmptyHotbarSlot(selected, slot -> empty[slot]);

        assertEquals(6, target);
        assertEquals(6, selected);
    }

    @Test
    void shiftTargetReturnsNoneWhenHotbarIsFull()
    {
        assertEquals(-1, CreatorPaletteClickPolicy.findFirstEmptyHotbarSlot(4, slot -> false));
    }
}
