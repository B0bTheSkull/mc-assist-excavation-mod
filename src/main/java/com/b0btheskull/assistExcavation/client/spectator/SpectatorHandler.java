package com.b0btheskull.assistExcavation.client.spectator;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.level.GameType;
import com.b0btheskull.assistExcavation.client.Common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Surfaces players who are in spectator mode.
 *
 * <p>Two independent, purely client-side aids:
 * <ul>
 *   <li><b>HUD list</b> ({@link #spectatorNames}): derived from the tab-list (player-info) packet,
 *       which carries every listed player's game mode even for spectators. This works while you are
 *       playing normally — you learn <i>who</i> is spectating, though not where.</li>
 *   <li><b>Glow</b> ({@link #handleTick}): sets the client-side glowing tag on any spectator player
 *       whose entity your client has actually received, so they render with an outline. On a vanilla
 *       server that only happens when you are a spectator too; otherwise there is simply no entity to
 *       glow and this no-ops. Nothing here is sent to the server.</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public final class SpectatorHandler {
    // UUIDs of player entities we set the glowing tag on, so we can clear exactly those (and not
    // stomp a glow set by something else, e.g. the Glowing effect) when they stop spectating or the
    // feature is turned off.
    private static final Set<UUID> glowed = new HashSet<>();

    private SpectatorHandler() {
    }

    /** Names of players currently in spectator mode (from the tab-list), excluding the local player. */
    public static List<String> spectatorNames(Minecraft client) {
        List<String> names = new ArrayList<>();
        ClientPacketListener conn = client.getConnection();
        if (conn == null) {
            return names;
        }
        UUID self = client.player != null ? client.player.getUUID() : null;
        for (PlayerInfo info : conn.getListedOnlinePlayers()) {
            if (info.getGameMode() != GameType.SPECTATOR) {
                continue;
            }
            if (self != null && info.getProfile().id().equals(self)) {
                continue;
            }
            names.add(info.getProfile().name());
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    /** Per-tick glow upkeep: light spectator players we can see, clear glow we previously set. */
    public static void handleTick(Minecraft client) {
        ClientLevel level = client.level;
        ClientPacketListener conn = client.getConnection();
        if (level == null || conn == null) {
            glowed.clear(); // left the world; entities are gone, drop our bookkeeping
            return;
        }
        if (!Common.isSpectatorGlow()) {
            clearAll(level);
            return;
        }

        UUID self = client.player != null ? client.player.getUUID() : null;
        Set<UUID> current = new HashSet<>();
        for (AbstractClientPlayer p : level.players()) {
            UUID id = p.getUUID();
            if (self != null && id.equals(self)) {
                continue;
            }
            PlayerInfo info = conn.getPlayerInfo(id);
            if (info != null && info.getGameMode() == GameType.SPECTATOR) {
                p.setGlowingTag(true);
                current.add(id);
            }
        }

        // Clear glow on anyone we lit last tick who is no longer a visible spectator.
        for (UUID id : glowed) {
            if (!current.contains(id)) {
                AbstractClientPlayer p = findPlayer(level, id);
                if (p != null) {
                    p.setGlowingTag(false);
                }
            }
        }
        glowed.clear();
        glowed.addAll(current);
    }

    private static void clearAll(ClientLevel level) {
        if (glowed.isEmpty()) {
            return;
        }
        for (UUID id : glowed) {
            AbstractClientPlayer p = findPlayer(level, id);
            if (p != null) {
                p.setGlowingTag(false);
            }
        }
        glowed.clear();
    }

    private static AbstractClientPlayer findPlayer(ClientLevel level, UUID id) {
        for (AbstractClientPlayer p : level.players()) {
            if (p.getUUID().equals(id)) {
                return p;
            }
        }
        return null;
    }
}
