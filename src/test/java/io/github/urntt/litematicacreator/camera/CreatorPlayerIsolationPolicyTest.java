package io.github.urntt.litematicacreator.camera;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatorPlayerIsolationPolicyTest
{
    @Test
    void onlyTheCurrentSessionPlayerIsIsolated()
    {
        assertTrue(CreatorPlayerIsolationPolicy.shouldIsolate(true, true, true));
        assertFalse(CreatorPlayerIsolationPolicy.shouldIsolate(false, true, true));
        assertFalse(CreatorPlayerIsolationPolicy.shouldIsolate(true, false, true));
        assertFalse(CreatorPlayerIsolationPolicy.shouldIsolate(true, true, false));
    }
}
