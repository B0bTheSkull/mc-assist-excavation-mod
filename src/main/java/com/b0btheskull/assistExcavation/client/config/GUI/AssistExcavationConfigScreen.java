package com.b0btheskull.assistExcavation.client.config.GUI;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.b0btheskull.assistExcavation.client.Common;
import com.b0btheskull.assistExcavation.client.config.ConfigManager;

import java.util.List;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class AssistExcavationConfigScreen extends Screen {
    // Tab categories.
    private static final int TAB_MINING = 0;
    private static final int TAB_TOOLS = 1;
    private static final int TAB_SAFETY = 2;
    private static final int TAB_DISPLAY = 3;
    private static final int TAB_PRESETS = 4;
    private static final String[] TAB_KEYS = {"mining", "tools", "safety", "display", "presets"};

    private final Screen parent;

    // Currently visible tab. Static so it survives reopening the screen within a session.
    private static int category = TAB_MINING;

    // Pending (unsaved) config values — edited in the UI, flushed to Common only on Done.
    private int delayTicks;
    private int reach;
    private int excavationMode;   // 0=rect, 1=sphere, 2=tunnel, 3=vein
    private boolean tunnel3x3;
    private boolean autoToolSwitch;
    private boolean fastInstantBreak;
    private boolean autoBridge;
    private int durabilityThreshold;
    private boolean protectEnchanted;
    private boolean restockTools;
    private boolean protectBlockEntities;
    private boolean serverSafe;
    private boolean previewOverlay;
    private boolean lavaGuard;
    private int lavaGuardRadius;
    private boolean spectatorHud;
    private boolean spectatorGlow;
    private boolean hudStats;

    // Y of the info line, computed in init() per tab.
    private int infoY;

    // Presets-tab state.
    private EditBox presetNameBox;
    private List<String> presetList = List.of();
    private int selectedPreset = 0;

    public AssistExcavationConfigScreen(Screen parent) {
        super(Component.translatable("screen.assist-excavation.config.title"));
        this.parent = parent;
        loadFromCommon();
    }

    /** (Re)load all pending UI values from the live {@link Common} config. */
    private void loadFromCommon() {
        this.delayTicks = Common.getDelayTicks();
        this.reach = Common.getReach();
        this.excavationMode = Common.getExcavationMode();
        this.tunnel3x3 = Common.isTunnel3x3();
        this.autoToolSwitch = Common.isAutoToolSwitch();
        this.fastInstantBreak = Common.isFastInstantBreak();
        this.autoBridge = Common.isAutoBridge();
        this.durabilityThreshold = Common.getDurabilityThreshold();
        this.protectEnchanted = Common.isProtectEnchanted();
        this.restockTools = Common.isRestockTools();
        this.protectBlockEntities = Common.isProtectBlockEntities();
        this.serverSafe = Common.isServerSafe();
        this.previewOverlay = Common.isPreviewOverlay();
        this.lavaGuard = Common.isLavaGuard();
        this.lavaGuardRadius = Common.getLavaGuardRadius();
        this.spectatorHud = Common.isSpectatorHud();
        this.spectatorGlow = Common.isSpectatorGlow();
        this.hudStats = Common.isHudStats();
    }

    @Override
    protected void init() {
        super.init();

        final int h = 20;
        final int pitch = 24;
        final int colW = 200;
        int cx = this.width / 2;
        int x = cx - colW / 2;

        // Refresh the preset list each (re)build and keep the selection in range.
        presetList = ConfigManager.presetNames();
        if (selectedPreset >= presetList.size()) {
            selectedPreset = 0;
        }

        // ---- Tab row (wider than the control column so 5 labels fit) ----
        int gap = 4;
        int tabRowW = Math.max(colW, TAB_KEYS.length * 48);
        int tabX0 = cx - tabRowW / 2;
        int catW = (tabRowW - gap * (TAB_KEYS.length - 1)) / TAB_KEYS.length;
        int catY = 28;
        for (int i = 0; i < TAB_KEYS.length; i++) {
            final int idx = i;
            int catX = tabX0 + i * (catW + gap);
            Button tab = Button.builder(
                    Component.translatable("screen.assist-excavation.config.tab." + TAB_KEYS[i]),
                    btn -> {
                        category = idx;
                        rebuildWidgets();
                    }).bounds(catX, catY, catW, h).build();
            tab.active = (i != category); // the active tab is greyed-out (can't click itself)
            addRenderableWidget(tab);
        }

        // ---- Active tab's controls (single centred column) ----
        int y = catY + pitch + 6;
        switch (category) {
            case TAB_MINING -> {
                addRenderableWidget(new DelayTicksSlider(x, y, colW, h,
                        Component.translatable("screen.assist-excavation.config.delayTicks", delayTicks),
                        (double) delayTicks / Common.MAX_DELAY_TICKS));
                y += pitch;
                addRenderableWidget(new ReachSlider(x, y, colW, h,
                        Component.translatable("screen.assist-excavation.config.reach", reach),
                        (double) (reach - 1) / (Common.MAX_REACH - 1)));
                y += pitch;
                addRenderableWidget(toggle(x, y, colW, h, this::getExcavationModeText,
                        () -> excavationMode = (excavationMode + 1) % (Common.MAX_EXCAVATION_MODE + 1)));
                y += pitch;
                addRenderableWidget(toggle(x, y, colW, h, this::getTunnelSizeText,
                        () -> tunnel3x3 = !tunnel3x3));
                y += pitch;
                addRenderableWidget(toggle(x, y, colW, h, this::getFastBreakText,
                        () -> fastInstantBreak = !fastInstantBreak));
                y += pitch;
                addRenderableWidget(toggle(x, y, colW, h, this::getAutoBridgeText,
                        () -> autoBridge = !autoBridge));
                y += pitch;
            }
            case TAB_TOOLS -> {
                addRenderableWidget(toggle(x, y, colW, h, this::getAutoToolText,
                        () -> autoToolSwitch = !autoToolSwitch));
                y += pitch;
                addRenderableWidget(new DurabilitySlider(x, y, colW, h, getDurabilityText(),
                        (double) durabilityThreshold / Common.MAX_DURABILITY));
                y += pitch;
                addRenderableWidget(toggle(x, y, colW, h, this::getRestockText,
                        () -> restockTools = !restockTools));
                y += pitch;
                addRenderableWidget(toggle(x, y, colW, h, this::getProtectEnchantedText,
                        () -> protectEnchanted = !protectEnchanted));
                y += pitch;
            }
            case TAB_SAFETY -> {
                addRenderableWidget(toggle(x, y, colW, h, this::getProtectContainersText,
                        () -> protectBlockEntities = !protectBlockEntities));
                y += pitch;
                addRenderableWidget(toggle(x, y, colW, h, this::getServerSafeText,
                        () -> serverSafe = !serverSafe));
                y += pitch;
                addRenderableWidget(toggle(x, y, colW, h, this::getLavaGuardText,
                        () -> lavaGuard = !lavaGuard));
                y += pitch;
                addRenderableWidget(new LavaRadiusSlider(x, y, colW, h, getLavaRadiusText(),
                        (double) (lavaGuardRadius - Common.MIN_LAVA_GUARD_RADIUS)
                                / (Common.MAX_LAVA_GUARD_RADIUS - Common.MIN_LAVA_GUARD_RADIUS)));
                y += pitch;
            }
            case TAB_DISPLAY -> {
                addRenderableWidget(toggle(x, y, colW, h, this::getPreviewText,
                        () -> previewOverlay = !previewOverlay));
                y += pitch;
                addRenderableWidget(toggle(x, y, colW, h, this::getStatsHudText,
                        () -> hudStats = !hudStats));
                y += pitch;
                addRenderableWidget(toggle(x, y, colW, h, this::getSpectatorHudText,
                        () -> spectatorHud = !spectatorHud));
                y += pitch;
                addRenderableWidget(toggle(x, y, colW, h, this::getSpectatorGlowText,
                        () -> spectatorGlow = !spectatorGlow));
                y += pitch;
            }
            case TAB_PRESETS -> {
                // Name field + "save current as preset".
                presetNameBox = new EditBox(this.font, x, y, colW, h,
                        Component.translatable("screen.assist-excavation.config.preset.name_hint"));
                presetNameBox.setMaxLength(32);
                presetNameBox.setHint(Component.translatable("screen.assist-excavation.config.preset.name_hint"));
                addRenderableWidget(presetNameBox);
                y += pitch;

                addRenderableWidget(Button.builder(
                        Component.translatable("screen.assist-excavation.config.preset.save"),
                        btn -> savePreset()).bounds(x, y, colW, h).build());
                y += pitch + 4;

                // Selected-preset cycler + Load/Delete.
                Button cycle = Button.builder(getPresetCycleText(), btn -> {
                    if (!presetList.isEmpty()) {
                        selectedPreset = (selectedPreset + 1) % presetList.size();
                        btn.setMessage(getPresetCycleText());
                    }
                }).bounds(x, y, colW, h).build();
                cycle.active = !presetList.isEmpty();
                addRenderableWidget(cycle);
                y += pitch;

                int half = (colW - 4) / 2;
                Button load = Button.builder(
                        Component.translatable("screen.assist-excavation.config.preset.load"),
                        btn -> loadPreset()).bounds(x, y, half, h).build();
                load.active = !presetList.isEmpty();
                addRenderableWidget(load);
                Button delete = Button.builder(
                        Component.translatable("screen.assist-excavation.config.preset.delete"),
                        btn -> deletePreset()).bounds(x + colW - half, y, half, h).build();
                delete.active = !presetList.isEmpty();
                addRenderableWidget(delete);
                y += pitch;
            }
            default -> { }
        }

        // ---- Info line + Reset/Done row ----
        this.infoY = y + 2;
        int rowY = y + 10;
        int half = (colW - 4) / 2;
        addRenderableWidget(Button.builder(
                Component.translatable("screen.assist-excavation.config.reset"),
                btn -> resetConfig())
                .bounds(x, rowY, half, h).build());
        addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                btn -> {
                    saveConfig();
                    this.onClose();
                })
                .bounds(x + colW - half, rowY, half, h).build());
    }

    /** A button that flips/advances a setting via {@code onClick} and refreshes its own label. */
    private Button toggle(int x, int y, int w, int h, Supplier<MutableComponent> text, Runnable onClick) {
        return Button.builder(text.get(), btn -> {
            onClick.run();
            btn.setMessage(text.get());
        }).bounds(x, y, w, h).build();
    }

    private MutableComponent getPresetCycleText() {
        if (presetList.isEmpty()) {
            return Component.translatable("screen.assist-excavation.config.preset.none");
        }
        String name = presetList.get(Math.min(selectedPreset, presetList.size() - 1));
        return Component.translatable("screen.assist-excavation.config.preset.selected", name);
    }

    /** Save the current (pending) settings as a named preset. */
    private void savePreset() {
        if (presetNameBox == null) {
            return;
        }
        String name = presetNameBox.getValue().trim();
        if (name.isEmpty()) {
            return;
        }
        // Flush the pending UI values to the live config first, then store them under this name.
        saveConfig();
        ConfigManager.savePreset(name);
        presetList = ConfigManager.presetNames();
        selectedPreset = Math.max(0, presetList.indexOf(name));
        rebuildWidgets();
    }

    /** Apply the selected preset to the live config and reload it into the UI. */
    private void loadPreset() {
        if (presetList.isEmpty()) {
            return;
        }
        String name = presetList.get(Math.min(selectedPreset, presetList.size() - 1));
        if (ConfigManager.applyPreset(name)) {
            loadFromCommon();
        }
        rebuildWidgets();
    }

    /** Delete the selected preset. */
    private void deletePreset() {
        if (presetList.isEmpty()) {
            return;
        }
        String name = presetList.get(Math.min(selectedPreset, presetList.size() - 1));
        ConfigManager.deletePreset(name);
        presetList = ConfigManager.presetNames();
        if (selectedPreset >= presetList.size()) {
            selectedPreset = 0;
        }
        rebuildWidgets();
    }

    private void saveConfig() {
        Common.setDelayTicks(delayTicks);
        Common.setReach(reach);
        Common.setExcavationMode(excavationMode);
        Common.setTunnel3x3(tunnel3x3);
        Common.setAutoToolSwitch(autoToolSwitch);
        Common.setFastInstantBreak(fastInstantBreak);
        Common.setAutoBridge(autoBridge);
        Common.setDurabilityThreshold(durabilityThreshold);
        Common.setProtectEnchanted(protectEnchanted);
        Common.setRestockTools(restockTools);
        Common.setProtectBlockEntities(protectBlockEntities);
        Common.setServerSafe(serverSafe);
        Common.setPreviewOverlay(previewOverlay);
        Common.setLavaGuard(lavaGuard);
        Common.setLavaGuardRadius(lavaGuardRadius);
        Common.setSpectatorHud(spectatorHud);
        Common.setSpectatorGlow(spectatorGlow);
        Common.setHudStats(hudStats);
        // Persist to disk so settings survive a restart.
        ConfigManager.save();
    }

    private void resetConfig() {
        delayTicks = 0;
        reach = 1;
        excavationMode = 0;
        tunnel3x3 = false;
        autoToolSwitch = true;
        fastInstantBreak = true;
        autoBridge = false;
        durabilityThreshold = 0;
        protectEnchanted = true;
        restockTools = true;
        protectBlockEntities = true;
        serverSafe = false;
        previewOverlay = false;
        lavaGuard = false;
        lavaGuardRadius = 4;
        spectatorHud = false;
        spectatorGlow = false;
        hudStats = false;
        // Rebuild so sliders/toggles on the current tab reflect the reset values.
        rebuildWidgets();
    }

    private MutableComponent onOff(String key, boolean value) {
        return Component.translatable(key, Component.translatable(value ? "options.on" : "options.off"));
    }

    private MutableComponent getExcavationModeText() {
        return Component.translatable("screen.assist-excavation.config.excavationMode",
                Component.translatable("screen.assist-excavation.config.excavationMode." + excavationMode));
    }

    private MutableComponent getTunnelSizeText() {
        return Component.translatable("screen.assist-excavation.config.tunnel3x3",
                Component.translatable(tunnel3x3
                        ? "screen.assist-excavation.config.tunnel3x3.wide"
                        : "screen.assist-excavation.config.tunnel3x3.narrow"));
    }

    private MutableComponent getDurabilityText() {
        if (durabilityThreshold <= 0) {
            return Component.translatable("screen.assist-excavation.config.durability",
                    Component.translatable("screen.assist-excavation.config.durability.off"));
        }
        return Component.translatable("screen.assist-excavation.config.durability", durabilityThreshold);
    }

    private MutableComponent getAutoToolText() {
        return onOff("screen.assist-excavation.config.autoTool", autoToolSwitch);
    }

    private MutableComponent getFastBreakText() {
        return onOff("screen.assist-excavation.config.fastBreak", fastInstantBreak);
    }

    private MutableComponent getAutoBridgeText() {
        return onOff("screen.assist-excavation.config.autoBridge", autoBridge);
    }

    private MutableComponent getProtectEnchantedText() {
        return onOff("screen.assist-excavation.config.protectEnchanted", protectEnchanted);
    }

    private MutableComponent getRestockText() {
        return onOff("screen.assist-excavation.config.restockTools", restockTools);
    }

    private MutableComponent getProtectContainersText() {
        return onOff("screen.assist-excavation.config.protectBlockEntities", protectBlockEntities);
    }

    private MutableComponent getServerSafeText() {
        return onOff("screen.assist-excavation.config.serverSafe", serverSafe);
    }

    private MutableComponent getPreviewText() {
        return onOff("screen.assist-excavation.config.preview", previewOverlay);
    }

    private MutableComponent getLavaGuardText() {
        return onOff("screen.assist-excavation.config.lavaGuard", lavaGuard);
    }

    private MutableComponent getLavaRadiusText() {
        return Component.translatable("screen.assist-excavation.config.lavaRadius", lavaGuardRadius);
    }

    private MutableComponent getSpectatorHudText() {
        return onOff("screen.assist-excavation.config.spectatorHud", spectatorHud);
    }

    private MutableComponent getSpectatorGlowText() {
        return onOff("screen.assist-excavation.config.spectatorGlow", spectatorGlow);
    }

    private MutableComponent getStatsHudText() {
        return onOff("screen.assist-excavation.config.hudStats", hudStats);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);

        extractor.centeredText(this.font,
                Component.translatable("screen.assist-excavation.config.title"),
                this.width / 2, 15, 0xFFFFFF);

        if (this.minecraft != null && this.minecraft.player != null) {
            double realReach = this.minecraft.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
            extractor.centeredText(this.font,
                    Component.translatable("screen.assist-excavation.config.real_reach", String.format("%.2f", realReach)),
                    this.width / 2, this.infoY, 0xAAAAAA);
        }
    }

    // Custom slider for delayTicks (0..MAX_DELAY_TICKS)
    private class DelayTicksSlider extends AbstractSliderButton {
        public DelayTicksSlider(int x, int y, int width, int height, Component text, double value) {
            super(x, y, width, height, text, value);
            delayTicks = (int) Math.round(value * Common.MAX_DELAY_TICKS);
        }

        @Override
        protected void updateMessage() {
            delayTicks = (int) Math.round(this.value * Common.MAX_DELAY_TICKS);
            this.setMessage(Component.translatable("screen.assist-excavation.config.delayTicks", delayTicks));
        }

        @Override
        protected void applyValue() {
        }
    }

    // Custom slider for reach (MIN_REACH..MAX_REACH)
    private class ReachSlider extends AbstractSliderButton {
        public ReachSlider(int x, int y, int width, int height, Component text, double value) {
            super(x, y, width, height, text, value);
            reach = (int) Math.round(value * (Common.MAX_REACH - 1)) + 1;
        }

        @Override
        protected void updateMessage() {
            reach = (int) Math.round(this.value * (Common.MAX_REACH - 1)) + 1;
            this.setMessage(Component.translatable("screen.assist-excavation.config.reach", reach));
        }

        @Override
        protected void applyValue() {
        }
    }

    // Custom slider for the durability guard (0..MAX_DURABILITY, 0 = off)
    private class DurabilitySlider extends AbstractSliderButton {
        public DurabilitySlider(int x, int y, int width, int height, Component text, double value) {
            super(x, y, width, height, text, value);
            durabilityThreshold = (int) Math.round(value * Common.MAX_DURABILITY);
        }

        @Override
        protected void updateMessage() {
            durabilityThreshold = (int) Math.round(this.value * Common.MAX_DURABILITY);
            this.setMessage(getDurabilityText());
        }

        @Override
        protected void applyValue() {
        }
    }

    // Custom slider for the lava-guard scan radius (MIN_LAVA_GUARD_RADIUS..MAX_LAVA_GUARD_RADIUS)
    private class LavaRadiusSlider extends AbstractSliderButton {
        public LavaRadiusSlider(int x, int y, int width, int height, Component text, double value) {
            super(x, y, width, height, text, value);
            lavaGuardRadius = (int) Math.round(value
                    * (Common.MAX_LAVA_GUARD_RADIUS - Common.MIN_LAVA_GUARD_RADIUS))
                    + Common.MIN_LAVA_GUARD_RADIUS;
        }

        @Override
        protected void updateMessage() {
            lavaGuardRadius = (int) Math.round(this.value
                    * (Common.MAX_LAVA_GUARD_RADIUS - Common.MIN_LAVA_GUARD_RADIUS))
                    + Common.MIN_LAVA_GUARD_RADIUS;
            this.setMessage(getLavaRadiusText());
        }

        @Override
        protected void applyValue() {
        }
    }
}
