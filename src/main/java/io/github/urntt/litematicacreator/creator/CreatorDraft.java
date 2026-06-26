package io.github.urntt.litematicacreator.creator;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;

public class CreatorDraft
{
    private final LitematicaSchematic schematic;
    private final SchematicPlacement placement;
    private final BlockPos anchor;
    private final Map<CreatorTilePos, String> regionNamesByTile = new HashMap<>();
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

    public BlockPos toLocalInRegion(BlockPos worldPos)
    {
        BlockPos regionMin = CreatorTilePos.fromWorldPos(worldPos).minBlockPos();
        return worldPos.subtract(regionMin);
    }
}
