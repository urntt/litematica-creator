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
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.util.InfoUtils;
import io.github.urntt.litematicacreator.creator.CreatorInventory;

public class GuiCreatorInventory extends Screen
{
    private static final int PAGE_SIZE = 16;

    private final List<Item> allBlockItems = new ArrayList<>();
    private final List<Item> filteredBlockItems = new ArrayList<>();
    private EditBox searchBox;
    private int page;

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

        int panelWidth = Math.min(420, this.width - 40);
        int x = (this.width - panelWidth) / 2;
        int y = 24;

        this.searchBox = new EditBox(this.font, x, y, panelWidth, 20, Component.translatable("litematica-creator.gui.label.search"));
        this.searchBox.setMaxLength(80);
        this.searchBox.setValue(value);
        this.searchBox.setResponder(this::applyFilter);
        this.addRenderableWidget(this.searchBox);
        this.setInitialFocus(this.searchBox);

        int buttonWidth = (panelWidth - 6) / 2;
        int buttonY = y + 28;
        int start = this.page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, this.filteredBlockItems.size());

        for (int i = start; i < end; ++i)
        {
            Item item = this.filteredBlockItems.get(i);
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            int local = i - start;
            int bx = x + (local % 2) * (buttonWidth + 6);
            int by = buttonY + (local / 2) * 22;

            this.addRenderableWidget(Button.builder(Component.literal(id.toString()), button -> this.selectItem(item))
                                           .bounds(bx, by, buttonWidth, 20)
                                           .build());
        }

        int navY = this.height - 30;
        this.addRenderableWidget(Button.builder(Component.literal("<"), button -> this.changePage(-1))
                                       .bounds(x, navY, 40, 20)
                                       .build());
        this.addRenderableWidget(Button.builder(this.getPageLabel(), button -> { })
                                       .bounds(x + 46, navY, panelWidth - 92, 20)
                                       .build()).active = false;
        this.addRenderableWidget(Button.builder(Component.literal(">"), button -> this.changePage(1))
                                       .bounds(x + panelWidth - 40, navY, 40, 20)
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
        int pages = Math.max(1, (this.filteredBlockItems.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        this.page = Math.floorMod(this.page + amount, pages);
        this.rebuildWidgetsForFilter();
    }

    private Component getPageLabel()
    {
        int pages = Math.max(1, (this.filteredBlockItems.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        return Component.translatable("litematica-creator.gui.label.creator_inventory_page", this.page + 1, pages);
    }

    private void selectItem(Item item)
    {
        CreatorInventory.getInstance().setStack(CreatorInventory.getInstance().getSelectedHotbarSlot(), new ItemStack(item));
        InfoUtils.showGuiOrInGameMessage(MessageType.SUCCESS, "litematica-creator.message.inventory.selected", BuiltInRegistries.ITEM.getKey(item).toString());
        GuiBase.openGui(null);
    }
}
