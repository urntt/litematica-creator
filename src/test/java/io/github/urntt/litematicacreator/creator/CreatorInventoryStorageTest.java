package io.github.urntt.litematicacreator.creator;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DebugStickState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatorInventoryStorageTest
{
    private static RegistryAccess registryAccess;

    @BeforeAll
    static void bootstrapMinecraft()
    {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        registryAccess = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
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
    void roundTripsCompleteItemStacks()
    {
        ItemStack[] stacks = emptyStacks();
        ItemStack namedStack = new ItemStack(Items.STONE, 37);
        namedStack.set(DataComponents.CUSTOM_NAME, Component.literal("Creator Stone"));
        stacks[4] = namedStack;
        List<String> errors = new ArrayList<>();

        JsonObject json = CreatorInventoryStorage.toJson(stacks, 4, registryAccess, errors::add);
        CreatorInventoryStorage.LoadResult result = CreatorInventoryStorage.fromJson(json, registryAccess, errors::add);

        assertTrue(errors.isEmpty());
        assertEquals(CreatorInventoryStorage.FORMAT_VERSION, json.get("formatVersion").getAsInt());
        assertEquals(4, result.selectedHotbarSlot());
        assertEquals(37, result.stacks()[4].getCount());
        assertTrue(ItemStack.isSameItemSameComponents(namedStack, result.stacks()[4]));
        assertFalse(result.migrated());
    }

    @Test
    void keepsTheSelectedDebugStickProperty()
    {
        ItemStack[] stacks = emptyStacks();
        ItemStack debugStick = new ItemStack(Items.DEBUG_STICK);
        debugStick.set(DataComponents.DEBUG_STICK_STATE,
                DebugStickState.EMPTY.withProperty(Blocks.OAK_STAIRS.builtInRegistryHolder(), StairBlock.HALF));
        stacks[CreatorInventory.OFFHAND_SLOT] = debugStick;
        List<String> errors = new ArrayList<>();

        JsonObject json = CreatorInventoryStorage.toJson(stacks, 0, registryAccess, errors::add);
        CreatorInventoryStorage.LoadResult result = CreatorInventoryStorage.fromJson(json, registryAccess, errors::add);

        assertTrue(errors.isEmpty());
        assertEquals(StairBlock.HALF, result.stacks()[CreatorInventory.OFFHAND_SLOT]
                .get(DataComponents.DEBUG_STICK_STATE)
                .properties()
                .get(Blocks.OAK_STAIRS.builtInRegistryHolder()));
    }

    @Test
    void migratesLegacyIdsToMaximumStackSize()
    {
        JsonObject root = new JsonObject();
        JsonArray slots = new JsonArray();
        JsonObject slot = new JsonObject();
        slot.addProperty("slot", 2);
        slot.addProperty("item", "minecraft:stone");
        slots.add(slot);
        root.addProperty("selectedHotbarSlot", 2);
        root.add("slots", slots);

        CreatorInventoryStorage.LoadResult result = CreatorInventoryStorage.fromJson(root, registryAccess, ignored -> { });

        assertTrue(result.migrated());
        assertTrue(result.stacks()[2].is(Items.STONE));
        assertEquals(result.stacks()[2].getMaxStackSize(), result.stacks()[2].getCount());
    }

    @Test
    void skipsOnlyMalformedSlots()
    {
        JsonObject root = new JsonObject();
        JsonArray slots = new JsonArray();
        JsonObject invalid = new JsonObject();
        invalid.addProperty("slot", 500);
        invalid.addProperty("stack", "not-a-stack");
        slots.add(invalid);
        root.addProperty("formatVersion", CreatorInventoryStorage.FORMAT_VERSION);
        root.add("slots", slots);
        List<String> errors = new ArrayList<>();

        CreatorInventoryStorage.LoadResult result = CreatorInventoryStorage.fromJson(root, registryAccess, errors::add);

        assertTrue(result.stacks()[0].isEmpty());
        assertFalse(errors.isEmpty());
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
}
