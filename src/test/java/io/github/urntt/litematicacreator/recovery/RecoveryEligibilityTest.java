package io.github.urntt.litematicacreator.recovery;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class RecoveryEligibilityTest
{
    @Test
    void nonFileBackedSchematicsAreAlwaysEligible()
    {
        assertTrue(RecoveryEligibility.shouldCache(null, false));
        assertTrue(RecoveryEligibility.shouldCache(null, true));
    }

    @Test
    void fileBackedSchematicsAreOnlyEligibleWhenDirty()
    {
        Path file = Path.of("build.litematic");
        assertFalse(RecoveryEligibility.shouldCache(file, false));
        assertTrue(RecoveryEligibility.shouldCache(file, true));
    }
}
