package io.github.urntt.litematicacreator.export;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.ScheduledTick;

import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.SchematicMetadata;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.util.nbt.NbtUtils;
import io.github.urntt.litematicacreator.compat.litematica.CreatorLitematicaDataAdapter;
import io.github.urntt.litematicacreator.config.CreatorExportRegionMode;
import io.github.urntt.litematicacreator.creator.CreatorSchematicEditor;
import io.github.urntt.litematicacreator.mixin.LitematicaSchematicAccessor;

final class CreatorSchematicSnapshot
{
    private static final Comparator<CreatorRegionSnapshot> REGION_ORDER = Comparator.comparing(CreatorRegionSnapshot::name);
    private static final String COMPACT_REGION_PREFIX = "__litematica_creator_region_";

    private final SchematicMetadata metadata;
    private final List<CreatorRegionSnapshot> regions;

    private CreatorSchematicSnapshot(SchematicMetadata metadata, List<CreatorRegionSnapshot> regions)
    {
        this.metadata = metadata;
        this.regions = List.copyOf(regions);
    }

    static CreatorSchematicSnapshot of(SchematicMetadata metadata, List<CreatorRegionSnapshot> regions)
    {
        return new CreatorSchematicSnapshot(copyMetadata(metadata), copyRegions(regions));
    }

    static CreatorSchematicSnapshot capture(LitematicaSchematic schematic)
    {
        LitematicaSchematicAccessor accessor = (LitematicaSchematicAccessor) schematic;
        SchematicMetadata metadata = new SchematicMetadata();
        CreatorSchematicMetadataCopies.copy(metadata, schematic.getMetadata());
        List<CreatorRegionSnapshot> regions = new ArrayList<>();

        accessor.litematicacreator$getBlockContainers().keySet().stream().sorted().forEach(name -> {
            LitematicaBlockStateContainer container = accessor.litematicacreator$getBlockContainers().get(name);
            BlockPos position = accessor.litematicacreator$getSubRegionPositions().get(name);
            BlockPos size = accessor.litematicacreator$getSubRegionSizes().get(name);

            if (container == null || position == null || size == null)
            {
                return;
            }

            Map<BlockPos, CompoundTag> blockEntities = CreatorLitematicaDataAdapter.snapshotBlockEntities(
                    accessor.litematicacreator$getTileEntities().getOrDefault(name, Map.of())
            );
            List<CreatorEntitySnapshot> entities = snapshotEntities(
                    accessor.litematicacreator$getEntities().getOrDefault(name, List.of())
            );
            Map<BlockPos, ScheduledTick<Block>> blockTicks = accessor.litematicacreator$getPendingBlockTicks().getOrDefault(name, Map.of());
            Map<BlockPos, ScheduledTick<Fluid>> fluidTicks = accessor.litematicacreator$getPendingFluidTicks().getOrDefault(name, Map.of());
            regions.add(new CreatorRegionSnapshot(
                    name,
                    position,
                    size,
                    CreatorRegionSnapshot.copyContainer(container),
                    blockEntities,
                    entities,
                    new HashMap<>(blockTicks),
                    new HashMap<>(fluidTicks)
            ));
        });

        return new CreatorSchematicSnapshot(metadata, regions);
    }

    CreatorSchematicSnapshot normalize(
            CreatorExportRegionMode mode,
            @Nullable ClientLevel world,
            @Nullable SchematicPlacement samplingPlacement)
    {
        return switch (mode)
        {
            case RAW -> withRecalculatedMetadata(this.metadata, copyRegions(this.regions));
            case SPARSE_COMPACT -> this.sparseCompact();
            case ENCLOSING_PROJECTION -> this.enclosing(null, null);
            case ENCLOSING_WITH_WORLD ->
            {
                if (world == null || samplingPlacement == null)
                {
                    throw new IllegalArgumentException("World-backed export requires a sampling placement");
                }

                yield this.enclosing(world, samplingPlacement);
            }
        };
    }

    CreatorExportPreview preview()
    {
        Bounds bounds = Bounds.of(this.regions);
        long volume = 0L;
        int blocks = 0;

        for (CreatorRegionSnapshot region : this.regions)
        {
            Vec3i size = region.blocks().getSize();
            volume += (long) size.getX() * size.getY() * size.getZ();
            blocks += countBlocks(region.blocks());
        }

        return new CreatorExportPreview(this.regions.size(), blocks, volume, bounds != null ? bounds.size() : BlockPos.ZERO);
    }

    CompoundTag toNbt(String finalFileName)
    {
        LitematicaSchematic schematic = this.toSchematic();
        CompoundTag nbt = CreatorSchematicMetadataCopies.writeSchematicToNbt(schematic);
        return CreatorSchematicExportNormalizer.normalizeForExport(nbt, finalFileName);
    }

    CreatorSchematicSnapshot withExportMetadata(CreatorExportMetadata exportMetadata)
    {
        SchematicMetadata metadata = copyMetadata(this.metadata);
        metadata.setName(exportMetadata.name());
        metadata.setAuthor(exportMetadata.author());
        metadata.setDescription(exportMetadata.description());
        return new CreatorSchematicSnapshot(metadata, this.regions);
    }

    SchematicMetadata metadata()
    {
        return this.metadata;
    }

    List<CreatorRegionSnapshot> regions()
    {
        return this.regions;
    }

    private CreatorSchematicSnapshot sparseCompact()
    {
        List<CreatorRegionSnapshot> preserved = new ArrayList<>();
        Map<BlockPos, CreatorRegionSnapshot> candidates = new LinkedHashMap<>();
        Set<BlockPos> duplicatePositions = new HashSet<>();
        List<CreatorRegionSnapshot> ordinary = this.regions.stream()
                .filter(region -> !isCreatorCell(region))
                .toList();

        for (CreatorRegionSnapshot region : this.regions)
        {
            if (!isCreatorCell(region) || overlapsAny(region.min(), ordinary))
            {
                preserved.add(region.copy());
                continue;
            }

            if (region.blocks().get(0, 0, 0).isAir())
            {
                if (region.hasAttachedData())
                {
                    preserved.add(region.copy());
                }

                continue;
            }

            CreatorRegionSnapshot previous = candidates.putIfAbsent(region.min(), region);

            if (previous != null)
            {
                duplicatePositions.add(region.min());
                preserved.add(region.copy());
            }
        }

        for (BlockPos duplicate : duplicatePositions)
        {
            CreatorRegionSnapshot first = candidates.remove(duplicate);

            if (first != null)
            {
                preserved.add(first.copy());
            }
        }

        Set<String> usedNames = new HashSet<>();
        this.regions.forEach(region -> usedNames.add(region.name()));
        int regionIndex = 1;

        for (CreatorCuboid cuboid : CreatorCuboidPartitioner.partition(candidates.keySet()))
        {
            String name;

            do
            {
                name = COMPACT_REGION_PREFIX + String.format("%04d", regionIndex++);
            }
            while (!usedNames.add(name));

            CreatorRegionSnapshot merged = emptyRegion(name, cuboid.min(), cuboid.sizeX(), cuboid.sizeY(), cuboid.sizeZ());

            for (int y = cuboid.min().getY(); y <= cuboid.max().getY(); y++)
            {
                for (int z = cuboid.min().getZ(); z <= cuboid.max().getZ(); z++)
                {
                    for (int x = cuboid.min().getX(); x <= cuboid.max().getX(); x++)
                    {
                        BlockPos logical = new BlockPos(x, y, z);
                        CreatorRegionSnapshot source = candidates.get(logical);
                        BlockPos target = logical.subtract(cuboid.min());
                        copyUnitCell(source, merged, target);
                    }
                }
            }

            preserved.add(merged);
        }

        preserved.sort(REGION_ORDER);
        return withRecalculatedMetadata(this.metadata, preserved);
    }

    private CreatorSchematicSnapshot enclosing(
            @Nullable ClientLevel world,
            @Nullable SchematicPlacement samplingPlacement)
    {
        EnclosingBase base = this.createEnclosingBase();

        if (base == null)
        {
            return withRecalculatedMetadata(this.metadata, List.of());
        }

        if (world != null && samplingPlacement != null)
        {
            fillFromWorld(
                    base.output(),
                    base.covered(),
                    world,
                    CreatorWorldSamplingTransform.capture(samplingPlacement)
            );
        }

        return withRecalculatedMetadata(this.metadata, List.of(base.output()));
    }

    WorldSamplingPlan createWorldSamplingPlan(
            ClientLevel world,
            CreatorWorldSamplingTransform transform)
    {
        EnclosingBase base = this.createEnclosingBase();

        if (base == null)
        {
            return WorldSamplingPlan.completed(withRecalculatedMetadata(this.metadata, List.of()));
        }

        return new WorldSamplingPlan(
                copyMetadata(this.metadata),
                base.output(),
                base.covered(),
                world,
                transform
        );
    }

    @Nullable
    private EnclosingBase createEnclosingBase()
    {
        Bounds bounds = Bounds.of(this.regions);

        if (bounds == null)
        {
            return null;
        }

        BlockPos size = bounds.size();
        validateVolume(size);
        CreatorRegionSnapshot output = emptyRegion("creator_enclosing", bounds.min(), size.getX(), size.getY(), size.getZ());
        BitSet covered = new BitSet(Math.toIntExact((long) size.getX() * size.getY() * size.getZ()));
        List<CreatorRegionSnapshot> sorted = new ArrayList<>(this.regions);
        sorted.sort(REGION_ORDER);

        for (CreatorRegionSnapshot source : sorted)
        {
            copyRegion(source, output, bounds.min(), covered);
        }

        return new EnclosingBase(output, covered);
    }

    private LitematicaSchematic toSchematic()
    {
        LitematicaSchematic schematic = LitematicaSchematicAccessor.litematicacreator$create(null);
        LitematicaSchematicAccessor accessor = (LitematicaSchematicAccessor) schematic;
        CreatorSchematicMetadataCopies.copy(schematic.getMetadata(), this.metadata);

        for (CreatorRegionSnapshot region : this.regions)
        {
            accessor.litematicacreator$getBlockContainers().put(region.name(), CreatorRegionSnapshot.copyContainer(region.blocks()));
            accessor.litematicacreator$getTileEntities().put(
                    region.name(),
                    CreatorLitematicaDataAdapter.restoreBlockEntities(region.blockEntities())
            );
            accessor.litematicacreator$getEntities().put(region.name(), restoreEntities(region.entities()));
            accessor.litematicacreator$getPendingBlockTicks().put(region.name(), new HashMap<>(region.blockTicks()));
            accessor.litematicacreator$getPendingFluidTicks().put(region.name(), new HashMap<>(region.fluidTicks()));
            accessor.litematicacreator$getSubRegionPositions().put(region.name(), region.position());
            accessor.litematicacreator$getSubRegionSizes().put(region.name(), region.signedSize());
        }

        return schematic;
    }

    private static void copyRegion(
            CreatorRegionSnapshot source,
            CreatorRegionSnapshot target,
            BlockPos enclosingMin,
            BitSet covered)
    {
        Vec3i size = source.blocks().getSize();
        BlockPos sourceMin = source.min();

        for (int y = 0; y < size.getY(); y++)
        {
            for (int z = 0; z < size.getZ(); z++)
            {
                for (int x = 0; x < size.getX(); x++)
                {
                    BlockPos targetPos = sourceMin.offset(x, y, z).subtract(enclosingMin);
                    target.blocks().set(targetPos.getX(), targetPos.getY(), targetPos.getZ(), source.blocks().get(x, y, z));
                    clearAttachedAt(target, targetPos);
                    covered.set(index(target.blocks().getSize(), targetPos));
                }
            }
        }

        BlockPos offset = sourceMin.subtract(enclosingMin);
        copyAttachedData(source, target, offset);
    }

    private static void copyUnitCell(CreatorRegionSnapshot source, CreatorRegionSnapshot target, BlockPos targetPos)
    {
        target.blocks().set(targetPos.getX(), targetPos.getY(), targetPos.getZ(), source.blocks().get(0, 0, 0));
        copyAttachedData(source, target, targetPos);
    }

    private static void copyAttachedData(CreatorRegionSnapshot source, CreatorRegionSnapshot target, BlockPos offset)
    {
        source.blockEntities().forEach((pos, tag) -> {
            BlockPos targetPos = pos.offset(offset);
            CompoundTag copy = tag.copy();
            NbtUtils.writeBlockPosToTag(targetPos, copy);
            target.blockEntities().put(targetPos, copy);
        });

        for (CreatorEntitySnapshot info : source.entities())
        {
            Vec3 targetPos = info.posVec().add(offset.getX(), offset.getY(), offset.getZ());
            CompoundTag copy = info.nbt().copy();
            NbtUtils.writeEntityPositionToTag(targetPos, copy);
            relocateAttachedEntityBlockPos(copy, offset);
            target.entities().add(new CreatorEntitySnapshot(targetPos, copy));
        }

        source.blockTicks().forEach((pos, tick) -> {
            BlockPos targetPos = pos.offset(offset);
            target.blockTicks().put(targetPos, relocateTick(tick, targetPos));
        });
        source.fluidTicks().forEach((pos, tick) -> {
            BlockPos targetPos = pos.offset(offset);
            target.fluidTicks().put(targetPos, relocateTick(tick, targetPos));
        });
    }

    private static void fillFromWorld(
            CreatorRegionSnapshot output,
            BitSet covered,
            ClientLevel world,
            CreatorWorldSamplingTransform transform)
    {
        Vec3i size = output.blocks().getSize();

        for (int y = 0; y < size.getY(); y++)
        {
            for (int z = 0; z < size.getZ(); z++)
            {
                for (int x = 0; x < size.getX(); x++)
                {
                    BlockPos local = new BlockPos(x, y, z);

                    if (covered.get(index(size, local)))
                    {
                        continue;
                    }

                    sampleWorldPosition(output, world, transform, local);
                }
            }
        }
    }

    private static void sampleWorldPosition(
            CreatorRegionSnapshot output,
            ClientLevel world,
            CreatorWorldSamplingTransform transform,
            BlockPos local)
    {
        BlockPos logical = output.position().offset(local);
        BlockPos worldPos = transform.toWorld(logical);

        if (!world.hasChunkAt(worldPos))
        {
            throw new IllegalStateException("World sampling reached an unloaded chunk at " + worldPos.toShortString());
        }

        var state = world.getBlockState(worldPos);
        output.blocks().set(local.getX(), local.getY(), local.getZ(), state);
        BlockEntity blockEntity = world.getBlockEntity(worldPos);

        if (blockEntity != null)
        {
            CompoundTag tag = blockEntity.saveWithFullMetadata(world.registryAccess());
            NbtUtils.writeBlockPosToTag(local, tag);
            output.blockEntities().put(local, tag);
        }
    }

    private static void clearAttachedAt(CreatorRegionSnapshot target, BlockPos pos)
    {
        target.blockEntities().remove(pos);
        target.blockTicks().remove(pos);
        target.fluidTicks().remove(pos);
    }

    private static void relocateAttachedEntityBlockPos(CompoundTag tag, BlockPos offset)
    {
        if (tag.contains("TileX") && tag.contains("TileY") && tag.contains("TileZ"))
        {
            tag.putInt("TileX", tag.getIntOr("TileX", 0) + offset.getX());
            tag.putInt("TileY", tag.getIntOr("TileY", 0) + offset.getY());
            tag.putInt("TileZ", tag.getIntOr("TileZ", 0) + offset.getZ());
        }

        tag.read("block_pos", BlockPos.CODEC).ifPresent(pos -> tag.store("block_pos", BlockPos.CODEC, pos.offset(offset)));
    }

    private static <T> ScheduledTick<T> relocateTick(ScheduledTick<T> tick, BlockPos targetPos)
    {
        return new ScheduledTick<>(tick.type(), targetPos, tick.triggerTick(), tick.priority(), tick.subTickOrder());
    }

    private static boolean isCreatorCell(CreatorRegionSnapshot region)
    {
        return region.name().startsWith(CreatorSchematicEditor.CELL_REGION_PREFIX) && region.isUnitCell();
    }

    private static boolean overlapsAny(BlockPos pos, List<CreatorRegionSnapshot> regions)
    {
        for (CreatorRegionSnapshot region : regions)
        {
            BlockPos min = region.min();
            BlockPos max = region.max();

            if (pos.getX() >= min.getX() && pos.getX() <= max.getX() &&
                pos.getY() >= min.getY() && pos.getY() <= max.getY() &&
                pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ())
            {
                return true;
            }
        }

        return false;
    }

    private static CreatorRegionSnapshot emptyRegion(String name, BlockPos position, int sizeX, int sizeY, int sizeZ)
    {
        return new CreatorRegionSnapshot(
                name,
                position,
                new BlockPos(sizeX, sizeY, sizeZ),
                new LitematicaBlockStateContainer(sizeX, sizeY, sizeZ),
                new HashMap<>(),
                new ArrayList<>(),
                new HashMap<>(),
                new HashMap<>()
        );
    }

    private static List<CreatorEntitySnapshot> snapshotEntities(List<LitematicaSchematic.EntityInfo> source)
    {
        List<CreatorEntitySnapshot> snapshots = new ArrayList<>(source.size());

        for (LitematicaSchematic.EntityInfo entity : source)
        {
            snapshots.add(new CreatorEntitySnapshot(
                    entity.posVec(),
                    CreatorLitematicaDataAdapter.snapshotEntityNbt(entity)
            ));
        }

        return snapshots;
    }

    private static List<LitematicaSchematic.EntityInfo> restoreEntities(List<CreatorEntitySnapshot> source)
    {
        List<LitematicaSchematic.EntityInfo> entities = new ArrayList<>(source.size());

        for (CreatorEntitySnapshot snapshot : source)
        {
            entities.add(CreatorLitematicaDataAdapter.createEntity(snapshot.posVec(), snapshot.nbt()));
        }

        return entities;
    }

    private static CreatorSchematicSnapshot withRecalculatedMetadata(
            SchematicMetadata sourceMetadata,
            List<CreatorRegionSnapshot> regions)
    {
        SchematicMetadata metadata = copyMetadata(sourceMetadata);
        Bounds bounds = Bounds.of(regions);
        long volume = 0L;
        int blocks = 0;

        for (CreatorRegionSnapshot region : regions)
        {
            Vec3i size = region.blocks().getSize();
            volume += (long) size.getX() * size.getY() * size.getZ();
            blocks += countBlocks(region.blocks());
        }

        metadata.setRegionCount(regions.size());
        metadata.setTotalVolume((int) Math.min(Integer.MAX_VALUE, volume));
        metadata.setTotalBlocks(blocks);
        metadata.setEnclosingSize(bounds != null ? bounds.size() : BlockPos.ZERO);
        return new CreatorSchematicSnapshot(metadata, regions);
    }

    private static int countBlocks(LitematicaBlockStateContainer container)
    {
        Vec3i size = container.getSize();
        int count = 0;

        for (int y = 0; y < size.getY(); y++)
        {
            for (int z = 0; z < size.getZ(); z++)
            {
                for (int x = 0; x < size.getX(); x++)
                {
                    if (!container.get(x, y, z).isAir())
                    {
                        count++;
                    }
                }
            }
        }

        return count;
    }

    private static int index(Vec3i size, BlockPos pos)
    {
        return (pos.getY() * size.getZ() + pos.getZ()) * size.getX() + pos.getX();
    }

    private static void validateVolume(BlockPos size)
    {
        long volume = (long) size.getX() * size.getY() * size.getZ();

        if (size.getX() <= 0 || size.getY() <= 0 || size.getZ() <= 0 || volume > Integer.MAX_VALUE)
        {
            throw new IllegalArgumentException("Export bounds are too large: " + size.toShortString());
        }
    }

    private static SchematicMetadata copyMetadata(SchematicMetadata source)
    {
        SchematicMetadata copy = new SchematicMetadata();
        CreatorSchematicMetadataCopies.copy(copy, source);
        return copy;
    }

    private static List<CreatorRegionSnapshot> copyRegions(List<CreatorRegionSnapshot> source)
    {
        return source.stream().map(CreatorRegionSnapshot::copy).toList();
    }

    private record EnclosingBase(CreatorRegionSnapshot output, BitSet covered)
    {
    }

    static final class WorldSamplingPlan
    {
        private final SchematicMetadata metadata;
        @Nullable private final CreatorRegionSnapshot output;
        @Nullable private final BitSet covered;
        @Nullable private final ClientLevel world;
        @Nullable private final CreatorWorldSamplingTransform transform;
        private final CompletableFuture<CreatorSchematicSnapshot> future = new CompletableFuture<>();
        private int cursor;
        private final int totalVolume;

        private WorldSamplingPlan(
                SchematicMetadata metadata,
                CreatorRegionSnapshot output,
                BitSet covered,
                ClientLevel world,
                CreatorWorldSamplingTransform transform)
        {
            this.metadata = metadata;
            this.output = output;
            this.covered = covered;
            this.world = world;
            this.transform = transform;
            Vec3i size = output.blocks().getSize();
            this.totalVolume = Math.toIntExact((long) size.getX() * size.getY() * size.getZ());
        }

        private WorldSamplingPlan(CreatorSchematicSnapshot completed)
        {
            this.metadata = completed.metadata;
            this.output = null;
            this.covered = null;
            this.world = null;
            this.transform = null;
            this.totalVolume = 0;
            this.future.complete(completed);
        }

        static WorldSamplingPlan completed(CreatorSchematicSnapshot result)
        {
            return new WorldSamplingPlan(result);
        }

        CompletableFuture<CreatorSchematicSnapshot> future()
        {
            return this.future;
        }

        boolean isDone()
        {
            return this.future.isDone();
        }

        void runFor(Minecraft minecraft, long deadlineNanos)
        {
            if (this.future.isDone())
            {
                return;
            }

            if (minecraft.level != this.world || this.output == null || this.covered == null || this.transform == null)
            {
                this.future.completeExceptionally(new IllegalStateException("The client world changed during schematic sampling"));
                return;
            }

            try
            {
                Vec3i size = this.output.blocks().getSize();

                while (this.cursor < this.totalVolume)
                {
                    if (System.nanoTime() >= deadlineNanos)
                    {
                        return;
                    }

                    int current = this.cursor++;

                    if (this.covered.get(current))
                    {
                        continue;
                    }

                    int x = current % size.getX();
                    int remainder = current / size.getX();
                    int z = remainder % size.getZ();
                    int y = remainder / size.getZ();
                    sampleWorldPosition(this.output, this.world, this.transform, new BlockPos(x, y, z));
                }

                this.future.complete(withRecalculatedMetadata(this.metadata, List.of(this.output)));
            }
            catch (Throwable error)
            {
                this.future.completeExceptionally(error);
            }
        }
    }

    private record Bounds(BlockPos min, BlockPos max)
    {
        @Nullable
        static Bounds of(List<CreatorRegionSnapshot> regions)
        {
            if (regions.isEmpty())
            {
                return null;
            }

            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;

            for (CreatorRegionSnapshot region : regions)
            {
                BlockPos min = region.min();
                BlockPos max = region.max();
                minX = Math.min(minX, min.getX());
                minY = Math.min(minY, min.getY());
                minZ = Math.min(minZ, min.getZ());
                maxX = Math.max(maxX, max.getX());
                maxY = Math.max(maxY, max.getY());
                maxZ = Math.max(maxZ, max.getZ());
            }

            return new Bounds(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ));
        }

        BlockPos size()
        {
            return this.max.subtract(this.min).offset(1, 1, 1);
        }
    }
}
