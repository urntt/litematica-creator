package io.github.urntt.litematicacreator.creator;

import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import fi.dy.masa.litematica.mixin.entity.IMixinEntity;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import io.github.urntt.litematicacreator.config.Configs;
import io.github.urntt.litematicacreator.camera.CreatorCameraController;
import io.github.urntt.litematicacreator.camera.CreatorCameraEntity;
import io.github.urntt.litematicacreator.gui.GuiFocusSwitcher;
import io.github.urntt.litematicacreator.render.CreatorVirtualLoadout;

public class CreatorEditService
{
    private static final CreatorEditService INSTANCE = new CreatorEditService();
    private static final long NO_TARGET_MESSAGE_INTERVAL_MS = 1500L;

    private long lastNoTargetWarning;

    private CreatorEditService()
    {
    }

    public static CreatorEditService getInstance()
    {
        return INSTANCE;
    }

    public void resetTransientState()
    {
        this.lastNoTargetWarning = 0L;
    }

    CreatorPlacementTrace tracePlacementTarget()
    {
        Minecraft mc = Minecraft.getInstance();

        if (!this.canEdit(mc))
        {
            return CreatorPlacementTrace.noTarget();
        }

        return this.getPlacementTarget(mc);
    }

    CreatorEditOutcome placeProjectionBlock(CreatorPlacementTrace placementTrace, boolean showWarnings)
    {
        Minecraft mc = Minecraft.getInstance();

        if (!this.canEdit(mc))
        {
            return CreatorEditOutcome.NO_CHANGE;
        }

        if (placementTrace.kind() == CreatorPlacementTrace.Kind.BLOCKED_BY_ENTITY)
        {
            return CreatorEditOutcome.NO_CHANGE;
        }

        CreatorEditTarget target = placementTrace.target();

        if (target == null)
        {
            if (showWarnings)
            {
                this.showNoTargetWarningThrottled();
            }

            return CreatorEditOutcome.NO_CHANGE;
        }

        CreatorInventory inventory = CreatorInventory.getInstance();
        @Nullable CreatorPlacementHandResolver.Selection heldItem = CreatorPlacementHandResolver.resolve(
                inventory.getSelectedStack(),
                inventory.getStack(CreatorInventory.OFFHAND_SLOT)
        );

        if (heldItem == null)
        {
            if (showWarnings)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica-creator.message.edit.no_block_selected");
            }

            return CreatorEditOutcome.NO_CHANGE;
        }

        CreatorTargetResolver.Resolution resolution = this.resolvePlacementTarget(target);

        if (resolution.action() == CreatorTargetResolver.Action.CHOOSE_OVERLAP)
        {
            GuiFocusSwitcher.openForOverlap(resolution.candidates());
            return CreatorEditOutcome.OVERLAP;
        }

        PlacementPreflight preflight = this.preflightPlacement(
                mc,
                heldItem.blockItem(),
                heldItem.stack(),
                heldItem.hand(),
                target,
                resolution.placement()
        );

        if (preflight.outcome() == PreflightOutcome.INVALID_STATE)
        {
            if (showWarnings)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica-creator.message.edit.no_place_state");
            }

            return CreatorEditOutcome.NO_CHANGE;
        }

        if (preflight.outcome() == PreflightOutcome.BLOCKED)
        {
            return CreatorEditOutcome.NO_CHANGE;
        }

        CreatorManager manager = CreatorManager.getInstance();
        SchematicPlacement placement;

        if (resolution.action() == CreatorTargetResolver.Action.EDIT)
        {
            placement = resolution.placement();
            manager.focusPlacement(placement);
        }
        else
        {
            placement = manager.createBlank(target.blockPos());
        }

        boolean edited = CreatorEditFeedback.afterSuccessfulEdit(
                CreatorSchematicEditor.setBlockState(placement, target.blockPos(), preflight.state()),
                () -> CreatorEditFeedback.feedbackTarget(
                        mc.player,
                        CreatorCameraController.getInstance().getCamera()
                ).swing(heldItem.hand(), heldItem.stack().getInteractAnimation(), false)
        );
        return edited ? CreatorEditOutcome.EDITED : CreatorEditOutcome.NO_CHANGE;
    }

    @Nullable
    CreatorEditTarget traceDeleteTarget()
    {
        Minecraft mc = Minecraft.getInstance();

        if (!this.canEdit(mc))
        {
            return null;
        }

        return this.getExistingCreatorTarget(mc);
    }

    CreatorEditOutcome deleteProjectionBlock(@Nullable CreatorEditTarget target)
    {
        Minecraft mc = Minecraft.getInstance();

        if (!this.canEdit(mc) || target == null)
        {
            return CreatorEditOutcome.NO_CHANGE;
        }

        List<CreatorPlacementTarget> candidates = CreatorPlacementIndex.INSTANCE.findAt(target.blockPos());

        if (candidates.size() > 1)
        {
            GuiFocusSwitcher.openForOverlap(candidates.stream().map(CreatorPlacementTarget::placement).toList());
            return CreatorEditOutcome.OVERLAP;
        }

        if (candidates.size() == 1)
        {
            SchematicPlacement placement = candidates.getFirst().placement();
            CreatorManager.getInstance().focusPlacement(placement);
            boolean edited = CreatorEditFeedback.afterSuccessfulEdit(
                    CreatorSchematicEditor.setBlockState(placement, target.blockPos(), Blocks.AIR.defaultBlockState()),
                    () -> CreatorEditFeedback.feedbackTarget(
                            mc.player,
                            CreatorCameraController.getInstance().getCamera()
                    ).swing(InteractionHand.MAIN_HAND, CreatorVirtualLoadout.getMainHand().getAttackAnimation(), false)
            );
            return edited ? CreatorEditOutcome.EDITED : CreatorEditOutcome.NO_CHANGE;
        }

        return CreatorEditOutcome.NO_CHANGE;
    }

    void swingWithoutEdit(@Nullable CreatorEditTarget target)
    {
        @Nullable CreatorCameraEntity camera = CreatorCameraController.getInstance().getCamera();

        if (CreatorEditFeedback.swingsWithoutEdit(target != null, camera != null))
        {
            camera.swing(InteractionHand.MAIN_HAND, CreatorVirtualLoadout.getMainHand().getAttackAnimation(), false);
        }
    }

    public boolean pickBlock()
    {
        Minecraft mc = Minecraft.getInstance();

        if (!this.canEdit(mc))
        {
            return false;
        }

        @Nullable CreatorEditTarget target = this.getPickTarget(mc);

        if (target == null)
        {
            return true;
        }

        BlockState state;
        if (target.schematicBlock())
        {
            Level schematicWorld = SchematicWorldHandler.getSchematicWorld();
            state = schematicWorld != null ? schematicWorld.getBlockState(target.blockPos()) : Blocks.AIR.defaultBlockState();
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

    private PlacementPreflight preflightPlacement(
            Minecraft mc,
            BlockItem blockItem,
            ItemStack stack,
            InteractionHand hand,
            CreatorEditTarget target,
            @Nullable SchematicPlacement placement)
    {
        Level schematicWorld = SchematicWorldHandler.getSchematicWorld();

        if (schematicWorld == null || mc.player == null)
        {
            BlockState state = blockItem.getBlock().defaultBlockState();
            return placement == null || CreatorSchematicEditor.getBlockState(placement, target.blockPos()).isAir() ?
                    PlacementPreflight.success(state) : PlacementPreflight.blocked();
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

            CreatorPlacementBlockContext context = new CreatorPlacementBlockContext(mc.player, hand, stack, hit);

            if (target.airTarget())
            {
                context.useClickedPosition();
            }

            BlockState state = blockItem.getBlock().getStateForPlacement(context);

            if (state == null || state.isAir())
            {
                return PlacementPreflight.invalidState();
            }

            if (placement != null)
            {
                BlockState targetState = CreatorSchematicEditor.getBlockState(placement, target.blockPos());

                if (!targetState.isAir())
                {
                    BlockHitResult targetHit = new BlockHitResult(
                            target.hitVec(),
                            target.side(),
                            target.blockPos(),
                            false
                    );
                    CreatorTargetPlaceContext targetContext = new CreatorTargetPlaceContext(
                            mc.player,
                            hand,
                            stack,
                            targetHit
                    );
                    boolean replaceable = targetContext.canReplace(targetState);

                    if (!CreatorPlacementPolicy.canWrite(false, replaceable))
                    {
                        return PlacementPreflight.blocked();
                    }
                }
            }

            if (!CreatorPlacementEntityCollision.canPlace(mc, schematicWorld, state, target.blockPos()))
            {
                return PlacementPreflight.blocked();
            }

            return PlacementPreflight.success(state);
        }
        finally
        {
            EntityUtils.setEntityRotations(mc.player, oldYaw, oldPitch);
            ((IMixinEntity) mc.player).litematica_setWorld(oldWorld);
        }
    }

    private CreatorPlacementTrace getPlacementTarget(Minecraft mc)
    {
        CreatorTargeting.TraceResult result = CreatorTargeting.traceResult(mc);
        CreatorPlacementTracePolicy.Action action = CreatorPlacementTracePolicy.decide(
                result.source(),
                Configs.Generic.ENABLE_AIR_PLACEMENT.getBooleanValue()
        );

        if (action == CreatorPlacementTracePolicy.Action.BLOCKED_BY_ENTITY)
        {
            return CreatorPlacementTrace.blockedByEntity();
        }

        if (action == CreatorPlacementTracePolicy.Action.USE_BLOCK_TARGET)
        {
            @Nullable RayTraceWrapper trace = result.trace();
            @Nullable BlockHitResult hit = trace != null ? trace.getBlockHitResult() : null;

            if (hit == null)
            {
                return CreatorPlacementTrace.noTarget();
            }

            boolean schematic = result.source() == CreatorTargetingPolicy.Source.SCHEMATIC_BLOCK;
            return CreatorPlacementTrace.target(new CreatorEditTarget(
                    hit.getBlockPos().relative(hit.getDirection()),
                    hit.getBlockPos(),
                    hit.getDirection(),
                    hit.getLocation(),
                    schematic,
                    false
            ));
        }

        if (action == CreatorPlacementTracePolicy.Action.NO_TARGET)
        {
            return CreatorPlacementTrace.noTarget();
        }

        Entity camera = CreatorCameraCompat.getCameraEntity();

        if (camera == null)
        {
            return CreatorPlacementTrace.noTarget();
        }

        int distance = CreatorAirPlacementTarget.effectiveDistance(
                Configs.Generic.AIR_PLACEMENT_DISTANCE.getIntegerValue(),
                Configs.Generic.CREATOR_EDIT_RANGE.getIntegerValue()
        );
        CreatorAirPlacementTarget.Target airTarget = CreatorAirPlacementTarget.resolve(
                camera.getEyePosition(1.0F),
                camera.getViewVector(1.0F),
                distance
        );
        return CreatorPlacementTrace.target(new CreatorEditTarget(
                airTarget.blockPos(),
                airTarget.blockPos(),
                airTarget.side(),
                airTarget.hitPosition(),
                false,
                true
        ));
    }

    @Nullable
    private CreatorEditTarget getExistingCreatorTarget(Minecraft mc)
    {
        RayTraceWrapper trace = this.trace(mc);

        if (trace == null || trace.getBlockHitResult() == null || trace.getHitType() != RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
        {
            return null;
        }

        BlockHitResult hit = trace.getBlockHitResult();
        return new CreatorEditTarget(hit.getBlockPos(), hit.getBlockPos(), hit.getDirection(), hit.getLocation(), true, false);
    }

    @Nullable
    private CreatorEditTarget getPickTarget(Minecraft mc)
    {
        RayTraceWrapper trace = this.trace(mc);

        if (trace == null || trace.getBlockHitResult() == null)
        {
            return null;
        }

        BlockHitResult hit = trace.getBlockHitResult();

        if (trace.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
        {
            return new CreatorEditTarget(hit.getBlockPos(), hit.getBlockPos(), hit.getDirection(), hit.getLocation(), true, false);
        }
        else if (trace.getHitType() == RayTraceWrapper.HitType.VANILLA_BLOCK)
        {
            return new CreatorEditTarget(hit.getBlockPos(), hit.getBlockPos(), hit.getDirection(), hit.getLocation(), false, false);
        }

        return null;
    }

    @Nullable
    private RayTraceWrapper trace(Minecraft mc)
    {
        return CreatorTargeting.trace(mc);
    }

    private CreatorTargetResolver.Resolution resolvePlacementTarget(CreatorEditTarget target)
    {
        CreatorManager manager = CreatorManager.getInstance();
        List<CreatorPlacementTarget> hitCandidates = target.schematicBlock() ? CreatorPlacementIndex.INSTANCE.findAt(target.clickedBlockPos()) : List.of();
        List<CreatorPlacementTarget> writeCandidates = CreatorPlacementIndex.INSTANCE.findAt(target.blockPos());
        return CreatorTargetResolver.resolve(hitCandidates, writeCandidates, manager.getFocus());
    }

    private boolean canEdit(Minecraft mc)
    {
        boolean canEdit = CreatorManager.getInstance().isCreatorModeEnabled() && mc.level != null && mc.player != null;

        return canEdit;
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

    private enum PreflightOutcome
    {
        SUCCESS,
        INVALID_STATE,
        BLOCKED
    }

    private record PlacementPreflight(PreflightOutcome outcome, @Nullable BlockState state)
    {
        private static PlacementPreflight success(BlockState state)
        {
            return new PlacementPreflight(PreflightOutcome.SUCCESS, state);
        }

        private static PlacementPreflight invalidState()
        {
            return new PlacementPreflight(PreflightOutcome.INVALID_STATE, null);
        }

        private static PlacementPreflight blocked()
        {
            return new PlacementPreflight(PreflightOutcome.BLOCKED, null);
        }
    }

    private static class CreatorTargetPlaceContext extends BlockPlaceContext
    {
        private CreatorTargetPlaceContext(
                net.minecraft.world.entity.player.Player player,
                InteractionHand hand,
                ItemStack stack,
                BlockHitResult hit)
        {
            super(player, hand, stack, hit);
        }

        private boolean canReplace(BlockState targetState)
        {
            this.replaceClicked = true;
            boolean replaceable = targetState.canBeReplaced(this);
            this.replaceClicked = replaceable;
            return replaceable;
        }
    }

    private static class CreatorPlacementBlockContext extends BlockPlaceContext
    {
        private CreatorPlacementBlockContext(
                net.minecraft.world.entity.player.Player player,
                InteractionHand hand,
                ItemStack stack,
                BlockHitResult hit)
        {
            super(player, hand, stack, hit);
        }

        private void useClickedPosition()
        {
            this.replaceClicked = true;
        }
    }
}
