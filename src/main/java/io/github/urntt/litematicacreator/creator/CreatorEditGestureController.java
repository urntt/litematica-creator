package io.github.urntt.litematicacreator.creator;

import net.minecraft.client.Minecraft;

import fi.dy.masa.malilib.util.GuiUtils;
import io.github.urntt.litematicacreator.config.Configs;
import io.github.urntt.litematicacreator.config.CreatorPlacementRepeatMode;

public final class CreatorEditGestureController
{
    public static final CreatorEditGestureController INSTANCE = new CreatorEditGestureController();

    private final InputLatch placeInput = new InputLatch();
    private final InputLatch breakInput = new InputLatch();
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

        for (CreatorEditTarget target : plan.targets())
        {
            CreatorEditOutcome outcome = edits.placeProjectionBlock(target, plan.freshPress());

            if (outcome == CreatorEditOutcome.OVERLAP)
            {
                this.placementRepeat.reset();
                return true;
            }
        }

        return false;
    }

    private boolean executeBreakPlan(
            CreatorEditService edits,
            CreatorBreakRepeatState.RepeatPlan<CreatorEditTarget> plan)
    {
        for (CreatorEditTarget target : plan.targets())
        {
            if (edits.deleteProjectionBlock(target) == CreatorEditOutcome.OVERLAP)
            {
                this.breakRepeat.reset();
                return true;
            }
        }

        return false;
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

    private static final class InputLatch
    {
        private boolean down;
        private boolean armed;
        private boolean blockedUntilRelease;
        private int pendingPresses;

        private void update(boolean pressed, boolean acceptsCreatorEdits)
        {
            if (!pressed)
            {
                this.down = false;
                this.armed = false;
                this.blockedUntilRelease = false;
                return;
            }

            if (this.down)
            {
                return;
            }

            this.down = true;

            if (acceptsCreatorEdits && !this.blockedUntilRelease)
            {
                ++this.pendingPresses;
                this.armed = true;
            }
            else
            {
                this.armed = false;
                this.blockedUntilRelease = true;
            }
        }

        private void sync(boolean currentlyDown, boolean acceptsCreatorEdits)
        {
            if (currentlyDown != this.down)
            {
                this.update(currentlyDown, acceptsCreatorEdits);
            }
        }

        private boolean consumePress()
        {
            if (this.pendingPresses <= 0)
            {
                return false;
            }

            --this.pendingPresses;
            return true;
        }

        private boolean isHeld()
        {
            return this.down && this.armed && !this.blockedUntilRelease;
        }

        private void suspend(boolean currentlyDown)
        {
            this.down = currentlyDown;
            this.armed = false;
            this.blockedUntilRelease = currentlyDown;
            this.pendingPresses = 0;
        }
    }
}
