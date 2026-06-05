package com.b0btheskull.assistExcavation.client.spectator;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import com.b0btheskull.assistExcavation.client.Common;
import com.b0btheskull.assistExcavation.client.hud.StatsHud;

import java.util.List;

/**
 * Top-right HUD list of who is currently in spectator mode. Gated on {@link Common#isSpectatorHud()};
 * the data comes from {@link SpectatorHandler#spectatorNames} (the tab-list), so it works even while
 * you are playing normally. Renders below the stats HUD when that is also showing.
 */
@Environment(EnvType.CLIENT)
public final class SpectatorHud implements HudElement {
    private static final int HEADER_COLOR = 0xFFFFAA00; // gold
    private static final int NAME_COLOR = 0xFFCCCCCC;   // faint grey
    private static final int MARGIN = 4;
    private static final int LINE_HEIGHT = 10;

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, DeltaTracker delta) {
        if (!Common.isSpectatorHud()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) {
            return;
        }
        // Don't fight the F3 debug overlay for the corner.
        if (client.getDebugOverlay().showDebugScreen()) {
            return;
        }

        List<String> names = SpectatorHandler.spectatorNames(client);
        if (names.isEmpty()) {
            return;
        }

        Font font = client.font;
        int right = extractor.guiWidth() - MARGIN;
        // Stack below the stats HUD when it is visible; otherwise hug the top.
        int reserved = StatsHud.reservedHeight();
        int y = reserved > 0 ? reserved + MARGIN : MARGIN;

        Component header = Component.translatable("hud.assist-excavation.spectators", names.size());
        extractor.text(font, header, right - font.width(header), y, HEADER_COLOR);
        y += LINE_HEIGHT;
        for (String name : names) {
            Component line = Component.literal("• " + name);
            extractor.text(font, line, right - font.width(line), y, NAME_COLOR);
            y += LINE_HEIGHT;
        }
    }
}
