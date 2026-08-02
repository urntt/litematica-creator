package io.github.urntt.litematicacreator.creator;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

final class CreatorBreakRepeatState<K, T>
{
    private boolean active;
    private boolean hasLastTarget;
    @Nullable
    private K lastTargetKey;
    private long nextRepeatTick;

    RepeatPlan<T> press(long tick, @Nullable ObservedTarget<K, T> target, int intervalTicks)
    {
        this.reset();
        this.active = true;
        this.nextRepeatTick = tick + intervalTicks;
        this.remember(target);
        return target != null ? RepeatPlan.of(target.snapshot()) : RepeatPlan.empty();
    }

    boolean shouldObserveHeld(long tick)
    {
        return this.active && tick >= this.nextRepeatTick;
    }

    RepeatPlan<T> hold(long tick, @Nullable ObservedTarget<K, T> target, int intervalTicks)
    {
        if (!this.active || tick < this.nextRepeatTick)
        {
            return RepeatPlan.empty();
        }

        this.nextRepeatTick = tick + intervalTicks;

        if (target == null)
        {
            this.remember(null);
            return RepeatPlan.empty();
        }

        if (this.hasLastTarget && Objects.equals(this.lastTargetKey, target.key()))
        {
            return RepeatPlan.empty();
        }

        this.remember(target);
        return RepeatPlan.of(target.snapshot());
    }

    void reset()
    {
        this.active = false;
        this.hasLastTarget = false;
        this.lastTargetKey = null;
        this.nextRepeatTick = 0L;
    }

    private void remember(@Nullable ObservedTarget<K, T> target)
    {
        this.hasLastTarget = target != null;
        this.lastTargetKey = target != null ? target.key() : null;
    }

    record ObservedTarget<K, T>(K key, T snapshot)
    {
    }

    record RepeatPlan<T>(List<T> targets)
    {
        RepeatPlan
        {
            targets = List.copyOf(targets);
        }

        private static <T> RepeatPlan<T> of(T target)
        {
            return new RepeatPlan<>(List.of(target));
        }

        private static <T> RepeatPlan<T> empty()
        {
            return new RepeatPlan<>(List.of());
        }
    }
}
