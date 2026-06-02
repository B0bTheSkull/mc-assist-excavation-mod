package com.b0btheskull.assistExcavation.client.bridge;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.b0btheskull.assistExcavation.client.Common;
import com.b0btheskull.assistExcavation.client.config.HotKey.AssistExcavationKeyBindings;

@Environment(EnvType.CLIENT)
public class BridgeKeyBindings {
    public static final Logger LOGGER = LoggerFactory.getLogger("assist-excavation");

    // Auto-bridge on/off key. Defaults to K (B collided with Xaero waypoint and BreedTimer).
    public static KeyMapping toggleBridgeKey;

    public static void registerKeyBindings() {
        toggleBridgeKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.assist-excavation.bridge",
                GLFW.GLFW_KEY_K,
                AssistExcavationKeyBindings.CATEGORY
        ));

        // Toggle key: flips once per press. The GUI button shares the same Common.autoBridge flag.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleBridgeKey.consumeClick()) {
                boolean now = !Common.isAutoBridge();
                Common.setAutoBridge(now);
                if (client.player != null) {
                    client.player.sendOverlayMessage(
                            Component.translatable(
                                    "message.assist-excavation.bridge.toggle",
                                    Component.translatable(now ? "message.assist-excavation.enabled" : "message.assist-excavation.disabled")
                            )
                    );
                }
                LOGGER.info("Auto bridge toggled: {}", now ? "enabled" : "disabled");
            }
        });

        LOGGER.info("Bridge key binding registered: toggle (K)");
    }
}
