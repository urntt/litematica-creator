package io.github.urntt.litematicacreator.creator;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class CreatorPlacementHandResolverTest
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
    void prefersPlaceableMainHand()
    {
        ItemStack mainHand = new ItemStack(Items.STONE);
        ItemStack offhand = new ItemStack(Items.DIRT);

        CreatorPlacementHandResolver.Selection selection = CreatorPlacementHandResolver.resolve(mainHand, offhand);

        assertEquals(InteractionHand.MAIN_HAND, selection.hand());
        assertSame(mainHand, selection.stack());
        assertSame(Items.STONE, selection.blockItem());
    }

    @Test
    void fallsBackToPlaceableOffhandWhenMainHandIsEmpty()
    {
        ItemStack offhand = new ItemStack(Items.DIRT);

        CreatorPlacementHandResolver.Selection selection = CreatorPlacementHandResolver.resolve(ItemStack.EMPTY, offhand);

        assertEquals(InteractionHand.OFF_HAND, selection.hand());
        assertSame(offhand, selection.stack());
        assertSame(Items.DIRT, selection.blockItem());
    }

    @Test
    void fallsBackToPlaceableOffhandWhenMainHandIsNotABlock()
    {
        CreatorPlacementHandResolver.Selection selection = CreatorPlacementHandResolver.resolve(
                new ItemStack(Items.STICK),
                new ItemStack(Items.STONE)
        );

        assertEquals(InteractionHand.OFF_HAND, selection.hand());
    }

    @Test
    void returnsNoSelectionWhenNeitherHandCanPlaceABlock()
    {
        assertNull(CreatorPlacementHandResolver.resolve(ItemStack.EMPTY, new ItemStack(Items.STICK)));
    }
}
