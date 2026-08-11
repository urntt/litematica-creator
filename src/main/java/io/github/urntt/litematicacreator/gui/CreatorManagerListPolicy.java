package io.github.urntt.litematicacreator.gui;

final class CreatorManagerListPolicy
{
    private CreatorManagerListPolicy()
    {
    }

    static boolean includePlacementRows(int placementCount)
    {
        return placementCount > 1;
    }

    static boolean schematicRowIsSelected(
            boolean sameSchematic,
            boolean hasInspectedPlacement,
            int placementCount)
    {
        return sameSchematic && (!hasInspectedPlacement || placementCount == 1);
    }
}
