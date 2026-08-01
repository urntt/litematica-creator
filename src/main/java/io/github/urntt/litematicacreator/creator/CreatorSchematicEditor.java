package io.github.urntt.litematicacreator.creator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.SchematicMetadata;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementEventHandler;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.SchematicUtils;
import io.github.urntt.litematicacreator.mixin.LitematicaSchematicAccessor;
import io.github.urntt.litematicacreator.mixin.SchematicPlacementAccessor;
import io.github.urntt.litematicacreator.recovery.CreatorRecoveryManager;

public final class CreatorSchematicEditor
{
    public static final String CELL_REGION_PREFIX = "__litematica_creator_cell_";

    private CreatorSchematicEditor()
    {
    }

    public static boolean setBlockState(SchematicPlacement placement, BlockPos worldPos, BlockState worldState)
    {
        @Nullable CreatorPlacementTarget target = findRegionAt(placement, worldPos);

        if (target == null)
        {
            if (worldState.isAir())
            {
                return false;
            }

            addCellRegion(placement, worldPos, worldState);
            return true;
        }

        LitematicaSchematic schematic = placement.getSchematic();
        String regionName = target.regionName();
        LitematicaBlockStateContainer container = schematic.getSubRegionContainer(regionName);
        SubRegionPlacement regionPlacement = placement.getRelativeSubRegionPlacement(regionName);

        if (container == null || regionPlacement == null)
        {
            return false;
        }

        BlockPos containerPos = SchematicUtils.getSchematicContainerPositionFromWorldPosition(
                worldPos,
                schematic,
                regionName,
                placement,
                regionPlacement,
                container
        );

        if (containerPos == null)
        {
            return false;
        }

        BlockState oldState = container.get(containerPos.getX(), containerPos.getY(), containerPos.getZ());
        BlockState newState = SchematicUtils.getUntransformedBlockState(worldState, placement, regionName);

        if (oldState.equals(newState))
        {
            return false;
        }

        container.set(containerPos.getX(), containerPos.getY(), containerPos.getZ(), newState);
        updateBlockCount(schematic.getMetadata(), oldState, newState);

        if (newState.isAir() && isRegionCompletelyEmpty(schematic, regionName, container))
        {
            removeRegion(schematic, regionName);
        }
        else
        {
            markModified(schematic);
            rebuildAllPlacements(schematic);
        }

        return true;
    }

    public static BlockState getBlockState(SchematicPlacement placement, BlockPos worldPos)
    {
        @Nullable CreatorPlacementTarget target = findRegionAt(placement, worldPos);

        if (target == null)
        {
            return Blocks.AIR.defaultBlockState();
        }

        LitematicaSchematic schematic = placement.getSchematic();
        String regionName = target.regionName();
        LitematicaBlockStateContainer container = schematic.getSubRegionContainer(regionName);
        SubRegionPlacement regionPlacement = placement.getRelativeSubRegionPlacement(regionName);

        if (container == null || regionPlacement == null)
        {
            return Blocks.AIR.defaultBlockState();
        }

        BlockPos containerPos = SchematicUtils.getSchematicContainerPositionFromWorldPosition(
                worldPos,
                schematic,
                regionName,
                placement,
                regionPlacement,
                container
        );

        if (containerPos == null)
        {
            return Blocks.AIR.defaultBlockState();
        }

        return toWorldBlockState(
                container.get(containerPos.getX(), containerPos.getY(), containerPos.getZ()),
                placement,
                regionPlacement
        );
    }

    @Nullable
    public static CreatorPlacementTarget findRegionAt(SchematicPlacement placement, BlockPos worldPos)
    {
        for (Map.Entry<String, Box> entry : placement.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED).entrySet())
        {
            if (contains(entry.getValue(), worldPos))
            {
                return new CreatorPlacementTarget(placement, entry.getKey(), entry.getValue());
            }
        }

        return null;
    }

    private static void addCellRegion(SchematicPlacement editedPlacement, BlockPos worldPos, BlockState worldState)
    {
        LitematicaSchematic schematic = editedPlacement.getSchematic();
        LitematicaSchematicAccessor schematicAccessor = (LitematicaSchematicAccessor) schematic;
        BlockPos relativePos = CreatorCoordinateTransforms.toSchematicRelative(
                worldPos,
                editedPlacement.getOrigin(),
                editedPlacement.getMirror(),
                editedPlacement.getRotation()
        );
        String regionName = uniqueCellRegionName(schematic, relativePos);
        LitematicaBlockStateContainer container = new LitematicaBlockStateContainer(1, 1, 1);
        List<SchematicPlacement> placements = new ArrayList<>();
        placements.add(editedPlacement);

        for (SchematicPlacement placement : DataManager.getSchematicPlacementManager().getAllPlacementsOfSchematic(schematic))
        {
            if (placement != editedPlacement)
            {
                placements.add(placement);
            }
        }

        schematicAccessor.litematicacreator$getBlockContainers().put(regionName, container);
        schematicAccessor.litematicacreator$getTileEntities().put(regionName, new HashMap<>());
        schematicAccessor.litematicacreator$getPendingBlockTicks().put(regionName, new HashMap<>());
        schematicAccessor.litematicacreator$getPendingFluidTicks().put(regionName, new HashMap<>());
        schematicAccessor.litematicacreator$getEntities().put(regionName, new ArrayList<>());
        schematicAccessor.litematicacreator$getSubRegionPositions().put(regionName, relativePos);
        schematicAccessor.litematicacreator$getSubRegionSizes().put(regionName, new BlockPos(1, 1, 1));

        for (SchematicPlacement placement : placements)
        {
            SchematicPlacementEventHandler.getInstance().invokePrePlacementChange(CreatorPlacementIndex.INSTANCE, placement);
            SchematicPlacementAccessor placementAccessor = (SchematicPlacementAccessor) placement;
            placementAccessor.litematicacreator$getRelativeSubRegionPlacements().put(regionName, new SubRegionPlacement(relativePos, regionName));
            placementAccessor.litematicacreator$setSubRegionCount(schematic.getSubRegionCount());

            if (placement == editedPlacement)
            {
                container.set(0, 0, 0, SchematicUtils.getUntransformedBlockState(worldState, editedPlacement, regionName));
            }

            SchematicPlacementEventHandler.getInstance().invokePlacementModified(CreatorPlacementIndex.INSTANCE, placement);
        }

        schematic.getMetadata().setTotalBlocks(Math.max(0, schematic.getMetadata().getTotalBlocks()) + 1);
        refreshGeometryMetadata(schematic);
        markModified(schematic);
    }

    private static void removeRegion(LitematicaSchematic schematic, String regionName)
    {
        LitematicaSchematicAccessor schematicAccessor = (LitematicaSchematicAccessor) schematic;
        List<SchematicPlacement> placements = List.copyOf(DataManager.getSchematicPlacementManager().getAllPlacementsOfSchematic(schematic));
        int remainingRegionCount = Math.max(0, schematic.getSubRegionCount() - 1);

        for (SchematicPlacement placement : placements)
        {
            SchematicPlacementEventHandler.getInstance().invokePrePlacementChange(CreatorPlacementIndex.INSTANCE, placement);
            SchematicPlacementAccessor placementAccessor = (SchematicPlacementAccessor) placement;
            placementAccessor.litematicacreator$getRelativeSubRegionPlacements().remove(regionName);
            placementAccessor.litematicacreator$setSubRegionCount(remainingRegionCount);

            if (remainingRegionCount == 0)
            {
                placementAccessor.litematicacreator$setEnclosingBox(null);
            }

            SchematicPlacementEventHandler.getInstance().invokePlacementModified(CreatorPlacementIndex.INSTANCE, placement);
        }

        schematicAccessor.litematicacreator$getBlockContainers().remove(regionName);
        schematicAccessor.litematicacreator$getTileEntities().remove(regionName);
        schematicAccessor.litematicacreator$getPendingBlockTicks().remove(regionName);
        schematicAccessor.litematicacreator$getPendingFluidTicks().remove(regionName);
        schematicAccessor.litematicacreator$getEntities().remove(regionName);
        schematicAccessor.litematicacreator$getSubRegionPositions().remove(regionName);
        schematicAccessor.litematicacreator$getSubRegionSizes().remove(regionName);

        refreshGeometryMetadata(schematic);
        markModified(schematic);
    }

    private static void rebuildAllPlacements(LitematicaSchematic schematic)
    {
        for (SchematicPlacement placement : DataManager.getSchematicPlacementManager().getAllPlacementsOfSchematic(schematic))
        {
            DataManager.getSchematicPlacementManager().markChunksForRebuild(placement);
        }
    }

    private static void refreshGeometryMetadata(LitematicaSchematic schematic)
    {
        LitematicaSchematicAccessor accessor = (LitematicaSchematicAccessor) schematic;
        SchematicMetadata metadata = schematic.getMetadata();
        long volume = 0L;
        BlockPos min = null;
        BlockPos max = null;

        for (Map.Entry<String, BlockPos> entry : accessor.litematicacreator$getSubRegionPositions().entrySet())
        {
            BlockPos size = accessor.litematicacreator$getSubRegionSizes().get(entry.getKey());
            if (size == null)
            {
                continue;
            }

            volume += (long) Math.abs(size.getX()) * Math.abs(size.getY()) * Math.abs(size.getZ());
            BlockPos end = entry.getValue().offset(relativeEnd(size));
            BlockPos regionMin = min(entry.getValue(), end);
            BlockPos regionMax = max(entry.getValue(), end);
            min = min == null ? regionMin : min(min, regionMin);
            max = max == null ? regionMax : max(max, regionMax);
        }

        metadata.setRegionCount(schematic.getSubRegionCount());
        metadata.setTotalVolume((int) Math.min(Integer.MAX_VALUE, volume));
        metadata.setEnclosingSize(min == null ? Vec3i.ZERO : new BlockPos(max.getX() - min.getX() + 1, max.getY() - min.getY() + 1, max.getZ() - min.getZ() + 1));
    }

    private static void updateBlockCount(SchematicMetadata metadata, BlockState oldState, BlockState newState)
    {
        int totalBlocks = Math.max(0, metadata.getTotalBlocks());

        if (oldState.isAir() && !newState.isAir())
        {
            metadata.setTotalBlocks(totalBlocks + 1);
        }
        else if (!oldState.isAir() && newState.isAir())
        {
            metadata.setTotalBlocks(Math.max(0, totalBlocks - 1));
        }
    }

    private static void markModified(LitematicaSchematic schematic)
    {
        schematic.getMetadata().setModifiedSinceSaved();
        schematic.getMetadata().setTimeModifiedToNow();
        CreatorRecoveryManager.getInstance().onSchematicChanged(schematic);
    }

    private static boolean isRegionCompletelyEmpty(
            LitematicaSchematic schematic,
            String regionName,
            LitematicaBlockStateContainer container)
    {
        LitematicaSchematicAccessor accessor = (LitematicaSchematicAccessor) schematic;
        return CreatorRegionEmptiness.isCompletelyEmpty(
                container,
                accessor.litematicacreator$getTileEntities().get(regionName),
                accessor.litematicacreator$getEntities().get(regionName),
                accessor.litematicacreator$getPendingBlockTicks().get(regionName),
                accessor.litematicacreator$getPendingFluidTicks().get(regionName)
        );
    }

    private static BlockState toWorldBlockState(
            BlockState state,
            SchematicPlacement placement,
            SubRegionPlacement regionPlacement)
    {
        Rotation rotation = placement.getRotation().getRotated(regionPlacement.getRotation());
        Mirror mainMirror = placement.getMirror();
        Mirror regionMirror = regionPlacement.getMirror();

        if (regionMirror != Mirror.NONE &&
            (placement.getRotation() == Rotation.CLOCKWISE_90 ||
             placement.getRotation() == Rotation.COUNTERCLOCKWISE_90))
        {
            regionMirror = regionMirror == Mirror.FRONT_BACK ? Mirror.LEFT_RIGHT : Mirror.FRONT_BACK;
        }

        if (mainMirror != Mirror.NONE)
        {
            state = state.mirror(mainMirror);
        }

        if (regionMirror != Mirror.NONE)
        {
            state = state.mirror(regionMirror);
        }

        if (rotation != Rotation.NONE)
        {
            state = state.rotate(rotation);
        }

        return state;
    }

    private static String uniqueCellRegionName(LitematicaSchematic schematic, BlockPos relativePos)
    {
        String base = CELL_REGION_PREFIX + relativePos.getX() + "_" + relativePos.getY() + "_" + relativePos.getZ();
        String name = base;
        int suffix = 2;

        while (schematic.getAreaSize(name) != null)
        {
            name = base + "_" + suffix++;
        }

        return name;
    }

    private static BlockPos relativeEnd(BlockPos size)
    {
        return new BlockPos(relativeEnd(size.getX()), relativeEnd(size.getY()), relativeEnd(size.getZ()));
    }

    private static int relativeEnd(int size)
    {
        return size > 0 ? size - 1 : size + 1;
    }

    private static BlockPos min(BlockPos a, BlockPos b)
    {
        return new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
    }

    private static BlockPos max(BlockPos a, BlockPos b)
    {
        return new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
    }

    private static boolean contains(Box box, BlockPos pos)
    {
        return pos.getX() >= Math.min(box.getPos1().getX(), box.getPos2().getX()) &&
               pos.getX() <= Math.max(box.getPos1().getX(), box.getPos2().getX()) &&
               pos.getY() >= Math.min(box.getPos1().getY(), box.getPos2().getY()) &&
               pos.getY() <= Math.max(box.getPos1().getY(), box.getPos2().getY()) &&
               pos.getZ() >= Math.min(box.getPos1().getZ(), box.getPos2().getZ()) &&
               pos.getZ() <= Math.max(box.getPos1().getZ(), box.getPos2().getZ());
    }
}
