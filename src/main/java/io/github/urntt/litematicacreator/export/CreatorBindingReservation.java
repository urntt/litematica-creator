package io.github.urntt.litematicacreator.export;

import java.nio.file.Path;

import fi.dy.masa.litematica.schematic.LitematicaSchematic;

public final class CreatorBindingReservation implements AutoCloseable
{
    private final CreatorSchematicBindingService owner;
    private final LitematicaSchematic schematic;
    private final Path target;
    private boolean active = true;

    CreatorBindingReservation(
            CreatorSchematicBindingService owner,
            LitematicaSchematic schematic,
            Path target)
    {
        this.owner = owner;
        this.schematic = schematic;
        this.target = target;
    }

    LitematicaSchematic schematic()
    {
        return this.schematic;
    }

    Path target()
    {
        return this.target;
    }

    boolean isActive()
    {
        return this.active;
    }

    void markReleased()
    {
        this.active = false;
    }

    @Override
    public void close()
    {
        this.owner.release(this);
    }
}
