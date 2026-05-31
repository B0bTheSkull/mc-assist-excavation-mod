package top.zspaces.assistExcavation.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.zspaces.assistExcavation.client.bridge.BridgeHandler;
import top.zspaces.assistExcavation.client.bridge.BridgeKeyBindings;
import top.zspaces.assistExcavation.client.config.HotKey.AssistExcavationKeyBindings;
import top.zspaces.assistExcavation.client.excavation.ExcavationHandler;

public class AssistExcavationClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("assist-excavation");

    @Override
    public void onInitializeClient() {
        // Register key bindings.
        AssistExcavationKeyBindings.registerKeyBindings();
        BridgeKeyBindings.registerKeyBindings();

        // Register client-tick handlers. Bridging runs after mining so the bridge's temporary
        // slot switch/restore doesn't interfere with mining's tool-switch.
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        ClientTickEvents.END_CLIENT_TICK.register(client -> BridgeHandler.handleBridge());

        LOGGER.info("Assist Excavation mod initialized - Enhancing your mining experience while respecting server rules");
    }

    /**
     * Client tick event: drives the excavation logic.
     */
    private void onClientTick(Minecraft client) {
        ExcavationHandler.handleExcavation();
    }
}
