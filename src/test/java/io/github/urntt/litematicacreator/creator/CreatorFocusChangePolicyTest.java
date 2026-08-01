package io.github.urntt.litematicacreator.creator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatorFocusChangePolicyTest
{
    @Test
    void samePlacementIsNotAChange()
    {
        Object placement = new Object();
        CreatorFocusChangePolicy.Decision decision = CreatorFocusChangePolicy.decide(placement, placement, true);

        assertFalse(decision.changed());
        assertFalse(decision.notifyPlayer());
    }

    @Test
    void differentPlacementIsAVisibleChange()
    {
        CreatorFocusChangePolicy.Decision decision = CreatorFocusChangePolicy.decide(new Object(), new Object(), true);

        assertTrue(decision.changed());
        assertTrue(decision.notifyPlayer());
    }

    @Test
    void lifecycleAndRecoveryChangesRemainSilent()
    {
        CreatorFocusChangePolicy.Decision restore = CreatorFocusChangePolicy.decide(null, new Object(), false);
        CreatorFocusChangePolicy.Decision clear = CreatorFocusChangePolicy.decide(new Object(), null, false);

        assertTrue(restore.changed());
        assertFalse(restore.notifyPlayer());
        assertTrue(clear.changed());
        assertFalse(clear.notifyPlayer());
    }

    @Test
    void visibleClearNotifiesOnlyWhenFocusExisted()
    {
        CreatorFocusChangePolicy.Decision clear = CreatorFocusChangePolicy.decide(new Object(), null, true);
        CreatorFocusChangePolicy.Decision alreadyClear = CreatorFocusChangePolicy.decide(null, null, true);

        assertTrue(clear.changed());
        assertTrue(clear.notifyPlayer());
        assertFalse(alreadyClear.changed());
        assertFalse(alreadyClear.notifyPlayer());
    }
}
