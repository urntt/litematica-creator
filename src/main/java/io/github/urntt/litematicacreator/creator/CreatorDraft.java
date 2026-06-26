package io.github.urntt.litematicacreator.creator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.SchematicMetadata;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.malilib.util.InfoUtils;
import io.github.urntt.litematicacreator.mixin.LitematicaSchematicAccessor;
import io.github.urntt.litematicacreator.mixin.SchematicPlacementAccessor;

public class CreatorDraft
{
    private final LitematicaSchematic schematic;
    private final SchematicPlacement placement;
    private final BlockPos anchor;
    private final Map<CreatorTilePos, String> regionNamesByTile = new HashMap<>();
    private int totalBlocks;
    private boolean dirty;

    private CreatorDraft(LitematicaSchematic schematic, SchematicPlacement placement, BlockPos anchor)
    {
        this.schematic = schematic;
        this.placement = placement;
        this.anchor = anchor;
    }

    @Nullable
    public static CreatorDraft create(String name, BlockPos seedWorldPos, String author)
    {
        CreatorTilePos tilePos = CreatorTilePos.fromWorldPos(seedWorldPos);
        String regionName = tilePos.regionName();
        BlockPos anchor = tilePos.minBlockPos();
        BlockPos max = anchor.offset(CreatorTilePos.TILE_SIZE - 1, CreatorTilePos.TILE_SIZE - 1, CreatorTilePos.TILE_SIZE - 1);

        AreaSelection area = new AreaSelection();
        area.setName(name);
        area.setExplicitOrigin(anchor);
        area.addSubRegionBox(new Box(anchor, max, regionName), true);

        LitematicaSchematic schematic = LitematicaSchematic.createEmptySchematic(area, author);

        if (schematic == null)
        {
            return null;
        }

        SchematicPlacement placement = SchematicPlacement.createFor(schematic, anchor, name, true, true);
        CreatorDraft draft = new CreatorDraft(schematic, placement, anchor);
        draft.regionNamesByTile.put(tilePos, regionName);

        SchematicHolder.getInstance().addSchematic(schematic, false);
        DataManager.getSchematicPlacementManager().addSchematicPlacement(placement, false);
        DataManager.getSchematicPlacementManager().markChunksForRebuild(placement);

        return draft;
    }

    public LitematicaSchematic getSchematic()
    {
        return this.schematic;
    }

    public SchematicPlacement getPlacement()
    {
        return this.placement;
    }

    public BlockPos getAnchor()
    {
        return this.anchor;
    }

    public boolean isDirty()
    {
        return this.dirty;
    }

    public int getTileCount()
    {
        return this.regionNamesByTile.size();
    }

    public int getTotalBlocks()
    {
        return this.totalBlocks;
    }

    public void markDirty()
    {
        this.dirty = true;
        this.schematic.getMetadata().setModifiedSinceSaved();
        this.schematic.getMetadata().setTimeModifiedToNow();
    }

    public void markSaved()
    {
        this.dirty = false;
        this.schematic.getMetadata().clearModifiedSinceSaved();
    }

    @Nullable
    public String getRegionNameForWorldPos(BlockPos worldPos)
    {
        return this.regionNamesByTile.get(CreatorTilePos.fromWorldPos(worldPos));
    }

    @Nullable
    public LitematicaBlockStateContainer getContainerForWorldPos(BlockPos worldPos)
    {
        String regionName = this.getRegionNameForWorldPos(worldPos);
        return regionName != null ? this.schematic.getSubRegionContainer(regionName) : null;
    }

    public BlockState getBlockState(BlockPos worldPos)
    {
        LitematicaBlockStateContainer container = this.getContainerForWorldPos(worldPos);

        if (container == null)
        {
            return Blocks.AIR.defaultBlockState();
        }

        BlockPos localPos = this.toLocalInRegion(worldPos);
        return container.get(localPos.getX(), localPos.getY(), localPos.getZ());
    }

    public void setBlockState(BlockPos worldPos, BlockState state)
    {
        LitematicaBlockStateContainer container = this.ensureTileForWorldPos(worldPos);
        BlockPos localPos = this.toLocalInRegion(worldPos);
        BlockState oldState = container.get(localPos.getX(), localPos.getY(), localPos.getZ());

        if (oldState.equals(state))
        {
            return;
        }

        if (oldState.isAir() && !state.isAir())
        {
            ++this.totalBlocks;
        }
        else if (!oldState.isAir() && state.isAir())
        {
            --this.totalBlocks;
        }

        container.set(localPos.getX(), localPos.getY(), localPos.getZ(), state);
        this.updateMetadata();
        this.markDirty();
        DataManager.getSchematicPlacementManager().markChunkForRebuild(new ChunkPos(worldPos.getX() >> 4, worldPos.getZ() >> 4));
    }

    private LitematicaBlockStateContainer ensureTileForWorldPos(BlockPos worldPos)
    {
        CreatorTilePos tilePos = CreatorTilePos.fromWorldPos(worldPos);
        String regionName = this.regionNamesByTile.get(tilePos);
        LitematicaSchematicAccessor accessor = (LitematicaSchematicAccessor) this.schematic;

        if (regionName != null)
        {
            return accessor.litematicacreator$getBlockContainers().get(regionName);
        }

        regionName = tilePos.regionName();
        BlockPos regionPosition = tilePos.minBlockPos().subtract(this.anchor);
        BlockPos regionSize = new BlockPos(CreatorTilePos.TILE_SIZE, CreatorTilePos.TILE_SIZE, CreatorTilePos.TILE_SIZE);

        accessor.litematicacreator$getBlockContainers().put(regionName, new LitematicaBlockStateContainer(CreatorTilePos.TILE_SIZE, CreatorTilePos.TILE_SIZE, CreatorTilePos.TILE_SIZE));
        accessor.litematicacreator$getTileEntities().put(regionName, new HashMap<>());
        accessor.litematicacreator$getPendingBlockTicks().put(regionName, new HashMap<>());
        accessor.litematicacreator$getPendingFluidTicks().put(regionName, new HashMap<>());
        accessor.litematicacreator$getEntities().put(regionName, new ArrayList<>());
        accessor.litematicacreator$getSubRegionPositions().put(regionName, regionPosition);
        accessor.litematicacreator$getSubRegionSizes().put(regionName, regionSize);

        this.regionNamesByTile.put(tilePos, regionName);
        ((SchematicPlacementAccessor) this.placement).litematicacreator$setSubRegionCount(this.schematic.getSubRegionCount());
        this.placement.resetAllSubRegionsToSchematicValues(InfoUtils.INFO_MESSAGE_CONSUMER);
        this.updateMetadata();

        return accessor.litematicacreator$getBlockContainers().get(regionName);
    }

    public BlockPos toLocalInRegion(BlockPos worldPos)
    {
        BlockPos regionMin = CreatorTilePos.fromWorldPos(worldPos).minBlockPos();
        return worldPos.subtract(regionMin);
    }

    private void updateMetadata()
    {
        LitematicaSchematicAccessor accessor = (LitematicaSchematicAccessor) this.schematic;
        SchematicMetadata metadata = this.schematic.getMetadata();
        Map<String, BlockPos> positions = accessor.litematicacreator$getSubRegionPositions();
        Map<String, BlockPos> sizes = accessor.litematicacreator$getSubRegionSizes();

        int minX = 0;
        int minY = 0;
        int minZ = 0;
        int maxX = 0;
        int maxY = 0;
        int maxZ = 0;
        boolean first = true;

        for (Map.Entry<String, BlockPos> entry : positions.entrySet())
        {
            BlockPos pos = entry.getValue();
            BlockPos size = sizes.get(entry.getKey());

            if (size == null)
            {
                continue;
            }

            int endX = pos.getX() + size.getX() - 1;
            int endY = pos.getY() + size.getY() - 1;
            int endZ = pos.getZ() + size.getZ() - 1;

            if (first)
            {
                minX = pos.getX();
                minY = pos.getY();
                minZ = pos.getZ();
                maxX = endX;
                maxY = endY;
                maxZ = endZ;
                first = false;
            }
            else
            {
                minX = Math.min(minX, pos.getX());
                minY = Math.min(minY, pos.getY());
                minZ = Math.min(minZ, pos.getZ());
                maxX = Math.max(maxX, endX);
                maxY = Math.max(maxY, endY);
                maxZ = Math.max(maxZ, endZ);
            }
        }

        metadata.setRegionCount(this.schematic.getSubRegionCount());
        metadata.setTotalVolume(this.schematic.getSubRegionCount() * CreatorTilePos.TILE_SIZE * CreatorTilePos.TILE_SIZE * CreatorTilePos.TILE_SIZE);
        metadata.setTotalBlocks(this.totalBlocks);

        if (!first)
        {
            metadata.setEnclosingSize(new BlockPos(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1));
        }
    }
}
