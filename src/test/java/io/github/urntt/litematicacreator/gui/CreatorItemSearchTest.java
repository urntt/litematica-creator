package io.github.urntt.litematicacreator.gui;

import java.util.List;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatorItemSearchTest
{
    @BeforeAll
    static void bootstrapMinecraft()
    {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        DataComponentMap testDefaults = DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build();
        BuiltInRegistries.ITEM.listElements().forEach(holder ->
        {
            if (!holder.areComponentsBound())
            {
                holder.bindComponents(testDefaults);
            }
        });
    }

    @Test
    void mergesCreativeVariantsWithOtherwiseMissingRegisteredItems()
    {
        ItemStack creativeStone = new ItemStack(Items.STONE);
        CreatorItemSearch search = new CreatorItemSearch(
                List.of(creativeStone),
                List.of(new ItemStack(Items.STONE, 64), new ItemStack(Items.DIRT)),
                stack -> List.of(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.is(Items.STONE) ? "Localized Stone" : "Soil Tooltip")
        );

        assertEquals(2, search.allItems().size());
        assertEquals(List.of(creativeStone), search.search("localized stone"));
        assertTrue(search.search("minecraft:dirt").getFirst().is(Items.DIRT));
        assertTrue(search.search("soil tooltip").getFirst().is(Items.DIRT));
    }

    @Test
    void matchesNamespacedAndPathOnlyTagQueries()
    {
        Identifier id = Identifier.parse("example:building/stone_blocks");

        assertTrue(CreatorItemSearch.matchesIdentifier(id, "stone"));
        assertTrue(CreatorItemSearch.matchesIdentifier(id, "example:building"));
    }
}
