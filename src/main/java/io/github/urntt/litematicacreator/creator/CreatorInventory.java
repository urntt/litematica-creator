package io.github.urntt.litematicacreator.creator;

import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
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
    public static final int ARMOR_HEAD_SLOT = ARMOR_START;
    public static final int ARMOR_CHEST_SLOT = ARMOR_START + 1;
    public static final int ARMOR_LEGS_SLOT = ARMOR_START + 2;
    public static final int ARMOR_FEET_SLOT = ARMOR_START + 3;
    public static final int DISCARD_SLOT = ARMOR_START + ARMOR_SIZE;
    public static final int SLOT_COUNT = DISCARD_SLOT + 1;

    private static final CreatorInventory INSTANCE = new CreatorInventory();
    private static final String FILE_NAME = Reference.MOD_ID + "-inventory.json";

    private final ItemStack[] stacks = new ItemStack[SLOT_COUNT];
    private final CreatorInventorySaveGate saveGate = new CreatorInventorySaveGate();
    private int selectedHotbarSlot;
    private boolean loaded;
    private boolean savePending;
    private RegistryAccess registryAccess;

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
        int normalized = Math.floorMod(slot, HOTBAR_SIZE);

        if (this.selectedHotbarSlot != normalized)
        {
            this.selectedHotbarSlot = normalized;
            this.markChanged();
        }
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

    public void swapSelectedWithOffhand()
    {
        this.runTransaction(() ->
        {
            if (CreatorInventorySlotPolicy.swapSelectedWithOffhand(this.stacks, this.selectedHotbarSlot))
            {
                this.markChanged();
            }
        });
    }

    public void setStack(int slot, ItemStack stack)
    {
        if (!this.isValidSlot(slot))
        {
            return;
        }

        ItemStack normalized = slot == DISCARD_SLOT ? ItemStack.EMPTY : normalize(stack);

        if (ItemStack.matches(this.stacks[slot], normalized))
        {
            return;
        }

        this.stacks[slot] = normalized;
        this.markChanged();
    }

    public boolean pickBlock(BlockState state)
    {
        Item item = state.getBlock().asItem();

        if (state.isAir() || item == Items.AIR)
        {
            return false;
        }

        ItemStack picked = new ItemStack(item);

        this.runTransaction(() ->
        {
            CreatorInventorySlotPolicy.PickPlan plan = CreatorInventorySlotPolicy.planPick(
                    this.stacks,
                    this.selectedHotbarSlot,
                    picked
            );

            switch (plan.action())
            {
                case SELECT -> this.setSelectedHotbarSlot(plan.targetSlot());
                case SWAP ->
                {
                    ItemStack targetStack = this.getStack(plan.targetSlot());
                    this.setStack(plan.targetSlot(), this.getStack(plan.sourceSlot()));
                    this.setStack(plan.sourceSlot(), targetStack);
                    this.setSelectedHotbarSlot(plan.targetSlot());
                }
                case INSERT ->
                {
                    if (plan.displacedSlot() >= 0)
                    {
                        this.setStack(plan.displacedSlot(), this.getStack(plan.targetSlot()));
                    }

                    this.setStack(plan.targetSlot(), picked);
                    this.setSelectedHotbarSlot(plan.targetSlot());
                }
            }
        });

        return true;
    }

    public void runTransaction(Runnable action)
    {
        this.saveGate.begin();

        try
        {
            action.run();
        }
        finally
        {
            if (this.saveGate.end())
            {
                this.save();
            }
        }
    }

    public void load(RegistryAccess registryAccess)
    {
        this.registryAccess = registryAccess;

        if (this.loaded)
        {
            if (this.savePending)
            {
                this.savePending = false;
                this.save();
            }

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

        CreatorInventoryStorage.LoadResult result = CreatorInventoryStorage.fromJson(
                element.getAsJsonObject(),
                registryAccess,
                message -> LitematicaCreator.LOGGER.warn("Creator inventory: {}", message)
        );
        this.selectedHotbarSlot = result.selectedHotbarSlot();

        for (int slot = 0; slot < SLOT_COUNT; ++slot)
        {
            this.stacks[slot] = result.stacks()[slot];
        }

        if (result.migrated())
        {
            this.save();
        }
    }

    public void save()
    {
        RegistryAccess registryAccess = this.getRegistryAccess();

        if (registryAccess == null)
        {
            this.savePending = true;
            return;
        }

        Path file = this.getFile();
        Path dir = file.getParent();

        if (!Files.exists(dir))
        {
            FileUtils.createDirectoriesIfMissing(dir);
        }

        JsonObject root = CreatorInventoryStorage.toJson(
                this.stacks,
                this.selectedHotbarSlot,
                registryAccess,
                message -> LitematicaCreator.LOGGER.warn("Creator inventory: {}", message)
        );
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");

        if (!JsonUtils.writeJsonToFile(root, temp))
        {
            LitematicaCreator.LOGGER.error("Failed to write temporary Creator inventory config {}", temp);
            return;
        }

        try
        {
            try
            {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            catch (AtomicMoveNotSupportedException ignored)
            {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (Exception exception)
        {
            LitematicaCreator.LOGGER.error("Failed to commit Creator inventory config {}", file, exception);
        }
    }

    private void markChanged()
    {
        if (this.saveGate.markChanged())
        {
            this.save();
        }
    }

    private RegistryAccess getRegistryAccess()
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level != null)
        {
            this.registryAccess = mc.level.registryAccess();
        }

        return this.registryAccess;
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
