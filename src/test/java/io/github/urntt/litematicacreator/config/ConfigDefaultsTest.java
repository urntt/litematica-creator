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
    void airPlacementDistanceUsesTheExpectedDefaultAndBounds()
    {
        assertEquals(5, CreatorConfigDefaults.AIR_PLACEMENT_DISTANCE);
        assertEquals(1, CreatorConfigDefaults.AIR_PLACEMENT_DISTANCE_MIN);
        assertEquals(128, CreatorConfigDefaults.AIR_PLACEMENT_DISTANCE_MAX);
    }

    @Test
    void continuousBreakUsesTheExpectedDefaultAndBounds()
    {
        assertEquals(4, CreatorConfigDefaults.CONTINUOUS_BREAK_INTERVAL_TICKS);
        assertEquals(1, CreatorConfigDefaults.CONTINUOUS_BREAK_INTERVAL_TICKS_MIN);
        assertEquals(20, CreatorConfigDefaults.CONTINUOUS_BREAK_INTERVAL_TICKS_MAX);
    }

    @Test
    void continuousPlaceUsesTheExpectedDefaultAndBounds()
    {
        assertEquals(4, CreatorConfigDefaults.CONTINUOUS_PLACE_INTERVAL_TICKS);
        assertEquals(1, CreatorConfigDefaults.CONTINUOUS_PLACE_INTERVAL_TICKS_MIN);
        assertEquals(20, CreatorConfigDefaults.CONTINUOUS_PLACE_INTERVAL_TICKS_MAX);
    }

    @Test
    void creatorCameraSpeedsUseTheExpectedDefaultsAndBounds()
    {
        assertEquals(1.0, CreatorConfigDefaults.CREATOR_CAMERA_GROUND_SPEED_MULTIPLIER);
        assertEquals(1.0, CreatorConfigDefaults.CREATOR_CAMERA_FLIGHT_SPEED_MULTIPLIER);
        assertEquals(0.1, CreatorConfigDefaults.CREATOR_CAMERA_SPEED_MULTIPLIER_MIN);
        assertEquals(5.0, CreatorConfigDefaults.CREATOR_CAMERA_SPEED_MULTIPLIER_MAX);
        assertTrue(CreatorConfigDefaults.ENABLE_CREATOR_CAMERA_WITH_CREATOR_MODE);
        assertTrue(CreatorConfigDefaults.DISABLE_CREATOR_CAMERA_WITH_CREATOR_MODE);
        assertTrue(CreatorConfigDefaults.CREATOR_CAMERA_PROJECTION_COLLISION);
    }

    @Test
    void creatorHotkeysUseTheExpectedDefaults()
    {
        assertEquals("Y", CreatorConfigDefaults.TOGGLE_CREATOR_MODE);
        assertEquals("M,B", CreatorConfigDefaults.TOGGLE_CREATOR_CAMERA);
        assertEquals("M,E", CreatorConfigDefaults.OPEN_CREATOR_INVENTORY);
        assertEquals("M,K", CreatorConfigDefaults.OPEN_CONFIG_GUI);
        assertEquals("M,LEFT_SHIFT,S", CreatorConfigDefaults.SAVE_DRAFT);
        assertEquals("M,LEFT_SHIFT,D", CreatorConfigDefaults.DISCARD_DRAFT);
        assertEquals("M,F", CreatorConfigDefaults.OPEN_FOCUS_SWITCHER);
        assertEquals("M,N", CreatorConfigDefaults.NEW_BLANK);
    }
}
