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

        ItemStack result = CreatorPaletteClickPolicy.applyClick(ItemStack.EMPTY, clicked, 0, false);

        assertEquals(23, result.getCount());
        assertTrue(ItemStack.isSameItemSameComponents(clicked, result));
    }

    @Test
    void matchingLeftClickAddsOneWithoutExceedingMaximum()
    {
        ItemStack carried = new ItemStack(Items.STONE, 12);

        ItemStack result = CreatorPaletteClickPolicy.applyClick(carried, new ItemStack(Items.STONE), 0, false);

        assertEquals(13, result.getCount());
        assertEquals(12, carried.getCount());

        ItemStack full = new ItemStack(Items.STONE, 64);
        assertEquals(64, CreatorPaletteClickPolicy.applyClick(full, new ItemStack(Items.STONE), 0, false).getCount());
    }

    @Test
    void matchingRightClickRemovesOneAndClearsAtZero()
    {
        assertEquals(11, CreatorPaletteClickPolicy.applyClick(
                new ItemStack(Items.STONE, 12),
                new ItemStack(Items.STONE),
                1,
                false
        ).getCount());
        assertTrue(CreatorPaletteClickPolicy.applyClick(
                new ItemStack(Items.STONE),
                new ItemStack(Items.STONE),
                1,
                false
        ).isEmpty());
    }

    @Test
    void differentItemClearsOnLeftAndShrinksCarriedOnRight()
    {
        ItemStack carried = new ItemStack(Items.STONE, 12);

        assertTrue(CreatorPaletteClickPolicy.applyClick(carried, new ItemStack(Items.DIRT), 0, false).isEmpty());
        assertEquals(11, CreatorPaletteClickPolicy.applyClick(carried, new ItemStack(Items.DIRT), 1, false).getCount());
    }

    @Test
    void emptyPaletteSlotUsesTheSameCarriedRemovalRules()
    {
        ItemStack carried = new ItemStack(Items.STONE, 2);

        assertTrue(CreatorPaletteClickPolicy.applyClick(carried, ItemStack.EMPTY, 0, false).isEmpty());
        assertEquals(1, CreatorPaletteClickPolicy.applyClick(carried, ItemStack.EMPTY, 1, false).getCount());
    }

    @Test
    void shiftClickCopiesMaximumStackToEmptyCursor()
    {
        ItemStack clicked = new ItemStack(Items.STONE);

        ItemStack result = CreatorPaletteClickPolicy.applyClick(ItemStack.EMPTY, clicked, 0, true);

        assertEquals(64, result.getCount());
        assertEquals(1, clicked.getCount());
    }

    @Test
    void shiftLeftClickFillsMatchingCursorToMaximum()
    {
        ItemStack carried = new ItemStack(Items.STONE, 12);

        ItemStack result = CreatorPaletteClickPolicy.applyClick(carried, new ItemStack(Items.STONE), 0, true);

        assertEquals(64, result.getCount());
        assertEquals(12, carried.getCount());
    }

    @Test
    void shiftClickKeepsVanillaRemovalRulesForOtherCases()
    {
        assertTrue(CreatorPaletteClickPolicy.applyClick(
                new ItemStack(Items.STONE, 12),
                new ItemStack(Items.DIRT),
                0,
                true
        ).isEmpty());
        assertEquals(11, CreatorPaletteClickPolicy.applyClick(
                new ItemStack(Items.STONE, 12),
                new ItemStack(Items.STONE),
                1,
                true
        ).getCount());
    }
}
