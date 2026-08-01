package io.github.urntt.litematicacreator.creator;

import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;

public record CreatorFocus(SchematicPlacement placement)
{
    public LitematicaSchematic schematic()
    {
        return this.placement.getSchematic();
    }

    public boolean isDirty()
    {
        return this.schematic().getMetadata().wasModifiedSinceSaved();
    }
}
