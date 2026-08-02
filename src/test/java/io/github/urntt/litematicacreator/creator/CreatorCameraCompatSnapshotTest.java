package io.github.urntt.litematicacreator.creator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatorCameraCompatSnapshotTest
{
    @Test
    void snapshotRetainsBothOriginalTweakerooValues()
    {
        CreatorCameraCompat.TweakerooConfigSnapshot snapshot =
                new CreatorCameraCompat.TweakerooConfigSnapshot(true, false);

        assertTrue(snapshot.playerInputs());
        assertFalse(snapshot.playerMovement());
    }
}
