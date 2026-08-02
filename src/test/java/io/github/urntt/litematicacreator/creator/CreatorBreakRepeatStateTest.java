package io.github.urntt.litematicacreator.creator;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatorBreakRepeatStateTest
{
    @Test
    void breakRunsImmediatelyThenSamplesAtTheConfiguredInterval()
    {
        CreatorBreakRepeatState<String, String> state = new CreatorBreakRepeatState<>();

        assertEquals(List.of("A"), state.press(10L, target("A"), 4).targets());
        assertFalse(state.shouldObserveHeld(13L));
        assertTrue(state.shouldObserveHeld(14L));
        assertTrue(state.hold(14L, target("A"), 4).targets().isEmpty());
        assertFalse(state.shouldObserveHeld(17L));
        assertEquals(List.of("B"), state.hold(18L, target("B"), 4).targets());
    }

    @Test
    void noTargetDoesNotProduceAnEditAndAllowsAReacquiredTarget()
    {
        CreatorBreakRepeatState<String, String> state = new CreatorBreakRepeatState<>();
        assertTrue(state.press(0L, null, 4).targets().isEmpty());
        assertEquals(List.of("A"), state.hold(4L, target("A"), 4).targets());

        assertTrue(state.hold(8L, null, 4).targets().isEmpty());
        assertEquals(List.of("A"), state.hold(12L, target("A"), 4).targets());
    }

    @Test
    void releaseStopsContinuousBreak()
    {
        CreatorBreakRepeatState<String, String> state = new CreatorBreakRepeatState<>();
        state.press(0L, target("A"), 1);
        state.reset();

        assertFalse(state.shouldObserveHeld(1L));
        assertTrue(state.hold(1L, target("B"), 1).targets().isEmpty());
    }

    private static CreatorBreakRepeatState.ObservedTarget<String, String> target(String value)
    {
        return new CreatorBreakRepeatState.ObservedTarget<>(value, value);
    }
}
