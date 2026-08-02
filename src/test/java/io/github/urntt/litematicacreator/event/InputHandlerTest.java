package io.github.urntt.litematicacreator.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputHandlerTest
{
    @Test
    void swapsOnlyOnAnUnclaimedFreshPress()
    {
        assertTrue(InputHandler.shouldSwapOffhand(true, false, false));
        assertFalse(InputHandler.shouldSwapOffhand(true, true, false));
        assertFalse(InputHandler.shouldSwapOffhand(false, true, false));
        assertFalse(InputHandler.shouldSwapOffhand(true, false, true));
    }
}
