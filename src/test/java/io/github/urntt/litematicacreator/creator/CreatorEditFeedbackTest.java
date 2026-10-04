package io.github.urntt.litematicacreator.creator;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatorEditFeedbackTest
{
    @Test
    void runsFeedbackOnlyForSuccessfulStateChanges()
    {
        AtomicInteger feedbackCount = new AtomicInteger();

        assertFalse(CreatorEditFeedback.afterSuccessfulEdit(false, feedbackCount::incrementAndGet));
        assertEquals(0, feedbackCount.get());
        assertTrue(CreatorEditFeedback.afterSuccessfulEdit(true, feedbackCount::incrementAndGet));
        assertEquals(1, feedbackCount.get());
    }

    @Test
    void targetsTheCreatorCameraWhenAvailable()
    {
        Object player = new Object();
        Object camera = new Object();

        assertSame(camera, CreatorEditFeedback.feedbackTarget(player, camera));
        assertSame(player, CreatorEditFeedback.feedbackTarget(player, null));
    }

    @Test
    void swingsTheCameraOnlyForAttacksThatTargetNoProjection()
    {
        assertTrue(CreatorEditFeedback.swingsWithoutEdit(false, true));
        assertFalse(CreatorEditFeedback.swingsWithoutEdit(true, true));
    }

    @Test
    void neverSwingsTheRealPlayerWithoutAnEdit()
    {
        assertFalse(CreatorEditFeedback.swingsWithoutEdit(false, false));
    }
}
