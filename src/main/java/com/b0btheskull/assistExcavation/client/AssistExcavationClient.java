package com.b0btheskull.assistExcavation.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.b0btheskull.assistExcavation.client.bridge.BridgeHandler;
import com.b0btheskull.assistExcavation.client.bridge.BridgeKeyBindings;
import com.b0btheskull.assistExcavation.client.config.HotKey.AssistExcavationKeyBindings;
import com.b0btheskull.assistExcavation.client.config.ConfigManager;
import com.b0btheskull.assistExcavation.client.excavation.ExcavationHandler;
import com.b0btheskull.assistExcavation.client.excavation.PreviewOverlay;

public class AssistExcavationClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("assist-excavation");

    @Override
    public void onInitializeClient() {
        // Load persisted settings before anything reads Common.
        ConfigManager.load();

        // Register key bindings.
        AssistExcavationKeyBindings.registerKeyBindings();
        BridgeKeyBindings.registerKeyBindings();

        // Register client-tick handlers. Bridging runs after mining so the bridge's temporary
        // slot switch/restore doesn't interfere with mining's tool-switch.
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        ClientTickEvents.END_CLIENT_TICK.register(client -> BridgeHandler.handleBridge());

        // Register the mining preview overlay (world render).
        PreviewOverlay.register();

        LOGGER.info("Assist Excavation mod initialized - Enhancing your mining experience while respecting server rules");
    }

    /**
     * Client tick event: drives the excavation logic.
     */
    private void onClientTick(Minecraft client) {
        ExcavationHandler.handleExcavation();
    }
}
