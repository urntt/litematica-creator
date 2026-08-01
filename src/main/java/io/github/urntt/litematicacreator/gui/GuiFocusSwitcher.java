package io.github.urntt.litematicacreator.gui;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.gui.GuiBase;
import io.github.urntt.litematicacreator.creator.CreatorFocus;
import io.github.urntt.litematicacreator.creator.CreatorManager;
import io.github.urntt.litematicacreator.creator.CreatorPlacementVisibility;

public class GuiFocusSwitcher extends Screen
{
    private static final int ROW_HEIGHT = 22;
    private static final int MAX_VISIBLE_ROWS = 10;

    private final List<SchematicPlacement> placements;
    private final boolean overlapChoice;
    private int selectedIndex;
    private int firstVisible;

    private GuiFocusSwitcher(List<SchematicPlacement> placements, boolean overlapChoice)
    {
        super(Component.translatable(overlapChoice ?
                "litematica-creator.gui.title.focus_overlap" :
                "litematica-creator.gui.title.focus_switcher"));
        this.placements = new ArrayList<>(placements);
        this.overlapChoice = overlapChoice;
        CreatorFocus focus = CreatorManager.getInstance().getFocus();

        if (focus != null)
        {
            int index = this.placements.indexOf(focus.placement());
            this.selectedIndex = Math.max(0, index);
        }
    }

    public static void open()
    {
        CreatorManager.getInstance().cancelFocusChoice();
        GuiBase.openGui(new GuiFocusSwitcher(
                DataManager.getSchematicPlacementManager().getAllSchematicsPlacements(),
                false
        ));
    }

    public static void openForOverlap(List<SchematicPlacement> candidates)
    {
        CreatorManager.getInstance().requestFocusChoice(candidates);
        GuiBase.openGui(new GuiFocusSwitcher(candidates, true));
    }

    @Override
    protected void init()
    {
        this.rebuildButtons();
    }

    @Override
    public boolean keyPressed(KeyEvent input)
    {
        if (input.key() == GLFW.GLFW_KEY_UP)
        {
            this.moveSelection(-1);
            return true;
        }
        else if (input.key() == GLFW.GLFW_KEY_DOWN)
        {
            this.moveSelection(1);
            return true;
        }
        else if (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER)
        {
            this.chooseSelected();
            return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public void onClose()
    {
        CreatorManager.getInstance().cancelFocusChoice();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    private void rebuildButtons()
    {
        this.clearWidgets();
        int panelWidth = Math.min(440, this.width - 24);
        int visibleRows = Math.min(MAX_VISIBLE_ROWS, Math.max(1, (this.height - 82) / ROW_HEIGHT));
        int x = (this.width - panelWidth) / 2;
        int y = Math.max(24, (this.height - visibleRows * ROW_HEIGHT - 34) / 2);
        this.keepSelectionVisible(visibleRows);
        int end = Math.min(this.placements.size(), this.firstVisible + visibleRows);

        for (int i = this.firstVisible; i < end; ++i)
        {
            final int index = i;
            this.addRenderableWidget(Button.builder(this.getPlacementLabel(i), button -> this.choose(index))
                                           .bounds(x, y + (i - this.firstVisible) * ROW_HEIGHT, panelWidth, 20)
                                           .build());
        }

        int controlsY = y + visibleRows * ROW_HEIGHT + 6;

        if (!this.overlapChoice)
        {
            this.addRenderableWidget(Button.builder(
                    Component.translatable("litematica-creator.gui.button.clear_focus"),
                    button -> {
                        CreatorManager.getInstance().clearFocus();
                        super.onClose();
                    }
            ).bounds(x, controlsY, (panelWidth - 6) / 2, 20).build());
        }

        int closeX = this.overlapChoice ? x : x + (panelWidth + 6) / 2;
        int closeWidth = this.overlapChoice ? panelWidth : (panelWidth - 6) / 2;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> this.onClose())
                                       .bounds(closeX, controlsY, closeWidth, 20)
                                       .build());
    }

    private Component getPlacementLabel(int index)
    {
        SchematicPlacement placement = this.placements.get(index);
        CreatorFocus focus = CreatorManager.getInstance().getFocus();
        String cursor = index == this.selectedIndex ? "> " : "  ";
        String focused = focus != null && focus.placement() == placement ? "* " : "";
        String stateKey = CreatorPlacementVisibility.isSuppressed(placement) ?
                "litematica-creator.gui.focus_state.hidden" :
                placement.isEnabled() ? "litematica-creator.gui.focus_state.on" : "litematica-creator.gui.focus_state.off";
        String state = Component.translatable(stateKey).getString();
        String schematicName = placement.getSchematic().getMetadata().getName();
        return Component.literal(cursor + focused + placement.getName() + " [" + schematicName + "] (" + state + ")");
    }

    private void moveSelection(int amount)
    {
        if (!this.placements.isEmpty())
        {
            this.selectedIndex = Math.floorMod(this.selectedIndex + amount, this.placements.size());
            this.rebuildButtons();
        }
    }

    private void chooseSelected()
    {
        if (!this.placements.isEmpty())
        {
            this.choose(this.selectedIndex);
        }
    }

    private void choose(int index)
    {
        SchematicPlacement placement = this.placements.get(index);
        CreatorManager manager = CreatorManager.getInstance();

        if (this.overlapChoice)
        {
            manager.focusPlacementFromOverlap(placement, this.placements);
        }
        else
        {
            manager.focusPlacement(placement);
        }

        super.onClose();
    }

    private void keepSelectionVisible(int visibleRows)
    {
        if (this.selectedIndex < this.firstVisible)
        {
            this.firstVisible = this.selectedIndex;
        }
        else if (this.selectedIndex >= this.firstVisible + visibleRows)
        {
            this.firstVisible = this.selectedIndex - visibleRows + 1;
        }
    }
}
