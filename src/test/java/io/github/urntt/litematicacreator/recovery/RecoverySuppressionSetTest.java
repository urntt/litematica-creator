package io.github.urntt.litematicacreator.recovery;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RecoverySuppressionSetTest
{
    @Test
    void lastPlacementRemovalSuppressesUntilPlacementIsAddedAgain()
    {
        RecoverySuppressionSet<Object> suppression = new RecoverySuppressionSet<>();
        Object schematic = new Object();

        suppression.suppress(schematic);
        assertTrue(suppression.contains(schematic));

        suppression.release(schematic);
        assertFalse(suppression.contains(schematic));
    }

    @Test
    void suppressionUsesSchematicIdentity()
    {
        RecoverySuppressionSet<String> suppression = new RecoverySuppressionSet<>();
        String first = new String("schematic");
        String equalButDistinct = new String("schematic");

        suppression.suppress(first);

        assertTrue(suppression.contains(first));
        assertFalse(suppression.contains(equalButDistinct));
    }
}
