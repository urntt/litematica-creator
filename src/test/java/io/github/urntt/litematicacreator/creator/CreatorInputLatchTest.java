package io.github.urntt.litematicacreator.creator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatorInputLatchTest
{
    @Test
    void quickPressAndReleaseKeepsOneFreshPress()
    {
        CreatorInputLatch latch = new CreatorInputLatch();

        latch.update(true, true);
        latch.update(false, true);

        assertTrue(latch.consumePress());
        assertFalse(latch.consumePress());
        assertFalse(latch.isHeld());
    }

    @Test
    void repeatedPressEventsDoNotCreateAdditionalFreshPresses()
    {
        CreatorInputLatch latch = new CreatorInputLatch();

        latch.update(true, true);
        latch.update(true, true);
        latch.update(true, true);

        assertTrue(latch.consumePress());
        assertFalse(latch.consumePress());
        assertTrue(latch.isHeld());
    }

    @Test
    void interruptionRequiresReleaseBeforeRearming()
    {
        CreatorInputLatch latch = new CreatorInputLatch();
        latch.update(true, true);
        latch.suspend(true);

        latch.sync(true, true);
        assertFalse(latch.consumePress());
        assertFalse(latch.isHeld());

        latch.update(false, true);
        latch.update(true, true);
        assertTrue(latch.consumePress());
        assertTrue(latch.isHeld());
    }

    @Test
    void pressOutsideAnEditableWorldIsBlockedUntilRelease()
    {
        CreatorInputLatch latch = new CreatorInputLatch();
        latch.update(true, false);

        latch.sync(true, true);
        assertFalse(latch.consumePress());

        latch.update(false, true);
        latch.update(true, true);
        assertTrue(latch.consumePress());
    }
}
