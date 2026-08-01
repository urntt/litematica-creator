package io.github.urntt.litematicacreator.creator;

import java.util.List;
import java.util.Map;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatorRegionEmptinessTest
{
    @BeforeAll
    static void bootstrapMinecraft()
    {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void recognizesAirOnlyContainersOfDifferentSizes()
    {
        assertTrue(CreatorRegionEmptiness.hasOnlyAir(new LitematicaBlockStateContainer(1, 1, 1)));
        assertTrue(CreatorRegionEmptiness.hasOnlyAir(new LitematicaBlockStateContainer(4, 3, 2)));
    }

    @Test
    void rejectsContainerWithAnyNonAirBlock()
    {
        LitematicaBlockStateContainer container = new LitematicaBlockStateContainer(4, 3, 2);
        container.set(3, 2, 1, Blocks.STONE.defaultBlockState());

        assertFalse(CreatorRegionEmptiness.hasOnlyAir(container));
    }

    @Test
    void requiresEveryAttachedDataCollectionToBeEmpty()
    {
        LitematicaBlockStateContainer container = new LitematicaBlockStateContainer(1, 1, 1);
        Map<BlockPos, Object> attachedData = Map.of(BlockPos.ZERO, new Object());

        assertTrue(CreatorRegionEmptiness.isCompletelyEmpty(container, Map.of(), List.of(), Map.of(), Map.of()));
        assertFalse(CreatorRegionEmptiness.isCompletelyEmpty(container, attachedData, List.of(), Map.of(), Map.of()));
        assertFalse(CreatorRegionEmptiness.isCompletelyEmpty(container, Map.of(), List.of(new Object()), Map.of(), Map.of()));
        assertFalse(CreatorRegionEmptiness.isCompletelyEmpty(container, Map.of(), List.of(), attachedData, Map.of()));
        assertFalse(CreatorRegionEmptiness.isCompletelyEmpty(container, Map.of(), List.of(), Map.of(), attachedData));
    }
}
