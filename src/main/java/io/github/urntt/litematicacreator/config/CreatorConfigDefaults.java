package io.github.urntt.litematicacreator.config;

final class CreatorConfigDefaults
{
    static final boolean OPEN_CREATOR_INVENTORY_WITH_INVENTORY_KEY = true;
    static final int CREATOR_EDIT_RANGE = 10;
    static final int CREATOR_EDIT_RANGE_MIN = 1;
    static final int CREATOR_EDIT_RANGE_MAX = 128;
    static final int AIR_PLACEMENT_DISTANCE = 5;
    static final int AIR_PLACEMENT_DISTANCE_MIN = 1;
    static final int AIR_PLACEMENT_DISTANCE_MAX = 128;
    static final int CONTINUOUS_PLACE_INTERVAL_TICKS = 4;
    static final int CONTINUOUS_PLACE_INTERVAL_TICKS_MIN = 1;
    static final int CONTINUOUS_PLACE_INTERVAL_TICKS_MAX = 20;
    static final int CONTINUOUS_BREAK_INTERVAL_TICKS = 4;
    static final int CONTINUOUS_BREAK_INTERVAL_TICKS_MIN = 1;
    static final int CONTINUOUS_BREAK_INTERVAL_TICKS_MAX = 20;
    static final double CREATOR_CAMERA_GROUND_SPEED_MULTIPLIER = 1.0;
    static final double CREATOR_CAMERA_FLIGHT_SPEED_MULTIPLIER = 1.0;
    static final double CREATOR_CAMERA_SPEED_MULTIPLIER_MIN = 0.1;
    static final double CREATOR_CAMERA_SPEED_MULTIPLIER_MAX = 5.0;

    static final String TOGGLE_CREATOR_MODE = "Y";
    static final String OPEN_CREATOR_INVENTORY = "M,E";
    static final String OPEN_CONFIG_GUI = "M,K";
    static final String SAVE_DRAFT = "M,LEFT_SHIFT,S";
    static final String DISCARD_DRAFT = "M,LEFT_SHIFT,D";
    static final String OPEN_FOCUS_SWITCHER = "M,F";
    static final String NEW_BLANK = "M,N";

    private CreatorConfigDefaults()
    {
    }
}
