package io.github.urntt.litematicacreator.gui;

import javax.annotation.Nullable;

import net.minecraft.client.gui.screens.inventory.DispenserScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

/** The vanilla screen for a dispenser or dropper in a projection, without any container packets. */
public final class ProjectionDispenserScreen extends DispenserScreen implements CreatorProjectionContainerScreen
{
    private final CreatorProjectionContainerInteraction interaction;

    ProjectionDispenserScreen(CreatorProjectionContainerInteraction interaction, DispenserMenu menu, Component title)
    {
        super(menu, interaction.inventory(), title);
        this.interaction = interaction;
    }

    @Override
    public CreatorProjectionContainerInteraction interaction()
    {
        return this.interaction;
    }

    @Override
    protected void slotClicked(@Nullable Slot slot, int slotId, int buttonNum, ContainerInput input)
    {
        this.interaction.slotClicked(slot, slotId, buttonNum, input);
    }

    // Vanilla also closes the server-side container here; a projection has none.
    @Override
    public void onClose()
    {
        this.minecraft.gui.setScreen(null);
    }

    @Override
    public void removed()
    {
        super.removed();
        this.interaction.close();
    }
}
