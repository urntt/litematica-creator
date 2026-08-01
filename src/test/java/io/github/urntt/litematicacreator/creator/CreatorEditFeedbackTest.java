package io.github.urntt.litematicacreator.creator;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
