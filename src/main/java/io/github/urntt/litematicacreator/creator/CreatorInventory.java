package io.github.urntt.litematicacreator.creator;

import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.data.json.JsonUtils;
import io.github.urntt.litematicacreator.LitematicaCreator;
import io.github.urntt.litematicacreator.Reference;

public class CreatorInventory
{
    public static final int HOTBAR_SIZE = 9;
    public static final int MAIN_SIZE = 27;
    public static final int OFFHAND_SLOT = HOTBAR_SIZE + MAIN_SIZE;
    public static final int ARMOR_START = OFFHAND_SLOT + 1;
    public static final int ARMOR_SIZE = 4;
    public static final int DISCARD_SLOT = ARMOR_START + ARMOR_SIZE;
    public static final int SLOT_COUNT = DISCARD_SLOT + 1;

    private static final CreatorInventory INSTANCE = new CreatorInventory();
    private static final String FILE_NAME = Reference.MOD_ID + "-inventory.json";

    private final ItemStack[] stacks = new ItemStack[SLOT_COUNT];
    private int selectedHotbarSlot;
    private boolean loaded;

    private CreatorInventory()
    {
        for (int i = 0; i < this.stacks.length; ++i)
        {
            this.stacks[i] = ItemStack.EMPTY;
        }
    }

    public static CreatorInventory getInstance()
    {
        return INSTANCE;
    }

    public int getSelectedHotbarSlot()
    {
        return this.selectedHotbarSlot;
    }

    public void setSelectedHotbarSlot(int slot)
    {
        this.selectedHotbarSlot = Math.floorMod(slot, HOTBAR_SIZE);
        this.save();
    }

    public void scrollHotbar(double amount)
    {
        int direction = amount > 0 ? -1 : 1;
        this.setSelectedHotbarSlot(this.selectedHotbarSlot + direction);
    }

    public ItemStack getSelectedStack()
    {
        return this.getStack(this.selectedHotbarSlot);
    }

    public ItemStack getStack(int slot)
    {
        return this.isValidSlot(slot) ? this.stacks[slot].copy() : ItemStack.EMPTY;
    }

    public void setStack(int slot, ItemStack stack)
    {
        if (!this.isValidSlot(slot))
        {
            return;
        }

        if (slot == DISCARD_SLOT)
        {
            this.stacks[slot] = ItemStack.EMPTY;
        }
        else
        {
            this.stacks[slot] = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        }

        this.save();
    }

    public boolean pickBlock(BlockState state)
    {
        Item item = state.getBlock().asItem();

        if (state.isAir() || item == Items.AIR)
        {
            return false;
        }

        this.setStack(this.selectedHotbarSlot, new ItemStack(item));
        return true;
    }

    public void load()
    {
        if (this.loaded)
        {
            return;
        }

        this.loaded = true;
        Path file = this.getFile();

        if (!Files.exists(file) || !Files.isReadable(file))
        {
            return;
        }

        JsonElement element = JsonUtils.parseJsonFile(file);

        if (element == null || !element.isJsonObject())
        {
            return;
        }

        JsonObject root = element.getAsJsonObject();
        this.selectedHotbarSlot = Math.floorMod(JsonUtils.getInteger(root, "selectedHotbarSlot"), HOTBAR_SIZE);
        JsonArray slots = root.has("slots") && root.get("slots").isJsonArray() ? root.getAsJsonArray("slots") : null;

        if (slots == null)
        {
            return;
        }

        for (JsonElement slotElement : slots)
        {
            if (!slotElement.isJsonObject())
            {
                continue;
            }

            JsonObject slotObject = slotElement.getAsJsonObject();
            int slot = JsonUtils.getInteger(slotObject, "slot");
            String idString = JsonUtils.getString(slotObject, "item");

            if (!this.isValidSlot(slot) || idString == null)
            {
                continue;
            }

            Identifier id = Identifier.tryParse(idString);
            Item item = id != null ? BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR) : Items.AIR;
            this.stacks[slot] = item != Items.AIR ? new ItemStack(item) : ItemStack.EMPTY;
        }
    }

    public void save()
    {
        Path file = this.getFile();
        Path dir = file.getParent();

        if (!Files.exists(dir))
        {
            FileUtils.createDirectoriesIfMissing(dir);
        }

        JsonObject root = new JsonObject();
        JsonArray slots = new JsonArray();
        root.addProperty("selectedHotbarSlot", this.selectedHotbarSlot);

        for (int i = 0; i < this.stacks.length; ++i)
        {
            ItemStack stack = this.stacks[i];

            if (!stack.isEmpty())
            {
                JsonObject slotObject = new JsonObject();
                slotObject.addProperty("slot", i);
                slotObject.addProperty("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
                slots.add(slotObject);
            }
        }

        root.add("slots", slots);
        JsonUtils.writeJsonToFile(root, file);
    }

    private Path getFile()
    {
        return FileUtils.getConfigDirectory().resolve(FILE_NAME);
    }

    private boolean isValidSlot(int slot)
    {
        boolean valid = slot >= 0 && slot < SLOT_COUNT;

        if (!valid)
        {
            LitematicaCreator.LOGGER.warn("Ignoring invalid Creator inventory slot {}", slot);
        }

        return valid;
    }
}
