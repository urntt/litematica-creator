package io.github.urntt.litematicacreator.creator;

final class CreatorPlacementPolicy
{
    private CreatorPlacementPolicy()
    {
    }

    static boolean canWrite(boolean targetIsAir, boolean targetIsReplaceable)
    {
        return targetIsAir || targetIsReplaceable;
    }

    /**
     * Vanilla already checked the cell it placed into. Any other cell a placement writes, such as the upper half of a
     * door, and a cell whose projection was hidden from vanilla by the clicked real block, must still be free.
     */
    static boolean canWriteAlongside(boolean checkedByVanilla, boolean targetIsAir, boolean targetIsReplaceable)
    {
        return checkedByVanilla || canWrite(targetIsAir, targetIsReplaceable);
    }
}
