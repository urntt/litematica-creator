package io.github.urntt.litematicacreator.creator;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.urntt.litematicacreator.config.CreatorPlacementRepeatMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatorPlacementRepeatStateTest
{
    @Test
    void fixedModeRunsImmediatelyAndAtTheConfiguredInterval()
    {
        CreatorPlacementRepeatState<String, String> state = new CreatorPlacementRepeatState<>();

        CreatorPlacementRepeatState.RepeatPlan<String> fresh = state.press(
                10L, target("A"), CreatorPlacementRepeatMode.FIXED, 4
        );

        assertTrue(fresh.freshPress());
        assertEquals(List.of("A"), fresh.targets());
        assertFalse(state.shouldObserveHeld(13L));
        assertTrue(state.shouldObserveHeld(14L));
        assertEquals(List.of("A"), state.hold(14L, target("A"), 4).targets());
        assertFalse(state.shouldObserveHeld(17L));
        assertTrue(state.shouldObserveHeld(18L));
    }

    @Test
    void fixedModeHonorsMinimumAndMaximumIntervals()
    {
        CreatorPlacementRepeatState<String, String> state = new CreatorPlacementRepeatState<>();
        state.press(0L, target("A"), CreatorPlacementRepeatMode.FIXED, 1);
        assertTrue(state.shouldObserveHeld(1L));

        state.press(0L, target("A"), CreatorPlacementRepeatMode.FIXED, 20);
        assertFalse(state.shouldObserveHeld(19L));
        assertTrue(state.shouldObserveHeld(20L));
    }

    @Test
    void freshPressWithoutATargetIsPreservedForWarningPolicy()
    {
        CreatorPlacementRepeatState<String, String> state = new CreatorPlacementRepeatState<>();
        CreatorPlacementRepeatState.RepeatPlan<String> plan = state.press(
                0L, null, CreatorPlacementRepeatMode.FIXED, 4
        );

        assertTrue(plan.freshPress());
        assertTrue(plan.targets().isEmpty());
    }

    @Test
    void accurateModeBackfillsDistinctTargetsThenUnlocks()
    {
        CreatorPlacementRepeatState<String, String> state = new CreatorPlacementRepeatState<>();
        state.press(0L, target("A"), CreatorPlacementRepeatMode.ACCURATE, 4);

        assertTrue(state.hold(1L, target("B"), 4).targets().isEmpty());
        assertTrue(state.hold(2L, target("B"), 4).targets().isEmpty());
        assertTrue(state.hold(3L, target("C"), 4).targets().isEmpty());
        assertTrue(state.hold(4L, target("C"), 4).targets().isEmpty());
        assertEquals(List.of("B", "C", "D"), state.hold(4L, target("D"), 4).targets());
        assertEquals(List.of("E"), state.hold(4L, target("E"), 4).targets());
    }

    @Test
    void accurateModeDoesNotReprocessASeenTargetAfterATemporaryMiss()
    {
        CreatorPlacementRepeatState<String, String> state = new CreatorPlacementRepeatState<>();
        state.press(0L, target("A"), CreatorPlacementRepeatMode.ACCURATE, 4);

        assertTrue(state.hold(1L, null, 4).targets().isEmpty());
        assertTrue(state.hold(4L, target("A"), 4).targets().isEmpty());
        assertEquals(List.of("B"), state.hold(4L, target("B"), 4).targets());
    }

    @Test
    void releaseClearsCooldownBackfillAndTargetHistory()
    {
        CreatorPlacementRepeatState<String, String> state = new CreatorPlacementRepeatState<>();
        state.press(0L, target("A"), CreatorPlacementRepeatMode.ACCURATE, 4);
        state.hold(1L, target("B"), 4);
        state.reset();

        assertFalse(state.isActive());
        assertFalse(state.shouldObserveHeld(10L));
        assertEquals(List.of("A"), state.press(10L, target("A"), CreatorPlacementRepeatMode.ACCURATE, 4).targets());
    }

    private static CreatorPlacementRepeatState.ObservedTarget<String, String> target(String value)
    {
        return new CreatorPlacementRepeatState.ObservedTarget<>(value, value);
    }
}
