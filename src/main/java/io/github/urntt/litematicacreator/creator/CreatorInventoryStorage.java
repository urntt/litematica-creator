package io.github.urntt.litematicacreator.creator;

import java.util.Arrays;
import java.util.function.Consumer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class CreatorInventoryStorage
{
    static final int FORMAT_VERSION = 2;

    private CreatorInventoryStorage()
    {
    }

    static JsonObject toJson(ItemStack[] stacks, int selectedHotbarSlot, RegistryAccess registryAccess, Consumer<String> errorConsumer)
    {
        RegistryOps<JsonElement> ops = registryAccess.createSerializationContext(JsonOps.INSTANCE);
        JsonObject root = new JsonObject();
        JsonArray slots = new JsonArray();
        root.addProperty("formatVersion", FORMAT_VERSION);
        root.addProperty("selectedHotbarSlot", Math.floorMod(selectedHotbarSlot, CreatorInventory.HOTBAR_SIZE));

        for (int slot = 0; slot < Math.min(stacks.length, CreatorInventory.DISCARD_SLOT); ++slot)
        {
            int slotIndex = slot;
            ItemStack stack = stacks[slot];

            if (stack == null || stack.isEmpty())
            {
                continue;
            }

            JsonElement encoded = ItemStack.CODEC.encodeStart(ops, stack)
                                                   .resultOrPartial(message -> errorConsumer.accept("slot " + slotIndex + ": " + message))
                                                   .orElse(null);

            if (encoded != null)
            {
                JsonObject slotObject = new JsonObject();
                slotObject.addProperty("slot", slot);
                slotObject.add("stack", encoded);
                slots.add(slotObject);
            }
        }

        root.add("slots", slots);
        return root;
    }

    static LoadResult fromJson(JsonObject root, RegistryAccess registryAccess, Consumer<String> errorConsumer)
    {
        ItemStack[] stacks = emptyStacks();
        int selectedHotbarSlot = Math.floorMod(getInteger(root, "selectedHotbarSlot", 0), CreatorInventory.HOTBAR_SIZE);
        int version = getInteger(root, "formatVersion", 1);
        JsonArray slots = root.has("slots") && root.get("slots").isJsonArray() ? root.getAsJsonArray("slots") : new JsonArray();

        if (version == 1)
        {
            loadVersionOne(slots, stacks, errorConsumer);
            return new LoadResult(stacks, selectedHotbarSlot, true);
        }

        if (version != FORMAT_VERSION)
        {
            errorConsumer.accept("unsupported format version " + version);
            return new LoadResult(stacks, selectedHotbarSlot, false);
        }

        RegistryOps<JsonElement> ops = registryAccess.createSerializationContext(JsonOps.INSTANCE);

        for (JsonElement slotElement : slots)
        {
            if (!slotElement.isJsonObject())
            {
                errorConsumer.accept("ignored a non-object slot entry");
                continue;
            }

            JsonObject slotObject = slotElement.getAsJsonObject();
            int slot = getInteger(slotObject, "slot", -1);

            if (!isPersistentSlot(slot) || !slotObject.has("stack"))
            {
                errorConsumer.accept("ignored invalid slot " + slot);
                continue;
            }

            ItemStack stack = ItemStack.CODEC.parse(ops, slotObject.get("stack"))
                                             .resultOrPartial(message -> errorConsumer.accept("slot " + slot + ": " + message))
                                             .orElse(ItemStack.EMPTY);
            stacks[slot] = normalize(stack);
        }

        return new LoadResult(stacks, selectedHotbarSlot, false);
    }

    private static void loadVersionOne(JsonArray slots, ItemStack[] stacks, Consumer<String> errorConsumer)
    {
        for (JsonElement slotElement : slots)
        {
            if (!slotElement.isJsonObject())
            {
                errorConsumer.accept("ignored a non-object legacy slot entry");
                continue;
            }

            JsonObject slotObject = slotElement.getAsJsonObject();
            int slot = getInteger(slotObject, "slot", -1);
            String idString = getString(slotObject, "item");

            if (!isPersistentSlot(slot) || idString == null)
            {
                errorConsumer.accept("ignored invalid legacy slot " + slot);
                continue;
            }

            Identifier id = Identifier.tryParse(idString);
            Item item = id != null ? BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR) : Items.AIR;

            if (item == Items.AIR)
            {
                errorConsumer.accept("ignored missing legacy item " + idString);
                continue;
            }

            ItemStack stack = new ItemStack(item);
            stack.setCount(stack.getMaxStackSize());
            stacks[slot] = stack;
        }
    }

    private static ItemStack normalize(ItemStack stack)
    {
        if (stack == null || stack.isEmpty())
        {
            return ItemStack.EMPTY;
        }

        ItemStack copy = stack.copy();
        copy.limitSize(copy.getMaxStackSize());
        return copy;
    }

    private static ItemStack[] emptyStacks()
    {
        ItemStack[] stacks = new ItemStack[CreatorInventory.SLOT_COUNT];
        Arrays.fill(stacks, ItemStack.EMPTY);
        return stacks;
    }

    private static boolean isPersistentSlot(int slot)
    {
        return slot >= 0 && slot < CreatorInventory.DISCARD_SLOT;
    }

    private static int getInteger(JsonObject object, String key, int fallback)
    {
        try
        {
            return object.has(key) ? object.get(key).getAsInt() : fallback;
        }
        catch (RuntimeException ignored)
        {
            return fallback;
        }
    }

    private static String getString(JsonObject object, String key)
    {
        try
        {
            return object.has(key) ? object.get(key).getAsString() : null;
        }
        catch (RuntimeException ignored)
        {
            return null;
        }
    }

    record LoadResult(ItemStack[] stacks, int selectedHotbarSlot, boolean migrated)
    {
    }
}
