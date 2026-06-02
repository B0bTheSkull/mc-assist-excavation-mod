package com.b0btheskull.assistExcavation.client.config.HotKey;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.b0btheskull.assistExcavation.client.Common;
import com.b0btheskull.assistExcavation.client.config.GUI.AssistExcavationConfigScreen;

@Environment(EnvType.CLIENT)
public class AssistExcavationKeyBindings {
    public static final Logger LOGGER = LoggerFactory.getLogger("assist-excavation");

    // Shared key category so all of this mod's binds group together in the Controls screen
    // (instead of being scattered through MISC). Label comes from key.category.assist-excavation.main.
    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("assist-excavation", "main"));

    // Excavation on/off key. Defaults to J (Z collided with Xaero "enlarge map").
    public static KeyMapping toggleExcavationKey;

    // Open-config key. Defaults to unbound (V collided with drop-item and voice chat);
    // the config is also reachable through Mod Menu.
    public static KeyMapping openConfigKey;

    // Lava Guard on/off key. Defaults to unbound; shares the Common.lavaGuard flag with the GUI.
    public static KeyMapping toggleLavaGuardKey;

    // Whether assist excavation is currently enabled.
    private static boolean excavationEnabled = false;

    // Tracks last tick's key state to detect the press edge.
    private static boolean wasToggleKeyPressed = false;

    public static void registerKeyBindings() {
        // Excavation toggle (default J)
        toggleExcavationKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.assist-excavation.toggle",
                GLFW.GLFW_KEY_J,
                CATEGORY
        ));

        // Open-config (default unbound; bind it yourself in Controls if you want a hotkey)
        openConfigKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.assist-excavation.open_config",
                GLFW.GLFW_KEY_UNKNOWN,
                CATEGORY
        ));

        // Lava Guard toggle (default unbound)
        toggleLavaGuardKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.assist-excavation.lavaguard",
                GLFW.GLFW_KEY_UNKNOWN,
                CATEGORY
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

            // Lava Guard toggle key: flips once per press, shares Common.lavaGuard with the GUI.
            while (toggleLavaGuardKey.consumeClick()) {
                boolean now = !Common.isLavaGuard();
                Common.setLavaGuard(now);
                if (client.player != null) {
                    client.player.sendOverlayMessage(
                        Component.translatable(
                            "message.assist-excavation.lavaguard.toggle",
                            Component.translatable(now ? "message.assist-excavation.enabled" : "message.assist-excavation.disabled")
                        )
                    );
                }
                LOGGER.info("Lava Guard toggled: {}", now ? "enabled" : "disabled");
            }
        });

        LOGGER.info("Key bindings registered: toggle (J), config (unbound), lava guard (unbound)");
    }

    /**
     * @return true if assist excavation is currently enabled.
     */
    public static boolean isExcavationEnabled() {
        return excavationEnabled;
    }
}
