package io.github.urntt.litematicacreator.creator;

import net.minecraft.client.Minecraft;

import fi.dy.masa.malilib.util.GuiUtils;
import io.github.urntt.litematicacreator.config.Configs;
import io.github.urntt.litematicacreator.config.CreatorPlacementRepeatMode;

public final class CreatorEditGestureController
{
    public static final CreatorEditGestureController INSTANCE = new CreatorEditGestureController();

    private final CreatorInputLatch placeInput = new CreatorInputLatch();
    private final CreatorInputLatch breakInput = new CreatorInputLatch();
    private final CreatorPlacementRepeatState<CreatorEditTarget.CreatorEditTargetKey, CreatorEditTarget> placementRepeat =
            new CreatorPlacementRepeatState<>();
    private final CreatorBreakRepeatState<CreatorEditTarget.CreatorEditTargetKey, CreatorEditTarget> breakRepeat =
            new CreatorBreakRepeatState<>();

    private CreatorEditGestureController()
    {
    }

    public void onPlaceInput(boolean pressed, boolean acceptsCreatorEdits)
    {
        this.placeInput.update(pressed, acceptsCreatorEdits);
    }

    public void onBreakInput(boolean pressed, boolean acceptsCreatorEdits)
    {
        this.breakInput.update(pressed, acceptsCreatorEdits);
    }

    public void onClientTick(Minecraft mc, long tick)
    {
        boolean acceptsCreatorEdits = CreatorManager.getInstance().isCreatorModeEnabled() &&
                                      mc.level != null &&
                                      mc.player != null &&
                                      GuiUtils.getCurrentScreen() == null;
        boolean placeDown = mc.options.keyUse.isDown();
        boolean breakDown = mc.options.keyAttack.isDown();

        this.placeInput.sync(placeDown, acceptsCreatorEdits);
        this.breakInput.sync(breakDown, acceptsCreatorEdits);

        if (!acceptsCreatorEdits)
        {
            this.suspend(placeDown, breakDown);
            return;
        }

        CreatorEditService edits = CreatorEditService.getInstance();
        CreatorPlacementRepeatMode repeatMode = (CreatorPlacementRepeatMode) Configs.Generic.PLACEMENT_REPEAT_MODE.getOptionListValue();
        int placementInterval = Configs.Generic.PLACEMENT_REPEAT_INTERVAL_TICKS.getIntegerValue();
        int breakInterval = Configs.Generic.CONTINUOUS_BREAK_INTERVAL_TICKS.getIntegerValue();

        if (this.placeInput.isHeld() && this.placementRepeat.isActive() && this.placementRepeat.mode() != repeatMode)
        {
            this.placeInput.suspend(placeDown);
            this.placementRepeat.reset();
        }

        boolean freshBreak = false;

        while (this.breakInput.consumePress())
        {
            freshBreak = true;
            CreatorEditTarget target = edits.traceDeleteTarget();
            CreatorBreakRepeatState.RepeatPlan<CreatorEditTarget> plan = this.breakRepeat.press(
                    tick,
                    observedBreak(target),
                    breakInterval
            );

            if (this.executeBreakPlan(edits, plan))
            {
                this.suspend(placeDown, breakDown);
                return;
            }

            if (GuiUtils.getCurrentScreen() != null)
            {
                this.suspend(placeDown, breakDown);
                return;
            }
        }

        if (!freshBreak && this.breakInput.isHeld() && this.breakRepeat.shouldObserveHeld(tick))
        {
            CreatorEditTarget target = edits.traceDeleteTarget();
            CreatorBreakRepeatState.RepeatPlan<CreatorEditTarget> plan = this.breakRepeat.hold(
                    tick,
                    observedBreak(target),
                    breakInterval
            );

            if (this.executeBreakPlan(edits, plan))
            {
                this.suspend(placeDown, breakDown);
                return;
            }

            if (GuiUtils.getCurrentScreen() != null)
            {
                this.suspend(placeDown, breakDown);
                return;
            }
        }

        if (!this.breakInput.isHeld())
        {
            this.breakRepeat.reset();
        }

        boolean freshPlace = false;

        while (this.placeInput.consumePress())
        {
            freshPlace = true;
            CreatorEditTarget target = edits.tracePlacementTarget();
            CreatorPlacementRepeatState.RepeatPlan<CreatorEditTarget> plan = this.placementRepeat.press(
                    tick,
                    observed(target),
                    repeatMode,
                    placementInterval
            );

            if (this.executePlacementPlan(edits, plan))
            {
                this.suspend(placeDown, breakDown);
                return;
            }

            if (GuiUtils.getCurrentScreen() != null)
            {
                this.suspend(placeDown, breakDown);
                return;
            }
        }

        if (!freshPlace && this.placeInput.isHeld() && this.placementRepeat.shouldObserveHeld(tick))
        {
            CreatorEditTarget target = edits.tracePlacementTarget();
            CreatorPlacementRepeatState.RepeatPlan<CreatorEditTarget> plan = this.placementRepeat.hold(
                    tick,
                    observed(target),
                    placementInterval
            );

            if (this.executePlacementPlan(edits, plan))
            {
                this.suspend(placeDown, breakDown);
                return;
            }

            if (GuiUtils.getCurrentScreen() != null)
            {
                this.suspend(placeDown, breakDown);
            }
        }

        if (!this.placeInput.isHeld())
        {
            this.placementRepeat.reset();
        }
    }

    public void resetTransientState()
    {
        Minecraft mc = Minecraft.getInstance();
        this.suspend(mc.options.keyUse.isDown(), mc.options.keyAttack.isDown());
    }

    private void suspend(boolean placeDown, boolean breakDown)
    {
        this.placeInput.suspend(placeDown);
        this.breakInput.suspend(breakDown);
        this.placementRepeat.reset();
        this.breakRepeat.reset();
    }

    private boolean executePlacementPlan(
            CreatorEditService edits,
            CreatorPlacementRepeatState.RepeatPlan<CreatorEditTarget> plan)
    {
        if (plan.freshPress() && plan.targets().isEmpty())
        {
            return edits.placeProjectionBlock(null, true) == CreatorEditOutcome.OVERLAP;
        }

        return CreatorEditBatchExecutor.execute(
                plan.targets(),
                target -> edits.placeProjectionBlock(target, plan.freshPress()),
                this.placementRepeat::reset
        );
    }

    private boolean executeBreakPlan(
            CreatorEditService edits,
            CreatorBreakRepeatState.RepeatPlan<CreatorEditTarget> plan)
    {
        return CreatorEditBatchExecutor.execute(
                plan.targets(),
                edits::deleteProjectionBlock,
                this.breakRepeat::reset
        );
    }

    private static CreatorPlacementRepeatState.ObservedTarget<CreatorEditTarget.CreatorEditTargetKey, CreatorEditTarget> observed(
            CreatorEditTarget target)
    {
        return target != null ? new CreatorPlacementRepeatState.ObservedTarget<>(target.key(), target) : null;
    }

    private static CreatorBreakRepeatState.ObservedTarget<CreatorEditTarget.CreatorEditTargetKey, CreatorEditTarget> observedBreak(
            CreatorEditTarget target)
    {
        return target != null ? new CreatorBreakRepeatState.ObservedTarget<>(target.key(), target) : null;
    }

}
