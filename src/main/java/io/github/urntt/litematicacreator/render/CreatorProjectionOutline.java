package io.github.urntt.litematicacreator.render;

import javax.annotation.Nullable;

import net.minecraft.SharedConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.util.GuiUtils;
import io.github.urntt.litematicacreator.creator.CreatorManager;
import io.github.urntt.litematicacreator.creator.CreatorPlacementIndex;
import io.github.urntt.litematicacreator.creator.CreatorTargeting;

public final class CreatorProjectionOutline
{
    private CreatorProjectionOutline()
    {
    }

    public static void replaceIfNeeded(Minecraft mc, Camera camera, LevelRenderState output)
    {
        if (!isActive(CreatorManager.getInstance().isCreatorModeEnabled(), GuiUtils.getCurrentScreen() != null))
        {
            return;
        }

        @Nullable RayTraceWrapper trace = CreatorTargeting.trace(mc);

        if (trace == null || trace.getHitType() != RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
        {
            return;
        }

        @Nullable BlockHitResult hit = trace.getBlockHitResult();

        if (hit == null)
        {
            return;
        }

        BlockPos pos = hit.getBlockPos();

        @Nullable WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();

        if (schematicWorld == null)
        {
            return;
        }

        BlockState state = schematicWorld.getBlockState(pos);

        if (!isRenderableTarget(!CreatorPlacementIndex.INSTANCE.findAt(pos).isEmpty(), !state.isAir()))
        {
            return;
        }

        BlockStateModel model = mc.getModelManager().getBlockStateModelSet().get(state);
        boolean translucent = model.hasMaterialFlag(1);
        boolean highContrast = mc.options.highContrastBlockOutline().get();
        CollisionContext context = CollisionContext.of(camera.entity());
        VoxelShape shape = state.getShape(schematicWorld, pos, context);

        if (SharedConstants.DEBUG_SHAPES)
        {
            output.blockOutlineRenderState = new BlockOutlineRenderState(
                    pos,
                    translucent,
                    highContrast,
                    shape,
                    state.getCollisionShape(schematicWorld, pos, context),
                    state.getOcclusionShape(),
                    state.getInteractionShape(schematicWorld, pos)
            );
        }
        else
        {
            output.blockOutlineRenderState = new BlockOutlineRenderState(pos, translucent, highContrast, shape);
        }
    }

    static boolean isActive(boolean creatorModeEnabled, boolean screenOpen)
    {
        return creatorModeEnabled && !screenOpen;
    }

    static boolean isRenderableTarget(boolean indexedPlacement, boolean nonAirState)
    {
        return indexedPlacement && nonAirState;
    }
}
