package com.b0btheskull.assistExcavation.client;

public class Common {
    // Allowed value ranges
    public static final int MIN_DELAY_TICKS = 0;      // minimum delay
    public static final int MAX_DELAY_TICKS = 40;     // maximum delay
    public static final int MIN_REACH = 1;            // minimum reach
    public static final int MAX_REACH = 6;            // maximum reach
    public static final int MIN_EXCAVATION_MODE = 0;  // minimum excavation mode
    public static final int MAX_EXCAVATION_MODE = 2;  // maximum excavation mode

    private static Integer delayTicks = 0;            // default delay between blocks
    private static Integer reach = 1;                 // default reach
    private static Integer excavationMode = 0;        // default: rectangle range
    private static boolean autoToolSwitch = true;     // switch to the fastest tool (default on)
    private static boolean fastInstantBreak = true;   // break multiple instant blocks per tick (default on)
    private static boolean autoBridge = false;        // auto-place a block under your feet (default off)

    // delayTicks getter/setter
    public static Integer getDelayTicks() {
        return delayTicks;
    }

    public static void setDelayTicks(Integer delayTicks) {
        if (delayTicks != null) {
            // clamp delayTicks to the valid range
            Common.delayTicks = Math.max(MIN_DELAY_TICKS, Math.min(MAX_DELAY_TICKS, delayTicks));
        }
    }

    // reach getter/setter
    public static Integer getReach() {
        return reach;
    }

    public static void setReach(Integer reach) {
        if (reach != null) {
            // clamp reach to the valid range
            Common.reach = Math.max(MIN_REACH, Math.min(MAX_REACH, reach));
        }
    }

    // excavationMode getter/setter
    public static Integer getExcavationMode() {
        return excavationMode;
    }

    public static void setExcavationMode(Integer excavationMode) {
        if (excavationMode != null) {
            // clamp excavationMode to the valid range
            Common.excavationMode = Math.max(MIN_EXCAVATION_MODE, Math.min(MAX_EXCAVATION_MODE, excavationMode));
        }
    }

    // autoToolSwitch getter/setter
    public static boolean isAutoToolSwitch() {
        return autoToolSwitch;
    }

    public static void setAutoToolSwitch(boolean autoToolSwitch) {
        Common.autoToolSwitch = autoToolSwitch;
    }

    // fastInstantBreak getter/setter
    public static boolean isFastInstantBreak() {
        return fastInstantBreak;
    }

    public static void setFastInstantBreak(boolean fastInstantBreak) {
        Common.fastInstantBreak = fastInstantBreak;
    }

    // autoBridge getter/setter
    public static boolean isAutoBridge() {
        return autoBridge;
    }

    public static void setAutoBridge(boolean autoBridge) {
        Common.autoBridge = autoBridge;
    }
}
