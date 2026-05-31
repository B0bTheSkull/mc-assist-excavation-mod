package top.zspaces.assistExcavation.client.config.GUI;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.ai.attributes.Attributes;
import top.zspaces.assistExcavation.client.Common;

@Environment(EnvType.CLIENT)
public class AssistExcavationConfigScreen extends Screen {
    private final Screen parent;

    // Config values
    private int delayTicks;
    private int reach;
    private int excavationMode; // 0=rectangle, 1=circle, 2=not implemented
    private boolean autoToolSwitch;
    private boolean fastInstantBreak;
    private boolean autoBridge;

    // UI widgets
    private DelayTicksSlider delayTicksSlider;
    private ReachSlider reachSlider;
    private Button excavationModeButton;
    private Button autoToolButton;
    private Button fastBreakButton;
    private Button autoBridgeButton;

    // Y of the "server real reach" info line (computed in init() with the layout, used when rendering).
    private int infoY;

    public AssistExcavationConfigScreen(Screen parent) {
        super(Component.translatable("screen.assist-excavation.config.title"));
        this.parent = parent;
        // Load current values from Common
        this.delayTicks = Common.getDelayTicks();
        this.reach = Common.getReach();
        this.excavationMode = Common.getExcavationMode();
        this.autoToolSwitch = Common.isAutoToolSwitch();
        this.fastInstantBreak = Common.isFastInstantBreak();
        this.autoBridge = Common.isAutoBridge();
    }

    @Override
    protected void init() {
        super.init();

        // Compact top-to-bottom flow layout, with Reset/Done as the final row directly under
        // the controls. They are no longer anchored to the screen bottom — that anchoring let
        // them overlap the lower toggles at GUI scale 4 / small windows, which swallowed the
        // click on "Done". Total height is ~200px, so it fits without scrolling at common scales.
        final int w = 200;
        final int h = 20;
        final int pitch = 24;
        int cx = this.width / 2;
        int x = cx - w / 2;
        int y = 36;

        delayTicksSlider = new DelayTicksSlider(x, y, w, h,
                Component.translatable("screen.assist-excavation.config.delayTicks", delayTicks),
                (double) delayTicks / 40.0);
        y += pitch;

        reachSlider = new ReachSlider(x, y, w, h,
                Component.translatable("screen.assist-excavation.config.reach", reach),
                (double) (reach - 1) / 5.0);
        y += pitch;

        excavationModeButton = Button.builder(getExcavationModeText(), button -> {
            excavationMode = (excavationMode + 1) % 3;
            button.setMessage(getExcavationModeText());
        }).bounds(x, y, w, h).build();
        y += pitch;

        autoToolButton = Button.builder(getAutoToolText(), button -> {
            autoToolSwitch = !autoToolSwitch;
            button.setMessage(getAutoToolText());
        }).bounds(x, y, w, h).build();
        y += pitch;

        fastBreakButton = Button.builder(getFastBreakText(), button -> {
            fastInstantBreak = !fastInstantBreak;
            button.setMessage(getFastBreakText());
        }).bounds(x, y, w, h).build();
        y += pitch;

        autoBridgeButton = Button.builder(getAutoBridgeText(), button -> {
            autoBridge = !autoBridge;
            button.setMessage(getAutoBridgeText());
        }).bounds(x, y, w, h).build();
        y += pitch;

        // The real-reach info line sits here; the Reset/Done row goes below it.
        this.infoY = y + 2;
        y += 14;

        int half = w / 2 - 2;
        Button resetButton = Button.builder(
                Component.translatable("screen.assist-excavation.config.reset"),
                button -> resetConfig())
                .bounds(x, y, half, h).build();
        Button doneButton = Button.builder(
                Component.translatable("gui.done"),
                button -> {
                    saveConfig();
                    this.onClose();
                })
                .bounds(cx + 2, y, half, h).build();

        // Add widgets to the screen
        addRenderableWidget(delayTicksSlider);
        addRenderableWidget(reachSlider);
        addRenderableWidget(excavationModeButton);
        addRenderableWidget(autoToolButton);
        addRenderableWidget(fastBreakButton);
        addRenderableWidget(autoBridgeButton);
        addRenderableWidget(resetButton);
        addRenderableWidget(doneButton);
    }

    private void saveConfig() {
        Common.setDelayTicks(delayTicks);
        Common.setReach(reach);
        Common.setExcavationMode(excavationMode);
        Common.setAutoToolSwitch(autoToolSwitch);
        Common.setFastInstantBreak(fastInstantBreak);
        Common.setAutoBridge(autoBridge);
    }

    private void resetConfig() {
        delayTicks = 0;
        reach = 1;
        excavationMode = 0;
        autoToolSwitch = true;
        fastInstantBreak = true;
        autoBridge = false;

        // Recreate the sliders so their displayed position resets too.
        // Coordinates must match the layout in init() (delay row, then reach row).
        removeWidget(delayTicksSlider);
        removeWidget(reachSlider);

        final int w = 200;
        final int h = 20;
        int cx = this.width / 2;
        int x = cx - w / 2;

        delayTicksSlider = new DelayTicksSlider(x, 36, w, h,
                Component.translatable("screen.assist-excavation.config.delayTicks", delayTicks),
                0.0);

        reachSlider = new ReachSlider(x, 60, w, h,
                Component.translatable("screen.assist-excavation.config.reach", reach),
                0.0);

        addRenderableWidget(delayTicksSlider);
        addRenderableWidget(reachSlider);
        excavationModeButton.setMessage(getExcavationModeText());
        autoToolButton.setMessage(getAutoToolText());
        fastBreakButton.setMessage(getFastBreakText());
        autoBridgeButton.setMessage(getAutoBridgeText());
    }

    private MutableComponent getExcavationModeText() {
        return Component.translatable("screen.assist-excavation.config.excavationMode",
                Component.translatable("screen.assist-excavation.config.excavationMode." + excavationMode));
    }

    private MutableComponent getAutoToolText() {
        return Component.translatable("screen.assist-excavation.config.autoTool",
                Component.translatable(autoToolSwitch ? "options.on" : "options.off"));
    }

    private MutableComponent getFastBreakText() {
        return Component.translatable("screen.assist-excavation.config.fastBreak",
                Component.translatable(fastInstantBreak ? "options.on" : "options.off"));
    }

    private MutableComponent getAutoBridgeText() {
        return Component.translatable("screen.assist-excavation.config.autoBridge",
                Component.translatable(autoBridge ? "options.on" : "options.off"));
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

        // Title
        extractor.centeredText(this.font,
                Component.translatable("screen.assist-excavation.config.title"),
                this.width / 2, 15, 0xFFFFFF);

        // Server's actual reach distance (informational), placed in the layout flow.
        if (this.minecraft != null && this.minecraft.player != null) {
            double realReach = this.minecraft.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
            extractor.centeredText(this.font,
                    Component.translatable("screen.assist-excavation.config.real_reach", String.format("%.2f", realReach)),
                    this.width / 2, this.infoY, 0xAAAAAA);
        }
    }

    // Custom slider for delayTicks
    private class DelayTicksSlider extends AbstractSliderButton {
        public DelayTicksSlider(int x, int y, int width, int height, Component text, double value) {
            super(x, y, width, height, text, value);
            delayTicks = (int) (value * 40);
        }

        @Override
        protected void updateMessage() {
            delayTicks = (int) (this.value * 40);
            this.setMessage(Component.translatable("screen.assist-excavation.config.delayTicks", delayTicks));
        }

        @Override
        protected void applyValue() {
            // value is read in updateMessage(); nothing extra to apply
        }
    }

    // Custom slider for reach
    private class ReachSlider extends AbstractSliderButton {
        public ReachSlider(int x, int y, int width, int height, Component text, double value) {
            super(x, y, width, height, text, value);
            reach = (int) (value * 5) + 1;
        }

        @Override
        protected void updateMessage() {
            reach = (int) (this.value * 5) + 1;
            this.setMessage(Component.translatable("screen.assist-excavation.config.reach", reach));
        }

        @Override
        protected void applyValue() {
            // value is read in updateMessage(); nothing extra to apply
        }
    }
}
