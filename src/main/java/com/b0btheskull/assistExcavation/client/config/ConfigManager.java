package com.b0btheskull.assistExcavation.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.b0btheskull.assistExcavation.client.AssistExcavationClient;
import com.b0btheskull.assistExcavation.client.Common;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads/saves {@link Common} settings to {@code config/assist-excavation.json} so they survive
 * a game restart. The on-disk shape is a flat POJO ({@link ConfigData}); everything funnels
 * through {@link Common} at runtime.
 */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE =
            FabricLoader.getInstance().getConfigDir().resolve("assist-excavation.json");

    private ConfigManager() {
    }

    /** Mirror of the persisted fields. Field names are the JSON keys. */
    public static final class ConfigData {
        int delayTicks = 0;
        int reach = 1;
        int excavationMode = 0;
        boolean tunnel3x3 = false;
        boolean autoToolSwitch = true;
        boolean fastInstantBreak = true;
        boolean autoBridge = false;
        int durabilityThreshold = 0;
        boolean protectEnchanted = true;
        boolean restockTools = true;
        boolean protectBlockEntities = true;
        List<String> blockBlacklist = new ArrayList<>();
        boolean serverSafe = false;
        boolean previewOverlay = false;
        boolean lavaGuard = false;
        int lavaGuardRadius = 4;
    }

    /** Read the file into {@link Common}. Writes defaults if the file is missing or unreadable. */
    public static void load() {
        if (!Files.exists(FILE)) {
            save();
            return;
        }
        try (Reader r = Files.newBufferedReader(FILE)) {
            ConfigData data = GSON.fromJson(r, ConfigData.class);
            if (data == null) {
                data = new ConfigData();
            }
            apply(data);
        } catch (Exception e) {
            AssistExcavationClient.LOGGER.warn("Failed to read config, using defaults", e);
        }
    }

    /** Write the current {@link Common} values to disk. */
    public static void save() {
        ConfigData data = snapshot();
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer w = Files.newBufferedWriter(FILE)) {
                GSON.toJson(data, w);
            }
        } catch (IOException e) {
            AssistExcavationClient.LOGGER.warn("Failed to write config", e);
        }
    }

    private static void apply(ConfigData d) {
        Common.setDelayTicks(d.delayTicks);
        Common.setReach(d.reach);
        Common.setExcavationMode(d.excavationMode);
        Common.setTunnel3x3(d.tunnel3x3);
        Common.setAutoToolSwitch(d.autoToolSwitch);
        Common.setFastInstantBreak(d.fastInstantBreak);
        Common.setAutoBridge(d.autoBridge);
        Common.setDurabilityThreshold(d.durabilityThreshold);
        Common.setProtectEnchanted(d.protectEnchanted);
        Common.setRestockTools(d.restockTools);
        Common.setProtectBlockEntities(d.protectBlockEntities);
        Common.setBlockBlacklist(d.blockBlacklist);
        Common.setServerSafe(d.serverSafe);
        Common.setPreviewOverlay(d.previewOverlay);
        Common.setLavaGuard(d.lavaGuard);
        Common.setLavaGuardRadius(d.lavaGuardRadius);
    }

    private static ConfigData snapshot() {
        ConfigData d = new ConfigData();
        d.delayTicks = Common.getDelayTicks();
        d.reach = Common.getReach();
        d.excavationMode = Common.getExcavationMode();
        d.tunnel3x3 = Common.isTunnel3x3();
        d.autoToolSwitch = Common.isAutoToolSwitch();
        d.fastInstantBreak = Common.isFastInstantBreak();
        d.autoBridge = Common.isAutoBridge();
        d.durabilityThreshold = Common.getDurabilityThreshold();
        d.protectEnchanted = Common.isProtectEnchanted();
        d.restockTools = Common.isRestockTools();
        d.protectBlockEntities = Common.isProtectBlockEntities();
        d.blockBlacklist = new ArrayList<>(Common.getBlockBlacklist());
        d.serverSafe = Common.isServerSafe();
        d.previewOverlay = Common.isPreviewOverlay();
        d.lavaGuard = Common.isLavaGuard();
        d.lavaGuardRadius = Common.getLavaGuardRadius();
        return d;
    }
}
