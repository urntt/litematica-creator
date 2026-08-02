package io.github.urntt.litematicacreator.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatorTranslationApplyGateTest
{
    @Test
    void loadingAndReentrantApplicationAreSuppressed()
    {
        CreatorTranslationApplyGate gate = new CreatorTranslationApplyGate();

        gate.beginLoading();
        assertFalse(gate.beginApplying());
        gate.endLoading();

        assertTrue(gate.beginApplying());
        assertFalse(gate.beginApplying());
        gate.endApplying();
        assertTrue(gate.beginApplying());
        gate.endApplying();
    }
}
