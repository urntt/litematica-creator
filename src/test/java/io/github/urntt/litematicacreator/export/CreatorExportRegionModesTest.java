package io.github.urntt.litematicacreator.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraft.world.ticks.TickPriority;
import org.junit.jupiter.api.Test;

import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.SchematicMetadata;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.malilib.util.nbt.NbtUtils;
import io.github.urntt.litematicacreator.config.CreatorExportRegionMode;
import io.github.urntt.litematicacreator.creator.CreatorSchematicEditor;

class CreatorExportRegionModesTest
{
    @Test
    void rawModePreservesRegionTopologyAndRecalculatesStatistics()
    {
        CreatorRegionSnapshot negative = region(
                "negative",
                new BlockPos(4, 2, 3),
                new BlockPos(-2, 1, 1),
                Blocks.STONE.defaultBlockState(),
                Blocks.DIRT.defaultBlockState()
        );
        SchematicMetadata metadata = metadata();
        metadata.setRegionCount(99);
        metadata.setTotalBlocks(99);
        metadata.setTotalVolume(99);

        CreatorSchematicSnapshot output = CreatorSchematicSnapshot.of(metadata, List.of(negative))
                .normalize(CreatorExportRegionMode.RAW, null, null);

        CreatorRegionSnapshot result = output.regions().getFirst();
        assertEquals("negative", result.name());
        assertEquals(new BlockPos(4, 2, 3), result.position());
        assertEquals(new BlockPos(-2, 1, 1), result.signedSize());
        assertEquals(Blocks.STONE.defaultBlockState(), result.blocks().get(0, 0, 0));
        assertEquals(Blocks.DIRT.defaultBlockState(), result.blocks().get(1, 0, 0));
        assertEquals(1, output.metadata().getRegionCount());
        assertEquals(2, output.metadata().getTotalBlocks());
        assertEquals(2, output.metadata().getTotalVolume());
    }

    @Test
    void sparseModeOnlyCompactsCreatorCellsAndLeavesOrdinaryRegionsIntact()
    {
        CreatorRegionSnapshot ordinary = region(
                "ordinary",
                new BlockPos(10, 0, 0),
                new BlockPos(-2, 1, 1),
                Blocks.COBBLESTONE.defaultBlockState(),
                Blocks.MOSSY_COBBLESTONE.defaultBlockState()
        );
        List<CreatorRegionSnapshot> regions = new ArrayList<>();
        regions.add(ordinary);

        for (int y = 0; y < 2; y++)
        {
            for (int z = 0; z < 2; z++)
            {
                for (int x = 0; x < 2; x++)
                {
                    regions.add(cell(x, y, z, Blocks.STONE.defaultBlockState()));
                }
            }
        }

        CreatorSchematicSnapshot output = CreatorSchematicSnapshot.of(metadata(), regions)
                .normalize(CreatorExportRegionMode.SPARSE_COMPACT, null, null);

        assertEquals(2, output.regions().size());
        CreatorRegionSnapshot preserved = output.regions().stream().filter(region -> region.name().equals("ordinary")).findFirst().orElseThrow();
        CreatorRegionSnapshot compact = output.regions().stream().filter(region -> !region.name().equals("ordinary")).findFirst().orElseThrow();
        assertEquals(ordinary.position(), preserved.position());
        assertEquals(ordinary.signedSize(), preserved.signedSize());
        assertEquals(new BlockPos(2, 2, 2), compact.signedSize());
        assertEquals(8, countNonAir(compact));
        assertEquals(10, output.metadata().getTotalBlocks());
        assertEquals(10, output.metadata().getTotalVolume());
    }

    @Test
    void sparseModeDropsEmptyCellsButRetainsCellsWithAttachedData()
    {
        CreatorRegionSnapshot empty = cell(0, 0, 0, Blocks.AIR.defaultBlockState());
        CreatorRegionSnapshot attached = cell(2, 0, 0, Blocks.AIR.defaultBlockState());
        attached.entities().add(entity(new Vec3(0.5, 0.0, 0.5)));

        CreatorSchematicSnapshot output = CreatorSchematicSnapshot.of(metadata(), List.of(empty, attached))
                .normalize(CreatorExportRegionMode.SPARSE_COMPACT, null, null);

        assertEquals(1, output.regions().size());
        assertEquals(attached.position(), output.regions().getFirst().position());
        assertTrue(output.regions().getFirst().blocks().get(0, 0, 0).isAir());
        assertEquals(1, output.regions().getFirst().entities().size());
    }

    @Test
    void sparseModeRelocatesAllAttachedDataIntoMergedRegion()
    {
        CreatorRegionSnapshot first = cell(4, 0, 0, Blocks.STONE.defaultBlockState());
        CreatorRegionSnapshot second = cell(5, 0, 0, Blocks.CHEST.defaultBlockState());
        CompoundTag blockEntity = new CompoundTag();
        blockEntity.putString("id", "minecraft:chest");
        NbtUtils.writeBlockPosToTag(BlockPos.ZERO, blockEntity);
        second.blockEntities().put(BlockPos.ZERO, blockEntity);
        second.entities().add(entity(new Vec3(0.25, 0.5, 0.75)));
        second.blockTicks().put(BlockPos.ZERO, new ScheduledTick<>(Blocks.STONE, BlockPos.ZERO, 8L, TickPriority.NORMAL, 2L));
        second.fluidTicks().put(BlockPos.ZERO, new ScheduledTick<>(Fluids.WATER, BlockPos.ZERO, 9L, TickPriority.HIGH, 3L));

        CreatorRegionSnapshot merged = CreatorSchematicSnapshot.of(metadata(), List.of(first, second))
                .normalize(CreatorExportRegionMode.SPARSE_COMPACT, null, null)
                .regions()
                .getFirst();

        BlockPos relocated = new BlockPos(1, 0, 0);
        assertEquals(new BlockPos(4, 0, 0), merged.position());
        assertEquals(new BlockPos(2, 1, 1), merged.signedSize());
        assertTrue(merged.blockEntities().containsKey(relocated));
        assertEquals(1, merged.blockEntities().get(relocated).getIntOr("x", -1));
        assertEquals(new Vec3(1.25, 0.5, 0.75), merged.entities().getFirst().posVec());
        assertEquals(relocated, merged.blockTicks().get(relocated).pos());
        assertEquals(8L, merged.blockTicks().get(relocated).triggerTick());
        assertEquals(relocated, merged.fluidTicks().get(relocated).pos());
        assertEquals(9L, merged.fluidTicks().get(relocated).triggerTick());
    }

    @Test
    void projectionEnclosingModeCreatesOneMinimalCuboidWithAirGaps()
    {
        CreatorRegionSnapshot first = cell(-2, 1, 3, Blocks.STONE.defaultBlockState());
        CreatorRegionSnapshot second = cell(1, 1, 3, Blocks.DIRT.defaultBlockState());

        CreatorSchematicSnapshot output = CreatorSchematicSnapshot.of(metadata(), List.of(first, second))
                .normalize(CreatorExportRegionMode.ENCLOSING_PROJECTION, null, null);

        assertEquals(1, output.regions().size());
        CreatorRegionSnapshot enclosing = output.regions().getFirst();
        assertEquals(new BlockPos(-2, 1, 3), enclosing.position());
        assertEquals(new BlockPos(4, 1, 1), enclosing.signedSize());
        assertEquals(Blocks.STONE.defaultBlockState(), enclosing.blocks().get(0, 0, 0));
        assertTrue(enclosing.blocks().get(1, 0, 0).isAir());
        assertTrue(enclosing.blocks().get(2, 0, 0).isAir());
        assertEquals(Blocks.DIRT.defaultBlockState(), enclosing.blocks().get(3, 0, 0));
        assertEquals(2, output.metadata().getTotalBlocks());
        assertEquals(4, output.metadata().getTotalVolume());
    }

    @Test
    void exportMetadataOverridesOnlyTheOutputSnapshot()
    {
        CreatorSchematicSnapshot source = CreatorSchematicSnapshot.of(metadata(), List.of(cell(0, 0, 0, Blocks.STONE.defaultBlockState())));
        CreatorSchematicSnapshot output = source.withExportMetadata(new CreatorExportMetadata("export", "builder", "description"));

        assertEquals("source", source.metadata().getName());
        assertEquals("export", output.metadata().getName());
        assertEquals("builder", output.metadata().getAuthor());
        assertEquals("description", output.metadata().getDescription());
    }

    private static CreatorRegionSnapshot cell(int x, int y, int z, BlockState state)
    {
        return region(
                CreatorSchematicEditor.CELL_REGION_PREFIX + x + "_" + y + "_" + z,
                new BlockPos(x, y, z),
                new BlockPos(1, 1, 1),
                state
        );
    }

    private static CreatorRegionSnapshot region(
            String name,
            BlockPos position,
            BlockPos signedSize,
            BlockState... states)
    {
        int sizeX = Math.abs(signedSize.getX());
        int sizeY = Math.abs(signedSize.getY());
        int sizeZ = Math.abs(signedSize.getZ());
        LitematicaBlockStateContainer container = new LitematicaBlockStateContainer(sizeX, sizeY, sizeZ);

        for (int index = 0; index < states.length; index++)
        {
            int x = index % sizeX;
            int remainder = index / sizeX;
            int z = remainder % sizeZ;
            int y = remainder / sizeZ;
            container.set(x, y, z, states[index]);
        }

        return new CreatorRegionSnapshot(
                name,
                position,
                signedSize,
                container,
                new HashMap<>(),
                new ArrayList<>(),
                new HashMap<BlockPos, ScheduledTick<Block>>(),
                new HashMap<BlockPos, ScheduledTick<Fluid>>()
        );
    }

    private static LitematicaSchematic.EntityInfo entity(Vec3 pos)
    {
        CompoundTag nbt = new CompoundTag();
        NbtUtils.writeEntityPositionToTag(pos, nbt);
        return new LitematicaSchematic.EntityInfo(pos, nbt);
    }

    private static SchematicMetadata metadata()
    {
        SchematicMetadata metadata = new SchematicMetadata();
        metadata.setName("source");
        metadata.setAuthor("author");
        metadata.setDescription("description");
        return metadata;
    }

    private static int countNonAir(CreatorRegionSnapshot region)
    {
        int count = 0;

        for (int y = 0; y < region.blocks().getSize().getY(); y++)
        {
            for (int z = 0; z < region.blocks().getSize().getZ(); z++)
            {
                for (int x = 0; x < region.blocks().getSize().getX(); x++)
                {
                    if (!region.blocks().get(x, y, z).isAir())
                    {
                        count++;
                    }
                }
            }
        }

        return count;
    }
}
