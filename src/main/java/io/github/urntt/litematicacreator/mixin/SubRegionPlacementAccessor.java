package io.github.urntt.litematicacreator.mixin;

import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;

@Mixin(SubRegionPlacement.class)
public interface SubRegionPlacementAccessor
{
    @Mutable
    @Accessor("defaultPos")
    void litematicacreator$setDefaultPos(BlockPos position);

    @Accessor("pos")
    void litematicacreator$setPos(BlockPos position);
}
