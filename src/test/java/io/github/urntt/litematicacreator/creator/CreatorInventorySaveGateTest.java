package io.github.urntt.litematicacreator.creator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatorInventorySaveGateTest
{
    @Test
    void savesOnceAfterNestedTransaction()
    {
        CreatorInventorySaveGate gate = new CreatorInventorySaveGate();
        gate.begin();
        gate.begin();

        assertFalse(gate.markChanged());
        assertFalse(gate.markChanged());
        assertFalse(gate.end());
        assertTrue(gate.end());
    }

    @Test
    void savesImmediatelyOutsideTransaction()
    {
        assertTrue(new CreatorInventorySaveGate().markChanged());
    }
}
