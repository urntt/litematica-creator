package io.github.urntt.litematicacreator.creator;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatorCellDataTest
{
    @BeforeAll
    static void bootstrapMinecraft()
    {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void differentBlockInvalidatesCellData()
    {
        assertTrue(CreatorCellData.isInvalidatedBy(Blocks.CHEST.defaultBlockState(), Blocks.AIR.defaultBlockState()));
        assertTrue(CreatorCellData.isInvalidatedBy(Blocks.CHEST.defaultBlockState(), Blocks.BARREL.defaultBlockState()));
        assertTrue(CreatorCellData.isInvalidatedBy(Blocks.AIR.defaultBlockState(), Blocks.STONE.defaultBlockState()));
    }

    @Test
    void stateChangeOfSameBlockKeepsCellData()
    {
        assertFalse(CreatorCellData.isInvalidatedBy(
                Blocks.CHEST.defaultBlockState(),
                Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.EAST)
        ));
    }

    @Test
    void clearRemovesOnlyTheEditedPosition()
    {
        BlockPos edited = new BlockPos(1, 2, 3);
        BlockPos other = new BlockPos(0, 0, 0);
        Map<BlockPos, Object> blockEntities = new HashMap<>(Map.of(edited, "chest", other, "sign"));
        Map<BlockPos, Object> blockTicks = new HashMap<>(Map.of(edited, "tick"));
        Map<BlockPos, Object> fluidTicks = new HashMap<>(Map.of(other, "fluid"));

        assertTrue(CreatorCellData.clear(edited, blockEntities, blockTicks, fluidTicks));
        assertEquals(Map.of(other, "sign"), blockEntities);
        assertEquals(Map.of(), blockTicks);
        assertEquals(Map.of(other, "fluid"), fluidTicks);
    }

    @Test
    void clearToleratesMissingMapsAndEntries()
    {
        assertFalse(CreatorCellData.clear(BlockPos.ZERO, null, null, null));
        assertFalse(CreatorCellData.clear(BlockPos.ZERO, new HashMap<>(), new HashMap<>(), new HashMap<>()));
    }
}
