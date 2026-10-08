package io.github.urntt.litematicacreator.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;

import io.github.urntt.litematicacreator.creator.CreatorBlockEntityEditSession;
import io.github.urntt.litematicacreator.creator.CreatorBlockEntityEditorPolicy;

/** Builds the vanilla menu and screen for a projection container, with the Creator virtual inventory as player slots. */
public final class CreatorProjectionContainerScreens
{
    // A projection menu is never the player's open container, so server packets never address it by this id.
    private static final int CONTAINER_ID = 0;

    private CreatorProjectionContainerScreens()
    {
    }

    public static Screen create(
            Minecraft minecraft,
            CreatorBlockEntityEditSession session,
            CreatorBlockEntityEditorPolicy.Editor editor,
            Container container,
            boolean largeChest,
            Component title)
    {
        CreatorProjectionContainerInteraction interaction = new CreatorProjectionContainerInteraction(minecraft, session);

        return switch (editor)
        {
            case CHEST, BARREL ->
            {
                ChestMenu menu = largeChest ?
                        ChestMenu.sixRows(CONTAINER_ID, interaction.inventory(), container) :
                        ChestMenu.threeRows(CONTAINER_ID, interaction.inventory(), container);
                interaction.bind(menu);
                yield new ProjectionChestScreen(interaction, menu, title);
            }
            case SHULKER_BOX ->
            {
                ShulkerBoxMenu menu = new ShulkerBoxMenu(CONTAINER_ID, interaction.inventory(), container);
                interaction.bind(menu);
                yield new ProjectionShulkerBoxScreen(interaction, menu, title);
            }
            case HOPPER ->
            {
                HopperMenu menu = new HopperMenu(CONTAINER_ID, interaction.inventory(), container);
                interaction.bind(menu);
                yield new ProjectionHopperScreen(interaction, menu, title);
            }
            case DISPENSER ->
            {
                DispenserMenu menu = new DispenserMenu(CONTAINER_ID, interaction.inventory(), container);
                interaction.bind(menu);
                yield new ProjectionDispenserScreen(interaction, menu, title);
            }
            default -> throw new IllegalArgumentException("Not a container editor: " + editor);
        };
    }
}
