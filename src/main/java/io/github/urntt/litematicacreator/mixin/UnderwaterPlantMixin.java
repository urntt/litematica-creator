package io.github.urntt.litematicacreator.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.KelpBlock;
import net.minecraft.world.level.block.SeagrassBlock;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import io.github.urntt.litematicacreator.creator.CreatorPlacementWorld;

@Mixin({SeagrassBlock.class, KelpBlock.class})
public abstract class UnderwaterPlantMixin
{
    // Seagrass and kelp only place into a full water cell. Projections cannot hold placed water yet, and these blocks
    // carry their own water, so in a projection the target cell counts as water.
    @WrapOperation(
            method = "getStateForPlacement",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;"
            )
    )
    private FluidState litematicacreator$waterInPlacementWorld(Level level, BlockPos pos, Operation<FluidState> original)
    {
        return level instanceof CreatorPlacementWorld ? Fluids.WATER.getSource(false) : original.call(level, pos);
    }
}
