package io.github.urntt.litematicacreator.creator;

import net.minecraft.client.Minecraft;

import fi.dy.masa.malilib.util.GuiUtils;
import io.github.urntt.litematicacreator.config.Configs;

public final class CreatorEditGestureController
{
    public static final CreatorEditGestureController INSTANCE = new CreatorEditGestureController();

    private final CreatorInputLatch placeInput = new CreatorInputLatch();
    private final CreatorInputLatch breakInput = new CreatorInputLatch();
    private final CreatorBreakRepeatState<CreatorEditTarget.CreatorEditTargetKey, CreatorEditTarget> breakRepeat =
            new CreatorBreakRepeatState<>();
    private long nextPlaceTick;

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

        if (edits.holdsDebugStick())
        {
            this.runDebugStick(edits, placeDown, breakDown);
            return;
        }

        int placeInterval = Configs.Generic.CONTINUOUS_PLACE_INTERVAL_TICKS.getIntegerValue();
        int breakInterval = Configs.Generic.CONTINUOUS_BREAK_INTERVAL_TICKS.getIntegerValue();

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

            // Only a fresh press swings without an edit; holding the key keeps a single swing.
            edits.swingWithoutEdit(target);
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
            CreatorPlacementTrace trace = edits.tracePlacementTarget();
            CreatorEditOutcome outcome = edits.placeProjectionBlock(trace, true);
            this.nextPlaceTick = tick + placeInterval;

            if (outcome == CreatorEditOutcome.OVERLAP)
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

        if (!freshPlace && this.placeInput.isHeld() && tick >= this.nextPlaceTick)
        {
            CreatorPlacementTrace trace = edits.tracePlacementTarget();
            CreatorEditOutcome outcome = edits.placeProjectionBlock(trace, false);
            this.nextPlaceTick = tick + placeInterval;

            if (outcome == CreatorEditOutcome.OVERLAP)
            {
                this.suspend(placeDown, breakDown);
                return;
            }

            if (GuiUtils.getCurrentScreen() != null)
            {
                this.suspend(placeDown, breakDown);
            }
        }
    }

    // A virtual debug stick takes over attack and use: one action per fresh press, never a held repeat.
    private void runDebugStick(CreatorEditService edits, boolean placeDown, boolean breakDown)
    {
        this.breakRepeat.reset();
        this.nextPlaceTick = 0L;

        while (this.breakInput.consumePress())
        {
            CreatorEditTarget target = edits.traceDeleteTarget();
            edits.useDebugStick(target, false);
            edits.swingWithoutEdit(target);

            if (GuiUtils.getCurrentScreen() != null)
            {
                this.suspend(placeDown, breakDown);
                return;
            }
        }

        while (this.placeInput.consumePress())
        {
            if (edits.useDebugStick(edits.traceDeleteTarget(), true) == CreatorEditOutcome.OVERLAP ||
                GuiUtils.getCurrentScreen() != null)
            {
                this.suspend(placeDown, breakDown);
                return;
            }
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
        this.breakRepeat.reset();
        this.nextPlaceTick = 0L;
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

    private static CreatorBreakRepeatState.ObservedTarget<CreatorEditTarget.CreatorEditTargetKey, CreatorEditTarget> observedBreak(
            CreatorEditTarget target)
    {
        return target != null ? new CreatorBreakRepeatState.ObservedTarget<>(target.key(), target) : null;
    }

}
