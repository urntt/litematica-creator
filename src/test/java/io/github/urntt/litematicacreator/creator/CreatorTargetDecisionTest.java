package io.github.urntt.litematicacreator.creator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreatorTargetDecisionTest
{
    @Test
    void uniqueCandidateWinsRegardlessOfFocus()
    {
        assertEquals(CreatorTargetDecision.Action.EDIT_CANDIDATE, CreatorTargetDecision.decide(1, false));
        assertEquals(CreatorTargetDecision.Action.EDIT_CANDIDATE, CreatorTargetDecision.decide(1, true));
    }

    @Test
    void overlapAlwaysRequiresChoice()
    {
        assertEquals(CreatorTargetDecision.Action.CHOOSE_OVERLAP, CreatorTargetDecision.decide(2, false));
        assertEquals(CreatorTargetDecision.Action.CHOOSE_OVERLAP, CreatorTargetDecision.decide(3, true));
    }

    @Test
    void outsideAllRangesExtendsFocusOrCreatesBlank()
    {
        assertEquals(CreatorTargetDecision.Action.EDIT_FOCUS, CreatorTargetDecision.decide(0, true));
        assertEquals(CreatorTargetDecision.Action.CREATE_NEW, CreatorTargetDecision.decide(0, false));
    }
}
