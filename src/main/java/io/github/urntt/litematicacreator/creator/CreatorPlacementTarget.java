package io.github.urntt.litematicacreator.creator;

import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.selection.Box;

public record CreatorPlacementTarget(SchematicPlacement placement, String regionName, Box box)
{
}
