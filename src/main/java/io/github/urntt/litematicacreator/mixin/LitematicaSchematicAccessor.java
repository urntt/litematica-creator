package io.github.urntt.litematicacreator.mixin;

import java.util.List;
import java.util.Map;
import java.nio.file.Path;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.ScheduledTick;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;

@Mixin(LitematicaSchematic.class)
public interface LitematicaSchematicAccessor
{
    @Invoker("<init>")
    static LitematicaSchematic litematicacreator$create(@Nullable Path file)
    {
        throw new AssertionError();
    }

    @Accessor("blockContainers")
    Map<String, LitematicaBlockStateContainer> litematicacreator$getBlockContainers();

    @Accessor("tileEntities")
    Map<String, Map<BlockPos, CompoundTag>> litematicacreator$getTileEntities();

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
