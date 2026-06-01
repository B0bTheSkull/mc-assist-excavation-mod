package com.b0btheskull.assistExcavation.client.config.HotKey;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.b0btheskull.assistExcavation.client.config.GUI.AssistExcavationConfigScreen;

@Environment(EnvType.CLIENT)
public class AssistExcavationKeyBindings {
    public static final Logger LOGGER = LoggerFactory.getLogger("assist-excavation");

    // Excavation on/off key. Defaults to J (Z collided with Xaero "enlarge map").
    public static KeyMapping toggleExcavationKey;

    // Open-config key. Defaults to unbound (V collided with drop-item and voice chat);
    // the config is also reachable through Mod Menu.
    public static KeyMapping openConfigKey;

    // Whether assist excavation is currently enabled.
    private static boolean excavationEnabled = false;

    // Tracks last tick's key state to detect the press edge.
    private static boolean wasToggleKeyPressed = false;

    public static void registerKeyBindings() {
        // Excavation toggle (default J)
        toggleExcavationKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.assist-excavation.toggle",
                GLFW.GLFW_KEY_J,
                KeyMapping.Category.MISC
        ));

        // Open-config (default unbound; bind it yourself in Controls if you want a hotkey)
        openConfigKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.assist-excavation.open_config",
                GLFW.GLFW_KEY_UNKNOWN,
                KeyMapping.Category.MISC
        ));

        // Per-tick key handling
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Toggle key: flip once per press (edge-detected)
            boolean isToggleKeyPressed = toggleExcavationKey.isDown();
            if (isToggleKeyPressed && !wasToggleKeyPressed) {
                excavationEnabled = !excavationEnabled;
                if (client.player != null) {
                    client.player.sendOverlayMessage(
                        Component.translatable(
                            "message.assist-excavation.toggle",
                            Component.translatable(excavationEnabled ? "message.assist-excavation.enabled" : "message.assist-excavation.disabled")
                        )
                    );
                }
                LOGGER.info("Excavation toggled: {}", excavationEnabled ? "enabled" : "disabled");
            }
            wasToggleKeyPressed = isToggleKeyPressed;

            // Open-config key
            while (openConfigKey.consumeClick()) {
                if (client.player != null) {
                    client.setScreen(new AssistExcavationConfigScreen(client.screen));
                    LOGGER.info("Config screen opened");
                }
            }
        });

        LOGGER.info("Key bindings registered: toggle (J), config (unbound)");
    }

    /**
     * @return true if assist excavation is currently enabled.
     */
    public static boolean isExcavationEnabled() {
        return excavationEnabled;
    }
}
