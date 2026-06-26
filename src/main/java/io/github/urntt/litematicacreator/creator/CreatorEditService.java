package io.github.urntt.litematicacreator.creator;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import fi.dy.masa.litematica.mixin.entity.IMixinEntity;
import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import io.github.urntt.litematicacreator.event.CreatorClientTickHandler;

public class CreatorEditService
{
    private static final CreatorEditService INSTANCE = new CreatorEditService();
    private static final double EDIT_RANGE = 10.0D;
    private static final int PLACE_INTERVAL_TICKS = 4;
    private static final long NO_TARGET_MESSAGE_INTERVAL_MS = 1500L;

    private long nextPlaceTick;
    private long lastNoTargetWarning;

    private CreatorEditService()
    {
    }

    public static CreatorEditService getInstance()
    {
        return INSTANCE;
    }

    public boolean placeProjectionBlock()
    {
        Minecraft mc = Minecraft.getInstance();

        if (!this.canEdit(mc))
        {
            return false;
        }

        if (!this.canPlaceNow())
        {
            return true;
        }

        this.nextPlaceTick = CreatorClientTickHandler.getClientTicks() + PLACE_INTERVAL_TICKS;

        @Nullable CreatorTarget target = this.getPlacementTarget(mc);

        if (target == null)
        {
            this.showNoTargetWarningThrottled();
            return true;
        }

        ItemStack stack = CreatorInventory.getInstance().getSelectedStack();

        if (!(stack.getItem() instanceof BlockItem blockItem))
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica-creator.message.edit.no_block_selected");
            return true;
        }

        CreatorDraft draft = CreatorManager.getInstance().getOrCreateDraft(target.blockPos());

        if (draft == null)
        {
            return true;
        }

        BlockState state = this.getPlacementState(mc, blockItem, stack, target);

        if (state == null || state.isAir())
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica-creator.message.edit.no_place_state");
            return true;
        }

        draft.setBlockState(target.blockPos(), state);
        return true;
    }

    public boolean deleteProjectionBlock()
    {
        Minecraft mc = Minecraft.getInstance();

        if (!this.canEdit(mc))
        {
            return false;
        }

        @Nullable CreatorTarget target = this.getExistingCreatorTarget(mc);

        if (target != null)
        {
            CreatorManager.getInstance().getCurrentDraft().setBlockState(target.blockPos(), Blocks.AIR.defaultBlockState());
        }

        return true;
    }

    public boolean pickBlock()
    {
        Minecraft mc = Minecraft.getInstance();

        if (!this.canEdit(mc))
        {
            return false;
        }

        @Nullable CreatorTarget target = this.getPickTarget(mc);

        if (target == null)
        {
            return true;
        }

        BlockState state;
        CreatorDraft draft = CreatorManager.getInstance().getCurrentDraft();

        if (target.schematicBlock() && draft != null)
        {
            state = draft.getBlockState(target.blockPos());
        }
        else
        {
            state = mc.level.getBlockState(target.blockPos());
        }

        if (!CreatorInventory.getInstance().pickBlock(state))
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica-creator.message.edit.pick_failed");
        }

        return true;
    }

    @Nullable
    private BlockState getPlacementState(Minecraft mc, BlockItem blockItem, ItemStack stack, CreatorTarget target)
    {
        Level schematicWorld = SchematicWorldHandler.getSchematicWorld();

        if (schematicWorld == null || mc.player == null)
        {
            return blockItem.getBlock().defaultBlockState();
        }

        Level oldWorld = mc.player.level();
        float oldYaw = mc.player.getYRot();
        float oldPitch = mc.player.getXRot();
        Entity camera = CreatorCameraCompat.getCameraEntity();
        BlockHitResult hit = new BlockHitResult(target.hitVec(), target.side(), target.clickedBlockPos(), false);

        try
        {
            ((IMixinEntity) mc.player).litematica_setWorld(schematicWorld);
            // Block placement state helpers read the player rotation from the context player.
            if (camera != null)
            {
                EntityUtils.setEntityRotations(mc.player, camera.getYRot(), camera.getXRot());
            }

            BlockPlaceContext context = new BlockPlaceContext(mc.player, InteractionHand.MAIN_HAND, stack, hit);
            return blockItem.getBlock().getStateForPlacement(context);
        }
        finally
        {
            EntityUtils.setEntityRotations(mc.player, oldYaw, oldPitch);
            ((IMixinEntity) mc.player).litematica_setWorld(oldWorld);
        }
    }

    @Nullable
    private CreatorTarget getPlacementTarget(Minecraft mc)
    {
        RayTraceWrapper trace = this.trace(mc);

        if (trace == null || trace.getBlockHitResult() == null)
        {
            return null;
        }

        BlockHitResult hit = trace.getBlockHitResult();

        if (trace.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK && this.isCurrentDraftBlock(hit.getBlockPos()))
        {
            return new CreatorTarget(hit.getBlockPos().relative(hit.getDirection()), hit.getBlockPos(), hit.getDirection(), hit.getLocation(), true);
        }
        else if (trace.getHitType() == RayTraceWrapper.HitType.VANILLA_BLOCK)
        {
            return new CreatorTarget(hit.getBlockPos().relative(hit.getDirection()), hit.getBlockPos(), hit.getDirection(), hit.getLocation(), false);
        }

        return null;
    }

    @Nullable
    private CreatorTarget getExistingCreatorTarget(Minecraft mc)
    {
        RayTraceWrapper trace = this.trace(mc);

        if (trace == null || trace.getBlockHitResult() == null || trace.getHitType() != RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
        {
            return null;
        }

        BlockHitResult hit = trace.getBlockHitResult();
        return this.isCurrentDraftBlock(hit.getBlockPos()) ? new CreatorTarget(hit.getBlockPos(), hit.getBlockPos(), hit.getDirection(), hit.getLocation(), true) : null;
    }

    @Nullable
    private CreatorTarget getPickTarget(Minecraft mc)
    {
        RayTraceWrapper trace = this.trace(mc);

        if (trace == null || trace.getBlockHitResult() == null)
        {
            return null;
        }

        BlockHitResult hit = trace.getBlockHitResult();

        if (trace.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK && this.isCurrentDraftBlock(hit.getBlockPos()))
        {
            return new CreatorTarget(hit.getBlockPos(), hit.getBlockPos(), hit.getDirection(), hit.getLocation(), true);
        }
        else if (trace.getHitType() == RayTraceWrapper.HitType.VANILLA_BLOCK)
        {
            return new CreatorTarget(hit.getBlockPos(), hit.getBlockPos(), hit.getDirection(), hit.getLocation(), false);
        }

        return null;
    }

    @Nullable
    private RayTraceWrapper trace(Minecraft mc)
    {
        Entity camera = CreatorCameraCompat.getCameraEntity();
        return camera != null && mc.level != null ? RayTraceUtils.getGenericTrace(mc.level, camera, EDIT_RANGE, true, false, false) : null;
    }

    private boolean isCurrentDraftBlock(BlockPos pos)
    {
        CreatorDraft draft = CreatorManager.getInstance().getCurrentDraft();
        return draft != null && !draft.getBlockState(pos).isAir();
    }

    private boolean canEdit(Minecraft mc)
    {
        boolean canEdit = CreatorManager.getInstance().isCreatorModeEnabled() && mc.level != null && mc.player != null;

        if (canEdit)
        {
            CreatorClientTickHandler.INSTANCE.updateTweakerooFreeCameraCompatibility(mc);
        }

        return canEdit;
    }

    private boolean canPlaceNow()
    {
        return CreatorClientTickHandler.getClientTicks() >= this.nextPlaceTick;
    }

    private void showNoTargetWarningThrottled()
    {
        long now = System.currentTimeMillis();

        if (now - this.lastNoTargetWarning >= NO_TARGET_MESSAGE_INTERVAL_MS)
        {
            this.lastNoTargetWarning = now;
            InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica-creator.message.edit.no_target");
        }
    }

    private record CreatorTarget(BlockPos blockPos, BlockPos clickedBlockPos, Direction side, Vec3 hitVec, boolean schematicBlock)
    {
    }
}
