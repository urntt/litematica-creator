package io.github.urntt.litematicacreator.creator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

import io.github.urntt.litematicacreator.config.CreatorPlacementRepeatMode;

final class CreatorPlacementRepeatState<K, T>
{
    private final LinkedHashMap<K, T> backfill = new LinkedHashMap<>();
    private final Set<K> seenTargets = new HashSet<>();

    private boolean active;
    private boolean accurateUnlocked;
    private long nextRepeatTick;
    private CreatorPlacementRepeatMode mode = CreatorPlacementRepeatMode.FIXED;

    RepeatPlan<T> press(
            long tick,
            @Nullable ObservedTarget<K, T> target,
            CreatorPlacementRepeatMode mode,
            int intervalTicks)
    {
        this.reset();
        this.active = true;
        this.mode = mode;
        this.nextRepeatTick = tick + intervalTicks;

        if (target == null)
        {
            return RepeatPlan.fresh(List.of());
        }

        this.seenTargets.add(target.key());
        return RepeatPlan.fresh(List.of(target.snapshot()));
    }

    boolean shouldObserveHeld(long tick)
    {
        return this.active && (this.mode == CreatorPlacementRepeatMode.ACCURATE || tick >= this.nextRepeatTick);
    }

    RepeatPlan<T> hold(long tick, @Nullable ObservedTarget<K, T> target, int intervalTicks)
    {
        if (!this.active)
        {
            return RepeatPlan.empty();
        }

        if (this.mode == CreatorPlacementRepeatMode.FIXED)
        {
            if (tick < this.nextRepeatTick)
            {
                return RepeatPlan.empty();
            }

            this.nextRepeatTick = tick + intervalTicks;
            return target != null ? RepeatPlan.repeat(List.of(target.snapshot())) : RepeatPlan.empty();
        }

        if (target == null || !this.seenTargets.add(target.key()))
        {
            return RepeatPlan.empty();
        }

        if (!this.accurateUnlocked)
        {
            if (tick < this.nextRepeatTick)
            {
                this.backfill.putIfAbsent(target.key(), target.snapshot());
                return RepeatPlan.empty();
            }

            List<T> targets = new ArrayList<>(this.backfill.values());
            targets.add(target.snapshot());
            this.backfill.clear();
            this.accurateUnlocked = true;
            return RepeatPlan.repeat(targets);
        }

        return RepeatPlan.repeat(List.of(target.snapshot()));
    }

    CreatorPlacementRepeatMode mode()
    {
        return this.mode;
    }

    boolean isActive()
    {
        return this.active;
    }

    void reset()
    {
        this.active = false;
        this.accurateUnlocked = false;
        this.nextRepeatTick = 0L;
        this.backfill.clear();
        this.seenTargets.clear();
    }

    record ObservedTarget<K, T>(K key, T snapshot)
    {
    }

    record RepeatPlan<T>(boolean freshPress, List<T> targets)
    {
        RepeatPlan
        {
            targets = List.copyOf(targets);
        }

        private static <T> RepeatPlan<T> fresh(List<T> targets)
        {
            return new RepeatPlan<>(true, targets);
        }

        private static <T> RepeatPlan<T> repeat(List<T> targets)
        {
            return new RepeatPlan<>(false, targets);
        }

        private static <T> RepeatPlan<T> empty()
        {
            return new RepeatPlan<>(false, List.of());
        }
    }
}
