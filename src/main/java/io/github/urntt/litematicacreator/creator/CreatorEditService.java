package io.github.urntt.litematicacreator.creator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DebugStickState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
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
        CreatorPlacementWorld.release();
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

        CreatorTargetResolver.Resolution resolution = this.resolvePlacementTarget(target, target.blockPos());

        if (resolution.action() == CreatorTargetResolver.Action.CHOOSE_OVERLAP)
        {
            GuiFocusSwitcher.openForOverlap(resolution.candidates());
            return CreatorEditOutcome.OVERLAP;
        }

        Player player = placementPlayer(mc);
        CreatorPlacementSimulation.Result result = CreatorPlacementSimulation.simulate(mc, player, heldItem, target, resolution.placement());

        // Vanilla decides where the block lands, for example on clicked real grass instead of next to it. A landing
        // cell that belongs to another projection is resolved again, so nothing is written into the wrong one.
        if (result.placed() && !result.primaryPos().equals(target.blockPos()))
        {
            CreatorTargetResolver.Resolution landed = this.resolvePlacementTarget(target, result.primaryPos());

            if (landed.action() == CreatorTargetResolver.Action.CHOOSE_OVERLAP)
            {
                GuiFocusSwitcher.openForOverlap(landed.candidates());
                return CreatorEditOutcome.OVERLAP;
            }

            if (landed.action() != resolution.action() || landed.placement() != resolution.placement())
            {
                BlockPos landedPos = result.primaryPos();
                resolution = landed;
                result = CreatorPlacementSimulation.simulate(mc, player, heldItem, target, landed.placement());

                if (result.placed() && !result.primaryPos().equals(landedPos))
                {
                    return CreatorEditOutcome.NO_CHANGE;
                }
            }
        }

        if (result.outcome() == CreatorPlacementSimulation.Outcome.NO_STATE && showWarnings)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica-creator.message.edit.no_place_state");
        }

        if (!result.placed())
        {
            return CreatorEditOutcome.NO_CHANGE;
        }

        // Focus and new drafts change only once the whole placement is known to be valid.
        CreatorManager manager = CreatorManager.getInstance();
        boolean editsExisting = resolution.action() == CreatorTargetResolver.Action.EDIT;
        SchematicPlacement placement = editsExisting ? resolution.placement() : manager.createBlank(result.primaryPos());

        Map<BlockPos, Optional<CompoundTag>> blockEntities = new LinkedHashMap<>();
        result.blockEntities().forEach((pos, data) -> blockEntities.put(pos, Optional.of(data)));

        if (!CreatorSchematicEditor.setBlockStates(placement, result.writes(), blockEntities))
        {
            return CreatorEditOutcome.NO_CHANGE;
        }

        if (editsExisting)
        {
            manager.focusPlacement(placement);
        }

        CreatorEditFeedback.feedbackTarget(mc.player, CreatorCameraController.getInstance().getCamera())
                .swing(heldItem.hand(), heldItem.stack().getInteractAnimation(), false);
        return CreatorEditOutcome.EDITED;
    }

    /**
     * Opens the vanilla editor of a projection sign, command block or container that the use input targets, unless the
     * placing player sneaks with something in a virtual hand, which places against it as in vanilla.
     *
     * @return the outcome, or {@code null} when the input should place a block instead
     */
    @Nullable
    CreatorEditOutcome useProjectionBlock(CreatorPlacementTrace placementTrace)
    {
        Minecraft mc = Minecraft.getInstance();
        @Nullable CreatorEditTarget target = placementTrace.target();

        if (!this.canEdit(mc) || target == null || !target.schematicBlock())
        {
            return null;
        }

        Player player = placementPlayer(mc);
        boolean holdsItem = !CreatorVirtualLoadout.getMainHand().isEmpty() || !CreatorVirtualLoadout.getOffhand().isEmpty();
        BlockPos pos = target.clickedBlockPos();
        List<SchematicPlacement> candidates = CreatorPlacementIndex.INSTANCE.findAt(pos).stream()
                .map(CreatorPlacementTarget::placement)
                .filter(placement -> CreatorBlockEntityEditorPolicy.opens(
                        CreatorBlockEntityEditorPolicy.editorFor(CreatorSchematicEditor.getBlockState(placement, pos)),
                        player.isSecondaryUseActive(),
                        holdsItem
                ))
                .toList();

        if (candidates.size() > 1)
        {
            GuiFocusSwitcher.openForOverlap(candidates);
            return CreatorEditOutcome.OVERLAP;
        }

        if (candidates.isEmpty() ||
            !CreatorBlockEntityEditSession.open(mc, candidates.getFirst(), pos, CreatorSchematicEditor.getBlockState(candidates.getFirst(), pos), player))
        {
            return null;
        }

        CreatorEditFeedback.feedbackTarget(mc.player, CreatorCameraController.getInstance().getCamera())
                .swing(InteractionHand.MAIN_HAND, CreatorVirtualLoadout.getMainHand().getInteractAnimation(), false);
        return CreatorEditOutcome.NO_CHANGE;
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

    boolean holdsDebugStick()
    {
        return CreatorDebugStick.activeHand(CreatorVirtualLoadout.getMainHand(), CreatorVirtualLoadout.getOffhand()) != null;
    }

    /**
     * Applies the virtual debug stick to a projection block: attacking selects a property on the stick, using cycles
     * that property on the projection. Real blocks and air are ignored, and the real player's permissions do not apply.
     */
    CreatorEditOutcome useDebugStick(@Nullable CreatorEditTarget target, boolean cycle)
    {
        Minecraft mc = Minecraft.getInstance();
        CreatorInventory inventory = CreatorInventory.getInstance();
        @Nullable InteractionHand hand = CreatorDebugStick.activeHand(
                inventory.getSelectedStack(),
                inventory.getStack(CreatorInventory.OFFHAND_SLOT)
        );

        if (!this.canEdit(mc) || hand == null || target == null)
        {
            return CreatorEditOutcome.NO_CHANGE;
        }

        int slot = hand == InteractionHand.MAIN_HAND ? inventory.getSelectedHotbarSlot() : CreatorInventory.OFFHAND_SLOT;
        ItemStack stick = inventory.getStack(slot);
        @Nullable DebugStickState stickState = stick.get(DataComponents.DEBUG_STICK_STATE);

        if (stickState == null)
        {
            return CreatorEditOutcome.NO_CHANGE;
        }

        // While the Creator Camera is active, sneaking belongs to the stand-in, not the real player.
        @Nullable CreatorCameraEntity camera = CreatorCameraController.getInstance().getCamera();
        boolean backward = camera != null ? camera.isSecondaryUseActive() : mc.player.isSecondaryUseActive();

        if (!cycle)
        {
            @Nullable Level schematicWorld = SchematicWorldHandler.getSchematicWorld();

            if (schematicWorld == null)
            {
                return CreatorEditOutcome.NO_CHANGE;
            }

            CreatorDebugStick.Action action = CreatorDebugStick.select(schematicWorld.getBlockState(target.blockPos()), stickState, backward);
            mc.player.sendOverlayMessage(action.message());

            if (action.stickState() != null)
            {
                ItemStack updated = stick.copy();
                updated.set(DataComponents.DEBUG_STICK_STATE, action.stickState());
                inventory.setStack(slot, updated);
                CreatorEditFeedback.feedbackTarget(mc.player, camera).swing(hand, stick.getAttackAnimation(), false);
            }

            return CreatorEditOutcome.NO_CHANGE;
        }

        List<CreatorPlacementTarget> candidates = CreatorPlacementIndex.INSTANCE.findAt(target.blockPos());

        if (candidates.size() > 1)
        {
            GuiFocusSwitcher.openForOverlap(candidates.stream().map(CreatorPlacementTarget::placement).toList());
            return CreatorEditOutcome.OVERLAP;
        }

        if (candidates.size() != 1)
        {
            return CreatorEditOutcome.NO_CHANGE;
        }

        SchematicPlacement placement = candidates.getFirst().placement();
        CreatorDebugStick.Action action = CreatorDebugStick.cycle(
                CreatorSchematicEditor.getBlockState(placement, target.blockPos()),
                stickState,
                backward
        );
        mc.player.sendOverlayMessage(action.message());

        if (action.state() == null)
        {
            return CreatorEditOutcome.NO_CHANGE;
        }

        CreatorManager.getInstance().focusPlacement(placement);
        boolean edited = CreatorEditFeedback.afterSuccessfulEdit(
                CreatorSchematicEditor.setBlockState(placement, target.blockPos(), action.state()),
                () -> CreatorEditFeedback.feedbackTarget(mc.player, camera).swing(hand, stick.getInteractAnimation(), false)
        );
        return edited ? CreatorEditOutcome.EDITED : CreatorEditOutcome.NO_CHANGE;
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

    private CreatorTargetResolver.Resolution resolvePlacementTarget(CreatorEditTarget target, BlockPos writePos)
    {
        CreatorManager manager = CreatorManager.getInstance();
        List<CreatorPlacementTarget> hitCandidates = target.schematicBlock() ? CreatorPlacementIndex.INSTANCE.findAt(target.clickedBlockPos()) : List.of();
        List<CreatorPlacementTarget> writeCandidates = CreatorPlacementIndex.INSTANCE.findAt(writePos);
        return CreatorTargetResolver.resolve(hitCandidates, writeCandidates, manager.getFocus());
    }

    // Placement rules read facing and sneaking from the placing player, which is the camera entity when one is active.
    private static Player placementPlayer(Minecraft mc)
    {
        return CreatorCameraCompat.getCameraEntity() instanceof Player camera ? camera : mc.player;
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
}
