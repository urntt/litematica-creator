package io.github.urntt.litematicacreator.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigDefaultsTest
{
    @Test
    void inventoryKeyOverrideDefaultsToEnabled()
    {
        assertTrue(CreatorConfigDefaults.OPEN_CREATOR_INVENTORY_WITH_INVENTORY_KEY);
    }

    @Test
    void creatorEditRangeUsesTheExpectedBounds()
    {
        assertEquals(10, CreatorConfigDefaults.CREATOR_EDIT_RANGE);
        assertEquals(1, CreatorConfigDefaults.CREATOR_EDIT_RANGE_MIN);
        assertEquals(128, CreatorConfigDefaults.CREATOR_EDIT_RANGE_MAX);
    }

    @Test
    void creatorHotkeysUseTheExpectedDefaults()
    {
        assertEquals("Y", CreatorConfigDefaults.TOGGLE_CREATOR_MODE);
        assertEquals("M,E", CreatorConfigDefaults.OPEN_CREATOR_INVENTORY);
        assertEquals("M,K", CreatorConfigDefaults.OPEN_CONFIG_GUI);
        assertEquals("M,LEFT_SHIFT,S", CreatorConfigDefaults.SAVE_DRAFT);
        assertEquals("M,LEFT_SHIFT,D", CreatorConfigDefaults.DISCARD_DRAFT);
        assertEquals("M,F", CreatorConfigDefaults.OPEN_FOCUS_SWITCHER);
        assertEquals("M,N", CreatorConfigDefaults.NEW_BLANK);
    }
}
