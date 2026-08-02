package io.github.urntt.litematicacreator.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.client.HotbarManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.inventory.Hotbar;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import org.jspecify.annotations.Nullable;

import fi.dy.masa.malilib.gui.GuiBase;
import io.github.urntt.litematicacreator.creator.CreatorInventory;

public class GuiCreatorInventory extends AbstractContainerScreen<CreatorInventoryMenu>
{
    private static final Identifier SCROLLER_SPRITE = Identifier.withDefaultNamespace("container/creative_inventory/scroller");
    private static final Identifier SCROLLER_DISABLED_SPRITE = Identifier.withDefaultNamespace("container/creative_inventory/scroller_disabled");
    private static final Identifier[] UNSELECTED_TOP_TABS = createTabSprites("tab_top_unselected");
    private static final Identifier[] SELECTED_TOP_TABS = createTabSprites("tab_top_selected");
    private static final Identifier[] UNSELECTED_BOTTOM_TABS = createTabSprites("tab_bottom_unselected");
    private static final Identifier[] SELECTED_BOTTOM_TABS = createTabSprites("tab_bottom_selected");
    private static final Component TRASH_SLOT_TOOLTIP = Component.translatable("inventory.binSlot");

    private final boolean suppressOpeningChar;
    private final Set<TagKey<Item>> visibleTags = new HashSet<>();
    private CreativeModeTab selectedTab = CreativeModeTabs.getDefaultTab();
    private CreatorItemSearch searchIndex;
    private EditBox searchBox;
    private float scrollOffset;
    private boolean scrolling;
    private boolean ignoreTextInput;
    private boolean suppressedOpeningChar;
    private long openNanos;

    private GuiCreatorInventory(LocalPlayer player, boolean suppressOpeningChar)
    {
        super(new CreatorInventoryMenu(player), player.getInventory(), CommonComponents.EMPTY, 195, 136);
        this.suppressOpeningChar = suppressOpeningChar;
    }

    public static void openFromHotkey()
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player != null && mc.level != null)
        {
            GuiBase.openGui(new GuiCreatorInventory(mc.player, true));
        }
    }

    @Override
    protected void init()
    {
        super.init();
        this.openNanos = System.nanoTime();
        CreativeModeTabs.tryRebuildTabContents(this.minecraft.level.enabledFeatures(), true, this.minecraft.level.registryAccess());
        this.rebuildSearchIndex();
        this.searchBox = new EditBox(this.font, this.leftPos + 82, this.topPos + 6, 80, 9, Component.translatable("itemGroup.search"));
        this.searchBox.setMaxLength(50);
        this.searchBox.setBordered(false);
        this.searchBox.setVisible(false);
        this.searchBox.setTextColor(-1);
        this.searchBox.setInvertHighlightedTextColor(false);
        this.addWidget(this.searchBox);

        if (!CreativeModeTabs.tabs().contains(this.selectedTab))
        {
            this.selectedTab = CreativeModeTabs.getDefaultTab();
        }

        this.selectTab(this.selectedTab, true);
    }

    @Override
    public void containerTick()
    {
        if (CreativeModeTabs.tryRebuildTabContents(this.minecraft.level.enabledFeatures(), true, this.minecraft.level.registryAccess()))
        {
            this.rebuildSearchIndex();
            this.refreshSelectedTab();
        }
    }

    @Override
    public void resize(int width, int height)
    {
        int row = this.menu.getRowIndexForScroll(this.scrollOffset);
        String query = this.searchBox != null ? this.searchBox.getValue() : "";
        this.init(width, height);
        this.searchBox.setValue(query);

        if (this.selectedTab.getType() == CreativeModeTab.Type.SEARCH)
        {
            this.refreshSearchResults();
        }

        this.scrollOffset = this.menu.getScrollForRowIndex(row);
        this.menu.scrollTo(this.scrollOffset);
    }

    @Override
    public void onClose()
    {
        this.minecraft.gui.setScreen(null);
    }

    @Override
    public void removed()
    {
        this.menu.setCarried(ItemStack.EMPTY);
    }

    @Override
    protected void slotClicked(@Nullable Slot slot, int slotId, int buttonNum, ContainerInput input)
    {
        if (this.menu.isPaletteSlot(slot))
        {
            this.handlePaletteClick(slot, buttonNum, input);
            return;
        }

        if (this.menu.isTrashSlot(slot))
        {
            if (input == ContainerInput.QUICK_MOVE)
            {
                this.menu.clearAllVirtualSlots();
            }
            else
            {
                this.menu.setCarried(ItemStack.EMPTY);
            }

            return;
        }

        if (slot == null || slotId == -999)
        {
            if (input == ContainerInput.QUICK_CRAFT)
            {
                this.menu.clicked(slotId, buttonNum, input, this.minecraft.player);
                return;
            }

            if (input == ContainerInput.THROW || input == ContainerInput.PICKUP)
            {
                this.clearCarried(buttonNum == 0);
            }

            return;
        }

        this.onMouseClickAction(slot, input);

        switch (input)
        {
            case SWAP ->
            {
                int target = buttonNum == 40 ? CreatorInventory.OFFHAND_SLOT : buttonNum;
                this.menu.swapVirtualSlot(slot, target);
            }
            case CLONE ->
            {
                if (this.menu.getCarried().isEmpty() && slot.hasItem())
                {
                    ItemStack stack = slot.getItem();
                    this.menu.setCarried(stack.copyWithCount(stack.getMaxStackSize()));
                }
            }
            case THROW -> CreatorInventory.getInstance().runTransaction(() -> slot.remove(buttonNum == 0 ? 1 : slot.getItem().getCount()));
            default -> this.menu.clicked(slot.index, buttonNum, input, this.minecraft.player);
        }
    }

    private void handlePaletteClick(Slot slot, int buttonNum, ContainerInput input)
    {
        if (!slot.mayPickup(this.minecraft.player))
        {
            return;
        }

        this.searchBox.moveCursorToEnd(false);
        this.searchBox.setHighlightPos(0);
        ItemStack clicked = slot.getItem();

        if (input == ContainerInput.SWAP)
        {
            int target = buttonNum == 40 ? CreatorInventory.OFFHAND_SLOT : buttonNum;

            if (target == CreatorInventory.OFFHAND_SLOT)
            {
                if (!clicked.isEmpty())
                {
                    CreatorInventory.getInstance().setStack(target, clicked.copyWithCount(clicked.getMaxStackSize()));
                }
            }
            else
            {
                this.menu.copyPaletteToHotbar(slot, target);
            }

            return;
        }

        if (input == ContainerInput.CLONE)
        {
            if (this.menu.getCarried().isEmpty() && !clicked.isEmpty())
            {
                this.menu.setCarried(clicked.copyWithCount(clicked.getMaxStackSize()));
            }

            return;
        }

        if (input == ContainerInput.QUICK_MOVE)
        {
            this.menu.copyPaletteToFirstEmptyHotbar(slot);
            return;
        }

        if (input == ContainerInput.THROW || input == ContainerInput.QUICK_CRAFT || input == ContainerInput.PICKUP_ALL)
        {
            return;
        }

        this.menu.setCarried(CreatorPaletteClickPolicy.applyNormalClick(this.menu.getCarried(), clicked, buttonNum));
    }

    private void clearCarried(boolean all)
    {
        ItemStack carried = this.menu.getCarried();

        if (all)
        {
            this.menu.setCarried(ItemStack.EMPTY);
        }
        else if (!carried.isEmpty())
        {
            carried.shrink(1);
        }
    }

    @Override
    public boolean charTyped(CharacterEvent event)
    {
        if (this.suppressOpeningChar && !this.suppressedOpeningChar && System.nanoTime() - this.openNanos <= 100_000_000L)
        {
            this.suppressedOpeningChar = true;
            return true;
        }

        if (this.ignoreTextInput || this.selectedTab.getType() != CreativeModeTab.Type.SEARCH)
        {
            return false;
        }

        String oldValue = this.searchBox.getValue();

        if (this.searchBox.charTyped(event))
        {
            if (!Objects.equals(oldValue, this.searchBox.getValue()))
            {
                this.refreshSearchResults();
            }

            return true;
        }

        return false;
    }

    @Override
    public boolean preeditUpdated(@Nullable PreeditEvent event)
    {
        return !this.ignoreTextInput && this.selectedTab.getType() == CreativeModeTab.Type.SEARCH && this.searchBox.preeditUpdated(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event)
    {
        this.ignoreTextInput = false;

        if (this.minecraft.options.keyInventory.matches(event))
        {
            this.onClose();
            return true;
        }

        if (this.selectedTab.getType() != CreativeModeTab.Type.SEARCH)
        {
            if (this.minecraft.options.keyChat.matches(event))
            {
                this.ignoreTextInput = true;
                this.selectTab(CreativeModeTabs.searchTab(), false);
                return true;
            }

            return super.keyPressed(event);
        }

        boolean canSwap = !this.menu.isPaletteSlot(this.hoveredSlot) || this.hoveredSlot.hasItem();
        boolean numberKey = InputConstants.getKey(event).getNumericKeyValue().isPresent();

        if (canSwap && numberKey && this.checkHotbarKeyPressed(event))
        {
            this.ignoreTextInput = true;
            return true;
        }

        String oldValue = this.searchBox.getValue();

        if (this.searchBox.keyPressed(event))
        {
            if (!Objects.equals(oldValue, this.searchBox.getValue()))
            {
                this.refreshSearchResults();
            }

            return true;
        }

        return this.searchBox.isFocused() && this.searchBox.isVisible() && !event.isEscape() || super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event)
    {
        this.ignoreTextInput = false;
        return super.keyReleased(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)
    {
        if (this.minecraft.options.keyPickItem.matchesMouse(event) && this.hoveredSlot != null)
        {
            this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, event.button(), ContainerInput.CLONE);
            return true;
        }

        if (event.button() == 0)
        {
            double relativeX = event.x() - this.leftPos;
            double relativeY = event.y() - this.topPos;

            for (CreativeModeTab tab : CreativeModeTabs.tabs())
            {
                if (this.checkTabClicked(tab, relativeX, relativeY))
                {
                    return true;
                }
            }

            if (this.selectedTab.getType() != CreativeModeTab.Type.INVENTORY && this.insideScrollbar(event.x(), event.y()))
            {
                this.scrolling = this.canScroll();
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event)
    {
        if (event.button() == 0)
        {
            double relativeX = event.x() - this.leftPos;
            double relativeY = event.y() - this.topPos;
            this.scrolling = false;

            for (CreativeModeTab tab : CreativeModeTabs.tabs())
            {
                if (this.checkTabClicked(tab, relativeX, relativeY))
                {
                    this.selectTab(tab, false);
                    return true;
                }
            }
        }

        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy)
    {
        if (!this.scrolling)
        {
            return super.mouseDragged(event, dx, dy);
        }

        int top = this.topPos + 18;
        int bottom = top + 112;
        this.scrollOffset = Mth.clamp(((float)event.y() - top - 7.5F) / (bottom - top - 15.0F), 0.0F, 1.0F);
        this.menu.scrollTo(this.scrollOffset);
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY)
    {
        if (super.mouseScrolled(x, y, scrollX, scrollY))
        {
            return true;
        }

        if (!this.canScroll())
        {
            return false;
        }

        this.scrollOffset = this.menu.subtractInputFromScroll(this.scrollOffset, scrollY);
        this.menu.scrollTo(this.scrollOffset);
        return true;
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top)
    {
        boolean outside = mouseX < left || mouseY < top || mouseX >= left + this.imageWidth || mouseY >= top + this.imageHeight;
        return outside && !this.checkTabClicked(this.selectedTab, mouseX - left, mouseY - top);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY)
    {
        if (this.selectedTab.showTitle())
        {
            graphics.text(this.font, this.selectedTab.getDisplayName(), 8, 6, -12566464, false);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick)
    {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);

        for (CreativeModeTab tab : CreativeModeTabs.tabs())
        {
            if (tab != this.selectedTab)
            {
                this.extractTabButton(graphics, mouseX, mouseY, tab);
            }
        }

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                this.selectedTab.getBackgroundTexture(),
                this.leftPos,
                this.topPos,
                0.0F,
                0.0F,
                this.imageWidth,
                this.imageHeight,
                256,
                256
        );

        if (this.insideScrollbar(mouseX, mouseY))
        {
            graphics.requestCursor(this.canScroll() ? this.scrolling ? CursorTypes.RESIZE_NS : CursorTypes.POINTING_HAND : CursorTypes.NOT_ALLOWED);
        }

        this.searchBox.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int scrollbarX = this.leftPos + 175;
        int scrollbarTop = this.topPos + 18;
        int scrollbarBottom = scrollbarTop + 112;

        if (this.selectedTab.canScroll())
        {
            Identifier sprite = this.canScroll() ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE;
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    sprite,
                    scrollbarX,
                    scrollbarTop + (int)((scrollbarBottom - scrollbarTop - 17) * this.scrollOffset),
                    12,
                    15
            );
        }

        this.extractTabButton(graphics, mouseX, mouseY, this.selectedTab);

        if (this.selectedTab.getType() == CreativeModeTab.Type.INVENTORY)
        {
            InventoryScreen.extractEntityInInventoryFollowsMouse(
                    graphics,
                    this.leftPos + 73,
                    this.topPos + 6,
                    this.leftPos + 105,
                    this.topPos + 49,
                    20,
                    0.0625F,
                    mouseX,
                    mouseY,
                    this.minecraft.player
            );
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick)
    {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        for (CreativeModeTab tab : CreativeModeTabs.tabs())
        {
            if (this.checkTabHovering(graphics, tab, mouseX, mouseY))
            {
                break;
            }
        }

        Slot trash = this.menu.isInventoryLayout() ? this.menu.slots.stream().filter(this.menu::isTrashSlot).findFirst().orElse(null) : null;

        if (trash != null && this.isHovering(trash.x, trash.y, 16, 16, mouseX, mouseY))
        {
            graphics.setTooltipForNextFrame(this.font, TRASH_SLOT_TOOLTIP, mouseX, mouseY);
        }
    }

    @Override
    public List<Component> getTooltipFromContainerItem(ItemStack stack)
    {
        boolean paletteSlot = this.menu.isPaletteSlot(this.hoveredSlot);
        boolean category = this.selectedTab.getType() == CreativeModeTab.Type.CATEGORY;
        boolean search = this.selectedTab.getType() == CreativeModeTab.Type.SEARCH;
        TooltipFlag.Default configuredFlag = this.minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL;
        TooltipFlag flag = paletteSlot ? configuredFlag.asCreative() : configuredFlag;
        List<Component> original = stack.getTooltipLines(Item.TooltipContext.of(this.minecraft.level), this.minecraft.player, flag);

        if (original.isEmpty() || category && paletteSlot)
        {
            return original;
        }

        List<Component> lines = new ArrayList<>(original);

        if (search && paletteSlot)
        {
            this.visibleTags.forEach(tag ->
            {
                if (stack.is(tag))
                {
                    lines.add(1, Component.literal("#" + tag.location()).withStyle(ChatFormatting.DARK_PURPLE));
                }
            });
        }

        int line = 1;

        for (CreativeModeTab tab : CreativeModeTabs.tabs())
        {
            if (tab.getType() != CreativeModeTab.Type.SEARCH && tab.contains(stack))
            {
                lines.add(line++, tab.getDisplayName().copy().withStyle(ChatFormatting.BLUE));
            }
        }

        return lines;
    }

    private void selectTab(CreativeModeTab tab, boolean keepSearch)
    {
        CreativeModeTab oldTab = this.selectedTab;
        this.selectedTab = tab;
        this.quickCraftSlots.clear();
        this.menu.setInventoryLayout(tab.getType() == CreativeModeTab.Type.INVENTORY);

        switch (tab.getType())
        {
            case HOTBAR -> this.populateSavedHotbars();
            case CATEGORY -> this.menu.populate(List.copyOf(tab.getDisplayItems()));
            case SEARCH ->
            {
                this.searchBox.setVisible(true);
                this.searchBox.setCanLoseFocus(false);
                this.searchBox.setFocused(true);

                if (!keepSearch && oldTab != tab)
                {
                    this.searchBox.setValue("");
                }

                this.refreshSearchResults();
            }
            case INVENTORY -> this.menu.populate(List.of());
        }

        if (tab.getType() != CreativeModeTab.Type.SEARCH)
        {
            this.searchBox.setVisible(false);
            this.searchBox.setCanLoseFocus(true);
            this.searchBox.setFocused(false);
            this.searchBox.setValue("");
        }

        this.scrollOffset = 0.0F;
        this.menu.scrollTo(0.0F);
    }

    private void refreshSelectedTab()
    {
        if (!CreativeModeTabs.tabs().contains(this.selectedTab))
        {
            this.selectedTab = CreativeModeTabs.getDefaultTab();
        }

        this.selectTab(this.selectedTab, true);
    }

    private void populateSavedHotbars()
    {
        List<ItemStack> stacks = new ArrayList<>();
        HotbarManager manager = this.minecraft.getHotbarManager();

        for (int hotbarIndex = 0; hotbarIndex < 9; ++hotbarIndex)
        {
            Hotbar hotbar = manager.get(hotbarIndex);

            if (hotbar.isEmpty())
            {
                for (int slot = 0; slot < 9; ++slot)
                {
                    if (slot == hotbarIndex)
                    {
                        ItemStack placeholder = new ItemStack(Items.PAPER);
                        placeholder.set(DataComponents.CREATIVE_SLOT_LOCK, Unit.INSTANCE);
                        Component slotKey = this.minecraft.options.keyHotbarSlots[hotbarIndex].getTranslatedKeyMessage();
                        Component saveKey = this.minecraft.options.keySaveHotbarActivator.getTranslatedKeyMessage();
                        placeholder.set(DataComponents.ITEM_NAME, Component.translatable("inventory.hotbarInfo", saveKey, slotKey));
                        stacks.add(placeholder);
                    }
                    else
                    {
                        stacks.add(ItemStack.EMPTY);
                    }
                }
            }
            else
            {
                stacks.addAll(hotbar.load(this.minecraft.level.registryAccess()));
            }
        }

        this.menu.populate(stacks);
    }

    private void rebuildSearchIndex()
    {
        Collection<ItemStack> creativeItems = CreativeModeTabs.searchTab().getDisplayItems();
        List<ItemStack> registeredItems = BuiltInRegistries.ITEM.stream()
                .filter(item -> item != Items.AIR && item.isEnabled(this.minecraft.level.enabledFeatures()))
                .map(ItemStack::new)
                .toList();
        TooltipFlag tooltipFlag = TooltipFlag.Default.NORMAL.asCreative();
        this.searchIndex = new CreatorItemSearch(creativeItems, registeredItems, stack ->
        {
            List<String> values = new ArrayList<>();
            values.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            stack.getTooltipLines(Item.TooltipContext.of(this.minecraft.level), this.minecraft.player, tooltipFlag)
                    .stream()
                    .map(Component::getString)
                    .forEach(values::add);
            return values;
        });
    }

    private void refreshSearchResults()
    {
        this.visibleTags.clear();
        String query = this.searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        this.menu.populate(this.searchIndex.search(query));

        if (query.startsWith("#"))
        {
            this.updateVisibleTags(query.substring(1));
        }

        this.scrollOffset = 0.0F;
    }

    private void updateVisibleTags(String query)
    {
        Predicate<Identifier> matcher = id -> CreatorItemSearch.matchesIdentifier(id, query);
        BuiltInRegistries.ITEM.getTags()
                .map(HolderSet.Named::key)
                .filter(tag -> matcher.test(tag.location()))
                .forEach(this.visibleTags::add);
    }

    private boolean canScroll()
    {
        return this.selectedTab.canScroll() && this.menu.canScroll();
    }

    private boolean insideScrollbar(double mouseX, double mouseY)
    {
        int left = this.leftPos + 175;
        int top = this.topPos + 18;
        return mouseX >= left && mouseY >= top && mouseX < left + 14 && mouseY < top + 112 && this.selectedTab.canScroll();
    }

    private int getTabX(CreativeModeTab tab)
    {
        int x = 27 * tab.column();
        return tab.isAlignedRight() ? this.imageWidth - 27 * (7 - tab.column()) + 1 : x;
    }

    private int getTabY(CreativeModeTab tab)
    {
        return tab.row() == CreativeModeTab.Row.TOP ? -32 : this.imageHeight;
    }

    private boolean checkTabClicked(CreativeModeTab tab, double mouseX, double mouseY)
    {
        int x = this.getTabX(tab);
        int y = this.getTabY(tab);
        return mouseX >= x && mouseX <= x + 26 && mouseY >= y && mouseY <= y + 32;
    }

    private boolean checkTabHovering(GuiGraphicsExtractor graphics, CreativeModeTab tab, int mouseX, int mouseY)
    {
        int x = this.getTabX(tab);
        int y = this.getTabY(tab);

        if (this.isHovering(x + 3, y + 3, 21, 27, mouseX, mouseY))
        {
            graphics.setTooltipForNextFrame(this.font, tab.getDisplayName(), mouseX, mouseY);
            return true;
        }

        return false;
    }

    private void extractTabButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CreativeModeTab tab)
    {
        boolean selected = tab == this.selectedTab;
        boolean top = tab.row() == CreativeModeTab.Row.TOP;
        int x = this.leftPos + this.getTabX(tab);
        int y = this.topPos - (top ? 28 : -(this.imageHeight - 4));
        Identifier[] sprites = top ? selected ? SELECTED_TOP_TABS : UNSELECTED_TOP_TABS : selected ? SELECTED_BOTTOM_TABS : UNSELECTED_BOTTOM_TABS;

        if (!selected && mouseX > x && mouseY > y && mouseX < x + 26 && mouseY < y + 32)
        {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprites[Mth.clamp(tab.column(), 0, sprites.length - 1)], x, y, 26, 32);
        graphics.item(tab.getIconItem(), x + 5, y + (top ? 9 : 7));
    }

    private static Identifier[] createTabSprites(String name)
    {
        Identifier[] sprites = new Identifier[7];

        for (int index = 0; index < sprites.length; ++index)
        {
            sprites[index] = Identifier.withDefaultNamespace("container/creative_inventory/" + name + "_" + (index + 1));
        }

        return sprites;
    }
}
