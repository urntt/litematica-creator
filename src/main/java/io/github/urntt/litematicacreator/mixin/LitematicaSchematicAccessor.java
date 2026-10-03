package io.github.urntt.litematicacreator.mixin;

import java.util.List;
import java.util.Map;
import java.nio.file.Path;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.ScheduledTick;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.util.FileType;
import fi.dy.masa.malilib.util.data.tag.CompoundData;

@Mixin(LitematicaSchematic.class)
public interface LitematicaSchematicAccessor
{
    @Invoker("<init>")
    static LitematicaSchematic litematicacreator$create(@Nullable Path file)
    {
        throw new AssertionError();
    }

    @Mutable
    @Accessor("schematicFile")
    void litematicacreator$setSchematicFile(@Nullable Path file);

    @Accessor("schematicType")
    FileType litematicacreator$getSchematicType();

    @Mutable
    @Accessor("schematicType")
    void litematicacreator$setSchematicType(FileType type);

    @Accessor("blockContainers")
    Map<String, LitematicaBlockStateContainer> litematicacreator$getBlockContainers();

    @Accessor("tileEntities")
    Map<String, Map<BlockPos, CompoundData>> litematicacreator$getTileEntities();

    @Accessor("pendingBlockTicks")
    Map<String, Map<BlockPos, ScheduledTick<Block>>> litematicacreator$getPendingBlockTicks();

    @Accessor("pendingFluidTicks")
    Map<String, Map<BlockPos, ScheduledTick<Fluid>>> litematicacreator$getPendingFluidTicks();

    @Accessor("entities")
    Map<String, List<LitematicaSchematic.EntityInfo>> litematicacreator$getEntities();

    @Accessor("subRegionPositions")
    Map<String, BlockPos> litematicacreator$getSubRegionPositions();

    @Accessor("subRegionSizes")
    Map<String, BlockPos> litematicacreator$getSubRegionSizes();
}
