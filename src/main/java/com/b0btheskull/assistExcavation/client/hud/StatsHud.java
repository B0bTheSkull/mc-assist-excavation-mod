package com.b0btheskull.assistExcavation.client.hud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import com.b0btheskull.assistExcavation.client.Common;
import com.b0btheskull.assistExcavation.client.config.HotKey.AssistExcavationKeyBindings;
import com.b0btheskull.assistExcavation.client.excavation.ExcavationHandler;

/**
 * Top-right HUD readout of the mod's live state: enabled/disabled, current mode, held-tool
 * durability, and the session blocks-mined counter. Gated on {@link Common#isHudStats()}.
 * Sits top-right.
 */
@Environment(EnvType.CLIENT)
public final class StatsHud implements HudElement {
    private static final int TITLE_COLOR = 0xFFFFAA00;   // gold
    private static final int ENABLED_COLOR = 0xFF55FF55; // green
    private static final int DISABLED_COLOR = 0xFFFF5555;// red
    private static final int LABEL_COLOR = 0xFFCCCCCC;   // faint grey
    private static final int DUR_OK = 0xFF55FF55;
    private static final int DUR_WARN = 0xFFFFFF55;
    private static final int DUR_LOW = 0xFFFF5555;
    private static final int MARGIN = 4;
    private static final int LINE_HEIGHT = 10;
    private static final int LINES = 5; // title, state, mode, tool, mined

    /** Vertical space (px) the stats block occupies from the top of the screen when visible, else 0. */
    public static int reservedHeight() {
        return Common.isHudStats() ? MARGIN + LINES * LINE_HEIGHT : 0;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, DeltaTracker delta) {
        if (!Common.isHudStats()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) {
            return;
        }
        if (client.getDebugOverlay().showDebugScreen()) {
            return;
        }

        Font font = client.font;
        int right = extractor.guiWidth() - MARGIN;
        int y = MARGIN;

        // Title
        drawRight(extractor, font, Component.translatable("hud.assist-excavation.stats.title"),
                right, y, TITLE_COLOR);
        y += LINE_HEIGHT;

        // State
        boolean on = AssistExcavationKeyBindings.isExcavationEnabled();
        drawRight(extractor, font,
                Component.translatable("hud.assist-excavation.stats.state",
                        Component.translatable(on ? "options.on" : "options.off")),
                right, y, on ? ENABLED_COLOR : DISABLED_COLOR);
        y += LINE_HEIGHT;

        // Mode
        drawRight(extractor, font,
                Component.translatable("hud.assist-excavation.stats.mode",
                        Component.translatable("screen.assist-excavation.config.excavationMode."
                                + Common.getExcavationMode())),
                right, y, LABEL_COLOR);
        y += LINE_HEIGHT;

        // Held tool durability
        ItemStack held = client.player.getMainHandItem();
        if (held.isDamageableItem()) {
            int remaining = held.getMaxDamage() - held.getDamageValue();
            int max = held.getMaxDamage();
            double frac = max > 0 ? (double) remaining / max : 0.0;
            int durColor = frac > 0.5 ? DUR_OK : frac > 0.2 ? DUR_WARN : DUR_LOW;
            drawRight(extractor, font,
                    Component.translatable("hud.assist-excavation.stats.tool", remaining, max),
                    right, y, durColor);
        } else {
            drawRight(extractor, font,
                    Component.translatable("hud.assist-excavation.stats.tool.none"),
                    right, y, LABEL_COLOR);
        }
        y += LINE_HEIGHT;

        // Session blocks mined
        drawRight(extractor, font,
                Component.translatable("hud.assist-excavation.stats.mined",
                        ExcavationHandler.getSessionBlocksMined()),
                right, y, LABEL_COLOR);
    }

    /** Draw a line right-aligned to {@code right} (its right edge lands on x = right). */
    private static void drawRight(GuiGraphicsExtractor extractor, Font font, Component text,
                                  int right, int y, int color) {
        int x = right - font.width(text);
        extractor.text(font, text, x, y, color);
    }
}
