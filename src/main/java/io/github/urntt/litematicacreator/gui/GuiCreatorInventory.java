package io.github.urntt.litematicacreator.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import io.github.urntt.litematicacreator.creator.CreatorInventory;

public class GuiCreatorInventory extends Screen
{
    private static final int DEFAULT_PAGE_SIZE = 16;
    private static final int SLOT_COLUMNS = 3;

    private final List<Item> allBlockItems = new ArrayList<>();
    private final List<Item> filteredBlockItems = new ArrayList<>();
    private EditBox searchBox;
    private int page;
    private int pageSize = DEFAULT_PAGE_SIZE;
    private int targetSlot = CreatorInventory.getInstance().getSelectedHotbarSlot();

    public GuiCreatorInventory()
    {
        super(Component.translatable("litematica-creator.gui.title.creator_inventory"));
        BuiltInRegistries.ITEM.stream()
                              .filter(item -> item instanceof BlockItem && item != Items.AIR)
                              .sorted(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()))
                              .forEach(this.allBlockItems::add);
        this.filteredBlockItems.addAll(this.allBlockItems);
    }

    @Override
    protected void init()
    {
        this.rebuildWidgetsForFilter();
    }

    private void rebuildWidgetsForFilter()
    {
        String value = this.searchBox != null ? this.searchBox.getValue() : "";
        this.clearWidgets();

        int totalWidth = Math.min(760, this.width - 24);
        int inventoryWidth = Math.min(330, totalWidth / 2);
        int listWidth = totalWidth - inventoryWidth - 12;
        int x = (this.width - totalWidth) / 2;
        int listX = x + inventoryWidth + 12;
        int y = 20;

        this.addSlotWidgets(x, y, inventoryWidth);

        this.searchBox = new EditBox(this.font, listX, y, listWidth, 20, Component.translatable("litematica-creator.gui.label.search"));
        this.searchBox.setMaxLength(80);
        this.searchBox.setValue(value);
        this.searchBox.setResponder(this::applyFilter);
        this.addRenderableWidget(this.searchBox);
        this.setInitialFocus(this.searchBox);

        int buttonWidth = (listWidth - 6) / 2;
        int buttonY = y + 28;
        int rows = Math.max(4, (this.height - buttonY - 36) / 22);
        this.pageSize = rows * 2;
        this.clampPage();
        int start = this.page * this.pageSize;
        int end = Math.min(start + this.pageSize, this.filteredBlockItems.size());

        for (int i = start; i < end; ++i)
        {
            Item item = this.filteredBlockItems.get(i);
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            int local = i - start;
            int bx = listX + (local % 2) * (buttonWidth + 6);
            int by = buttonY + (local / 2) * 22;

            this.addRenderableWidget(Button.builder(Component.literal(this.abbreviate(id.toString(), 34)), button -> this.selectItem(item))
                                           .bounds(bx, by, buttonWidth, 20)
                                           .build());
        }

        int navY = this.height - 30;
        this.addRenderableWidget(Button.builder(Component.literal("<"), button -> this.changePage(-1))
                                       .bounds(listX, navY, 40, 20)
                                       .build());
        this.addRenderableWidget(Button.builder(this.getPageLabel(), button -> { })
                                       .bounds(listX + 46, navY, listWidth - 92, 20)
                                       .build()).active = false;
        this.addRenderableWidget(Button.builder(Component.literal(">"), button -> this.changePage(1))
                                       .bounds(listX + listWidth - 40, navY, 40, 20)
                                       .build());
    }

    private void addSlotWidgets(int x, int y, int width)
    {
        int buttonWidth = (width - (SLOT_COLUMNS - 1) * 4) / SLOT_COLUMNS;

        for (int slot = 0; slot < CreatorInventory.SLOT_COUNT; ++slot)
        {
            final int slotIndex = slot;
            int row = slot / SLOT_COLUMNS;
            int column = slot % SLOT_COLUMNS;
            int bx = x + column * (buttonWidth + 4);
            int by = y + row * 22;

            this.addRenderableWidget(Button.builder(this.getSlotLabel(slotIndex), button -> this.selectSlot(slotIndex))
                                           .bounds(bx, by, buttonWidth, 20)
                                           .build());
        }

        int controlsY = y + ((CreatorInventory.SLOT_COUNT + SLOT_COLUMNS - 1) / SLOT_COLUMNS) * 22 + 6;
        this.addRenderableWidget(Button.builder(Component.translatable("litematica-creator.gui.button.clear_slot"), button -> this.clearTargetSlot())
                                       .bounds(x, controlsY, (width - 6) / 2, 20)
                                       .build());
        this.addRenderableWidget(Button.builder(Component.translatable("litematica-creator.gui.button.done"), button -> this.onClose())
                                       .bounds(x + (width + 6) / 2, controlsY, (width - 6) / 2, 20)
                                       .build());
    }

    private void applyFilter(String value)
    {
        String query = value.toLowerCase(Locale.ROOT).trim();
        this.filteredBlockItems.clear();

        for (Item item : this.allBlockItems)
        {
            String id = BuiltInRegistries.ITEM.getKey(item).toString();

            if (query.isEmpty() || id.contains(query))
            {
                this.filteredBlockItems.add(item);
            }
        }

        this.page = 0;
        this.rebuildWidgetsForFilter();
    }

    private void changePage(int amount)
    {
        int pages = Math.max(1, (this.filteredBlockItems.size() + this.pageSize - 1) / this.pageSize);
        this.page = Math.floorMod(this.page + amount, pages);
        this.rebuildWidgetsForFilter();
    }

    private Component getPageLabel()
    {
        int pages = Math.max(1, (this.filteredBlockItems.size() + this.pageSize - 1) / this.pageSize);
        return Component.translatable("litematica-creator.gui.label.creator_inventory_page", this.page + 1, pages);
    }

    private void selectItem(Item item)
    {
        CreatorInventory inventory = CreatorInventory.getInstance();
        inventory.setStack(this.targetSlot, new ItemStack(item));

        if (this.targetSlot < CreatorInventory.HOTBAR_SIZE)
        {
            inventory.setSelectedHotbarSlot(this.targetSlot);
        }

        InfoUtils.showGuiOrInGameMessage(MessageType.SUCCESS, "litematica-creator.message.inventory.selected", BuiltInRegistries.ITEM.getKey(item).toString());
        this.rebuildWidgetsForFilter();
    }

    private void selectSlot(int slot)
    {
        if (slot == CreatorInventory.DISCARD_SLOT)
        {
            this.clearTargetSlot();
            return;
        }

        this.targetSlot = slot;

        if (slot < CreatorInventory.HOTBAR_SIZE)
        {
            CreatorInventory.getInstance().setSelectedHotbarSlot(slot);
        }

        this.rebuildWidgetsForFilter();
    }

    private void clearTargetSlot()
    {
        CreatorInventory.getInstance().setStack(this.targetSlot, ItemStack.EMPTY);
        this.rebuildWidgetsForFilter();
    }

    private void clampPage()
    {
        int pages = Math.max(1, (this.filteredBlockItems.size() + this.pageSize - 1) / this.pageSize);

        if (this.page >= pages)
        {
            this.page = pages - 1;
        }
    }

    private Component getSlotLabel(int slot)
    {
        CreatorInventory inventory = CreatorInventory.getInstance();
        ItemStack stack = inventory.getStack(slot);
        String marker = slot == this.targetSlot ? "> " : "";
        String selected = slot == inventory.getSelectedHotbarSlot() && slot < CreatorInventory.HOTBAR_SIZE ? "* " : "";
        String itemName = stack.isEmpty() ? Component.translatable("litematica-creator.gui.slot.empty").getString() : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();

        return Component.literal(marker + selected + this.getSlotName(slot) + ": " + this.abbreviate(itemName, 18));
    }

    private String getSlotName(int slot)
    {
        if (slot < CreatorInventory.HOTBAR_SIZE)
        {
            return Component.translatable("litematica-creator.gui.slot.hotbar", slot + 1).getString();
        }
        else if (slot < CreatorInventory.OFFHAND_SLOT)
        {
            return Component.translatable("litematica-creator.gui.slot.main", slot - CreatorInventory.HOTBAR_SIZE + 1).getString();
        }
        else if (slot == CreatorInventory.OFFHAND_SLOT)
        {
            return Component.translatable("litematica-creator.gui.slot.offhand").getString();
        }
        else if (slot < CreatorInventory.DISCARD_SLOT)
        {
            return Component.translatable("litematica-creator.gui.slot.armor", slot - CreatorInventory.ARMOR_START + 1).getString();
        }

        return Component.translatable("litematica-creator.gui.slot.discard").getString();
    }

    private String abbreviate(String value, int maxLength)
    {
        int keep = Math.max(1, maxLength - 3);
        return value.length() <= maxLength ? value : value.substring(0, keep) + "...";
    }
}
