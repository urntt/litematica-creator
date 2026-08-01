package io.github.urntt.litematicacreator.creator;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatorSparseRegionPolicyTest
{
    @Test
    void onlyCreatorOwnedOneBlockRegionsAreRemovable()
    {
        String creatorCell = CreatorSchematicEditor.CELL_REGION_PREFIX + "0_0_0";
        assertTrue(CreatorSparseRegionPolicy.isRemovableCell(creatorCell, new BlockPos(1, 1, 1)));
        assertTrue(CreatorSparseRegionPolicy.isRemovableCell(creatorCell, new BlockPos(-1, 1, -1)));
        assertFalse(CreatorSparseRegionPolicy.isRemovableCell("user-region", new BlockPos(1, 1, 1)));
        assertFalse(CreatorSparseRegionPolicy.isRemovableCell(creatorCell, new BlockPos(2, 1, 1)));
        assertFalse(CreatorSparseRegionPolicy.isRemovableCell(creatorCell, null));
    }
}
