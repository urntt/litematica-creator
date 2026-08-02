package io.github.urntt.litematicacreator.creator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.github.urntt.litematicacreator.config.CreatorPlacementRepeatMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatorEditBatchExecutorTest
{
    @Test
    void blockedTargetsAreSkippedWithoutStoppingLaterTargets()
    {
        List<String> attempted = new ArrayList<>();
        boolean aborted = CreatorEditBatchExecutor.execute(
                List.of("blocked", "edited"),
                target ->
                {
                    attempted.add(target);
                    return target.equals("blocked") ? CreatorEditOutcome.NO_CHANGE : CreatorEditOutcome.EDITED;
                },
                () -> { }
        );

        assertFalse(aborted);
        assertEquals(List.of("blocked", "edited"), attempted);
    }

    @Test
    void overlapStopsTheRemainingBackfillAndClearsTheGesture()
    {
        CreatorPlacementRepeatState<String, String> state = new CreatorPlacementRepeatState<>();
        state.press(0L, target("A"), CreatorPlacementRepeatMode.ACCURATE, 4);
        state.hold(1L, target("B"), 4);
        CreatorPlacementRepeatState.RepeatPlan<String> plan = state.hold(4L, target("C"), 4);
        List<String> attempted = new ArrayList<>();

        boolean aborted = CreatorEditBatchExecutor.execute(
                plan.targets(),
                target ->
                {
                    attempted.add(target);
                    return target.equals("B") ? CreatorEditOutcome.OVERLAP : CreatorEditOutcome.EDITED;
                },
                state::reset
        );

        assertTrue(aborted);
        assertEquals(List.of("B"), attempted);
        assertFalse(state.isActive());
        assertFalse(state.shouldObserveHeld(20L));
    }

    @Test
    void queuedTargetsUseExecutionTimeItemAndPlacementContext()
    {
        AtomicReference<String> currentItem = new AtomicReference<>("stone");
        AtomicReference<String> currentPlacement = new AtomicReference<>("placement-a");
        List<String> writes = new ArrayList<>();
        List<String> queuedTargets = List.of("B", "C");

        currentItem.set("dirt");
        currentPlacement.set("placement-b");
        CreatorEditBatchExecutor.execute(
                queuedTargets,
                target ->
                {
                    writes.add(target + ":" + currentItem.get() + ":" + currentPlacement.get());
                    return CreatorEditOutcome.EDITED;
                },
                () -> { }
        );

        assertEquals(List.of("B:dirt:placement-b", "C:dirt:placement-b"), writes);
    }

    private static CreatorPlacementRepeatState.ObservedTarget<String, String> target(String value)
    {
        return new CreatorPlacementRepeatState.ObservedTarget<>(value, value);
    }
}
