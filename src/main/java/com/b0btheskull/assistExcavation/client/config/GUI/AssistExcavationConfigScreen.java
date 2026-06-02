package com.b0btheskull.assistExcavation.client.config.GUI;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.b0btheskull.assistExcavation.client.Common;
import com.b0btheskull.assistExcavation.client.config.ConfigManager;

@Environment(EnvType.CLIENT)
public class AssistExcavationConfigScreen extends Screen {
    private final Screen parent;

    // Config values
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

    // UI widgets that need rebuilding on reset.
    private DelayTicksSlider delayTicksSlider;
    private ReachSlider reachSlider;
    private DurabilitySlider durabilitySlider;
    private LavaRadiusSlider lavaRadiusSlider;
    private Button excavationModeButton;
    private Button tunnelSizeButton;
    private Button autoToolButton;
    private Button fastBreakButton;
    private Button autoBridgeButton;
    private Button protectEnchantedButton;
    private Button restockToolsButton;
    private Button protectContainersButton;
    private Button serverSafeButton;
    private Button previewButton;
    private Button lavaGuardButton;

    // Cached layout coordinates (computed in init(), reused by resetConfig()).
    private int colW;
    private int leftX;
    private int rightX;
    private int delayY;
    private int reachY;
    private int durY;
    private int lavaRadiusY;
    private int infoY;

    public AssistExcavationConfigScreen(Screen parent) {
        super(Component.translatable("screen.assist-excavation.config.title"));
        this.parent = parent;
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
    }

    @Override
    protected void init() {
        super.init();

        // Two-column flow so all controls fit without scrolling. Reset/Done sit centred below.
        final int h = 20;
        final int pitch = 24;
        this.colW = 150;
        int cx = this.width / 2;
        this.leftX = cx - colW - 4;
        this.rightX = cx + 4;
        int top = 36;

        // ---- Left column ----
        int ly = top;
        this.delayY = ly;
        delayTicksSlider = new DelayTicksSlider(leftX, ly, colW, h,
                Component.translatable("screen.assist-excavation.config.delayTicks", delayTicks),
                (double) delayTicks / Common.MAX_DELAY_TICKS);
        ly += pitch;

        this.reachY = ly;
        reachSlider = new ReachSlider(leftX, ly, colW, h,
                Component.translatable("screen.assist-excavation.config.reach", reach),
                (double) (reach - 1) / (Common.MAX_REACH - 1));
        ly += pitch;

        this.durY = ly;
        durabilitySlider = new DurabilitySlider(leftX, ly, colW, h,
                getDurabilityText(),
                (double) durabilityThreshold / Common.MAX_DURABILITY);
        ly += pitch;

        this.lavaRadiusY = ly;
        lavaRadiusSlider = new LavaRadiusSlider(leftX, ly, colW, h,
                getLavaRadiusText(),
                (double) (lavaGuardRadius - Common.MIN_LAVA_GUARD_RADIUS)
                        / (Common.MAX_LAVA_GUARD_RADIUS - Common.MIN_LAVA_GUARD_RADIUS));
        ly += pitch;

        excavationModeButton = Button.builder(getExcavationModeText(), button -> {
            excavationMode = (excavationMode + 1) % (Common.MAX_EXCAVATION_MODE + 1);
            button.setMessage(getExcavationModeText());
        }).bounds(leftX, ly, colW, h).build();
        ly += pitch;

        tunnelSizeButton = Button.builder(getTunnelSizeText(), button -> {
            tunnel3x3 = !tunnel3x3;
            button.setMessage(getTunnelSizeText());
        }).bounds(leftX, ly, colW, h).build();
        ly += pitch;

        previewButton = Button.builder(getPreviewText(), button -> {
            previewOverlay = !previewOverlay;
            button.setMessage(getPreviewText());
        }).bounds(leftX, ly, colW, h).build();
        ly += pitch;

        // ---- Right column ----
        int ry = top;
        autoToolButton = Button.builder(getAutoToolText(), button -> {
            autoToolSwitch = !autoToolSwitch;
            button.setMessage(getAutoToolText());
        }).bounds(rightX, ry, colW, h).build();
        ry += pitch;

        fastBreakButton = Button.builder(getFastBreakText(), button -> {
            fastInstantBreak = !fastInstantBreak;
            button.setMessage(getFastBreakText());
        }).bounds(rightX, ry, colW, h).build();
        ry += pitch;

        autoBridgeButton = Button.builder(getAutoBridgeText(), button -> {
            autoBridge = !autoBridge;
            button.setMessage(getAutoBridgeText());
        }).bounds(rightX, ry, colW, h).build();
        ry += pitch;

        protectEnchantedButton = Button.builder(getProtectEnchantedText(), button -> {
            protectEnchanted = !protectEnchanted;
            button.setMessage(getProtectEnchantedText());
        }).bounds(rightX, ry, colW, h).build();
        ry += pitch;

        restockToolsButton = Button.builder(getRestockText(), button -> {
            restockTools = !restockTools;
            button.setMessage(getRestockText());
        }).bounds(rightX, ry, colW, h).build();
        ry += pitch;

        protectContainersButton = Button.builder(getProtectContainersText(), button -> {
            protectBlockEntities = !protectBlockEntities;
            button.setMessage(getProtectContainersText());
        }).bounds(rightX, ry, colW, h).build();
        ry += pitch;

        serverSafeButton = Button.builder(getServerSafeText(), button -> {
            serverSafe = !serverSafe;
            button.setMessage(getServerSafeText());
        }).bounds(rightX, ry, colW, h).build();
        ry += pitch;

        lavaGuardButton = Button.builder(getLavaGuardText(), button -> {
            lavaGuard = !lavaGuard;
            button.setMessage(getLavaGuardText());
        }).bounds(rightX, ry, colW, h).build();
        ry += pitch;

        // Info line + Reset/Done row beneath the taller of the two columns.
        int bottom = Math.max(ly, ry);
        this.infoY = bottom + 2;
        int rowY = bottom + 14;

        int fullW = colW * 2 + 8;
        int half = fullW / 2 - 2;
        Button resetButton = Button.builder(
                Component.translatable("screen.assist-excavation.config.reset"),
                button -> resetConfig())
                .bounds(leftX, rowY, half, h).build();
        Button doneButton = Button.builder(
                Component.translatable("gui.done"),
                button -> {
                    saveConfig();
                    this.onClose();
                })
                .bounds(cx + 2, rowY, half, h).build();

        addRenderableWidget(delayTicksSlider);
        addRenderableWidget(reachSlider);
        addRenderableWidget(durabilitySlider);
        addRenderableWidget(lavaRadiusSlider);
        addRenderableWidget(excavationModeButton);
        addRenderableWidget(tunnelSizeButton);
        addRenderableWidget(previewButton);
        addRenderableWidget(autoToolButton);
        addRenderableWidget(fastBreakButton);
        addRenderableWidget(autoBridgeButton);
        addRenderableWidget(protectEnchantedButton);
        addRenderableWidget(restockToolsButton);
        addRenderableWidget(protectContainersButton);
        addRenderableWidget(serverSafeButton);
        addRenderableWidget(lavaGuardButton);
        addRenderableWidget(resetButton);
        addRenderableWidget(doneButton);
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

        // Rebuild the sliders so their handle positions reset too.
        removeWidget(delayTicksSlider);
        removeWidget(reachSlider);
        removeWidget(durabilitySlider);
        removeWidget(lavaRadiusSlider);

        delayTicksSlider = new DelayTicksSlider(leftX, delayY, colW, 20,
                Component.translatable("screen.assist-excavation.config.delayTicks", delayTicks), 0.0);
        reachSlider = new ReachSlider(leftX, reachY, colW, 20,
                Component.translatable("screen.assist-excavation.config.reach", reach), 0.0);
        durabilitySlider = new DurabilitySlider(leftX, durY, colW, 20, getDurabilityText(), 0.0);
        lavaRadiusSlider = new LavaRadiusSlider(leftX, lavaRadiusY, colW, 20, getLavaRadiusText(),
                (double) (lavaGuardRadius - Common.MIN_LAVA_GUARD_RADIUS)
                        / (Common.MAX_LAVA_GUARD_RADIUS - Common.MIN_LAVA_GUARD_RADIUS));

        addRenderableWidget(delayTicksSlider);
        addRenderableWidget(reachSlider);
        addRenderableWidget(durabilitySlider);
        addRenderableWidget(lavaRadiusSlider);

        excavationModeButton.setMessage(getExcavationModeText());
        tunnelSizeButton.setMessage(getTunnelSizeText());
        previewButton.setMessage(getPreviewText());
        autoToolButton.setMessage(getAutoToolText());
        fastBreakButton.setMessage(getFastBreakText());
        autoBridgeButton.setMessage(getAutoBridgeText());
        protectEnchantedButton.setMessage(getProtectEnchantedText());
        restockToolsButton.setMessage(getRestockText());
        protectContainersButton.setMessage(getProtectContainersText());
        serverSafeButton.setMessage(getServerSafeText());
        lavaGuardButton.setMessage(getLavaGuardText());
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
