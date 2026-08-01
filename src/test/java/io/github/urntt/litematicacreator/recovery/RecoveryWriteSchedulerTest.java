package io.github.urntt.litematicacreator.recovery;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RecoveryWriteSchedulerTest
{
    @Test
    void writesAfterFiveIdleSeconds()
    {
        RecoveryWriteScheduler scheduler = new RecoveryWriteScheduler();
        scheduler.markChanged(40L);

        assertFalse(scheduler.shouldWrite(139L));
        assertTrue(scheduler.shouldWrite(140L));
    }

    @Test
    void continuousChangesWriteAtThirtySecondMaximum()
    {
        RecoveryWriteScheduler scheduler = new RecoveryWriteScheduler();
        scheduler.markChanged(20L);

        for (long tick = 40L; tick <= 600L; tick += 20L)
        {
            scheduler.markChanged(tick);
            assertFalse(scheduler.shouldWrite(tick));
        }

        scheduler.markChanged(620L);
        assertTrue(scheduler.shouldWrite(620L));
    }

    @Test
    void submittingResetsThePendingWindow()
    {
        RecoveryWriteScheduler scheduler = new RecoveryWriteScheduler();
        scheduler.markChanged(10L);
        scheduler.markSubmitted();

        assertFalse(scheduler.hasPendingChanges());
        assertFalse(scheduler.shouldWrite(1_000L));
    }
}
