package io.github.urntt.litematicacreator.gui;

import java.util.Map;

import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;

import io.github.urntt.litematicacreator.creator.CreatorBlockEntityEditSession;

/** The vanilla editor for a sign in a projection. Its text is committed to the schematic instead of the server. */
public final class ProjectionSignEditScreen extends SignEditScreen
{
    private final CreatorBlockEntityEditSession session;

    public ProjectionSignEditScreen(CreatorBlockEntityEditSession session, SignBlockEntity sign, SignTextSlot slot)
    {
        super(sign, slot, false);
        this.session = session;
    }

    // Vanilla closes the editor once the real player is out of reach of a real sign; a projection sign is neither.
    @Override
    public void tick()
    {
    }

    @Override
    public void removed()
    {
        this.minecraft.textInputManager().stopTextInput(this);
        this.session.commit(Map.of());
    }
}
