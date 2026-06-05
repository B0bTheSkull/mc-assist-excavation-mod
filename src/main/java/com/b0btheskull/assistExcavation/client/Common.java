package com.b0btheskull.assistExcavation.client;

import java.util.LinkedHashSet;
import java.util.Set;

public class Common {
    // Allowed value ranges
    public static final int MIN_DELAY_TICKS = 0;      // minimum delay
    public static final int MAX_DELAY_TICKS = 40;     // maximum delay
    public static final int MIN_REACH = 1;            // minimum reach
    public static final int MAX_REACH = 6;            // maximum reach
    public static final int MIN_EXCAVATION_MODE = 0;  // minimum excavation mode
    public static final int MAX_EXCAVATION_MODE = 3;  // 0=rectangle 1=sphere 2=tunnel 3=vein
    public static final int MIN_DURABILITY = 0;       // 0 = guard disabled
    public static final int MAX_DURABILITY = 200;
    public static final int MIN_LAVA_GUARD_RADIUS = 1; // lava-guard scan radius bounds
    public static final int MAX_LAVA_GUARD_RADIUS = 6;

    private static Integer delayTicks = 0;            // default delay between blocks
    private static Integer reach = 1;                 // default reach
    private static Integer excavationMode = 0;        // default: rectangle range
    private static boolean tunnel3x3 = false;         // tunnel cross-section: false=1x2, true=3x3
    private static boolean autoToolSwitch = true;     // switch to the fastest tool (default on)
    private static boolean fastInstantBreak = true;   // break multiple instant blocks per tick (default on)
    private static boolean autoBridge = false;        // auto-place a block under your feet (default off)

    // Tool-safety guard
    private static Integer durabilityThreshold = 0;   // stop using a tool at/below this remaining durability (0=off)
    private static boolean protectEnchanted = true;   // never auto-switch away from Silk Touch / Fortune tools
    private static boolean restockTools = true;        // when a tool hits the durability guard, pull a fresh one from the inventory

    // Block protection (blacklist)
    private static boolean protectBlockEntities = true;            // never break chests, spawners, etc.
    private static final Set<String> blockBlacklist = new LinkedHashSet<>(); // extra block ids, e.g. "minecraft:bedrock"

    // Server-safe: disable detectable techniques (silent-rotation bridge, packet bursts)
    private static boolean serverSafe = false;

    // Render the outline of blocks that will be mined
    private static boolean previewOverlay = false;

    // Lava guard: while assist mining, suspend mining to cap a nearby lava SOURCE block so you
    // don't dig yourself into a burn. Off by default; radius is its own scan range (in blocks).
    private static boolean lavaGuard = false;
    private static Integer lavaGuardRadius = 4;

    // Stats HUD: top-right readout of mode / state / held-tool durability / session blocks mined.
    private static boolean hudStats = false;

    // delayTicks getter/setter
    public static Integer getDelayTicks() {
        return delayTicks;
    }

    public static void setDelayTicks(Integer delayTicks) {
        if (delayTicks != null) {
            Common.delayTicks = Math.max(MIN_DELAY_TICKS, Math.min(MAX_DELAY_TICKS, delayTicks));
        }
    }

    // reach getter/setter
    public static Integer getReach() {
        return reach;
    }

    public static void setReach(Integer reach) {
        if (reach != null) {
            Common.reach = Math.max(MIN_REACH, Math.min(MAX_REACH, reach));
        }
    }

    // excavationMode getter/setter
    public static Integer getExcavationMode() {
        return excavationMode;
    }

    public static void setExcavationMode(Integer excavationMode) {
        if (excavationMode != null) {
            Common.excavationMode = Math.max(MIN_EXCAVATION_MODE, Math.min(MAX_EXCAVATION_MODE, excavationMode));
        }
    }

    // tunnel3x3 getter/setter
    public static boolean isTunnel3x3() {
        return tunnel3x3;
    }

    public static void setTunnel3x3(boolean tunnel3x3) {
        Common.tunnel3x3 = tunnel3x3;
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

    // durabilityThreshold getter/setter
    public static Integer getDurabilityThreshold() {
        return durabilityThreshold;
    }

    public static void setDurabilityThreshold(Integer durabilityThreshold) {
        if (durabilityThreshold != null) {
            Common.durabilityThreshold = Math.max(MIN_DURABILITY, Math.min(MAX_DURABILITY, durabilityThreshold));
        }
    }

    // protectEnchanted getter/setter
    public static boolean isProtectEnchanted() {
        return protectEnchanted;
    }

    public static void setProtectEnchanted(boolean protectEnchanted) {
        Common.protectEnchanted = protectEnchanted;
    }

    // restockTools getter/setter
    public static boolean isRestockTools() {
        return restockTools;
    }

    public static void setRestockTools(boolean restockTools) {
        Common.restockTools = restockTools;
    }

    // protectBlockEntities getter/setter
    public static boolean isProtectBlockEntities() {
        return protectBlockEntities;
    }

    public static void setProtectBlockEntities(boolean protectBlockEntities) {
        Common.protectBlockEntities = protectBlockEntities;
    }

    // blockBlacklist accessors
    public static Set<String> getBlockBlacklist() {
        return blockBlacklist;
    }

    public static void setBlockBlacklist(java.util.Collection<String> ids) {
        blockBlacklist.clear();
        if (ids != null) {
            for (String id : ids) {
                if (id != null && !id.isBlank()) {
                    blockBlacklist.add(id.trim());
                }
            }
        }
    }

    // serverSafe getter/setter
    public static boolean isServerSafe() {
        return serverSafe;
    }

    public static void setServerSafe(boolean serverSafe) {
        Common.serverSafe = serverSafe;
    }

    // previewOverlay getter/setter
    public static boolean isPreviewOverlay() {
        return previewOverlay;
    }

    public static void setPreviewOverlay(boolean previewOverlay) {
        Common.previewOverlay = previewOverlay;
    }

    // lavaGuard getter/setter
    public static boolean isLavaGuard() {
        return lavaGuard;
    }

    public static void setLavaGuard(boolean lavaGuard) {
        Common.lavaGuard = lavaGuard;
    }

    // lavaGuardRadius getter/setter
    public static Integer getLavaGuardRadius() {
        return lavaGuardRadius;
    }

    public static void setLavaGuardRadius(Integer lavaGuardRadius) {
        if (lavaGuardRadius != null) {
            Common.lavaGuardRadius = Math.max(MIN_LAVA_GUARD_RADIUS,
                    Math.min(MAX_LAVA_GUARD_RADIUS, lavaGuardRadius));
        }
    }

    // hudStats getter/setter
    public static boolean isHudStats() {
        return hudStats;
    }

    public static void setHudStats(boolean hudStats) {
        Common.hudStats = hudStats;
    }
}
