package com.b0btheskull.assistExcavation.client.excavation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Remembers the block positions this mod placed (lava-guard caps, auto-bridge blocks) so the
 * excavator never mines its own placements. Without this, capping a lava source and then mining the
 * fresh cap on the next tick produces an endless place&rarr;mine&rarr;place churn that also burns
 * through the player's blocks.
 *
 * Entries are pruned once they leave the server interaction range: an out-of-reach block can't be
 * mined anyway, so it no longer needs protection, and this bounds the set to roughly the reach
 * volume without needing a tick clock. The whole set is cleared on a world change.
 */
@Environment(EnvType.CLIENT)
public final class PlacedBlockRegistry {
    private static final Set<BlockPos> placed = new HashSet<>();
    private static ClientLevel lastLevel = null;

    private PlacedBlockRegistry() {
    }

    /** Record a position the mod just placed a block at. */
    public static void record(BlockPos pos) {
        placed.add(pos.immutable());
    }

    /** Whether {@code pos} was placed by the mod and so must never be mined. */
    public static boolean isProtected(BlockPos pos) {
        return placed.contains(pos);
    }

    /** Clear everything if the player has changed worlds since the last call. */
    public static void onLevelMaybeChanged(ClientLevel level) {
        if (level != lastLevel) {
            placed.clear();
            lastLevel = level;
        }
    }

    /** Drop entries outside the player's server interaction range (they can't be mined anyway). */
    public static void prune(LocalPlayer player) {
        if (placed.isEmpty()) {
            return;
        }
        double reach = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
        double reachSq = reach * reach;
        double px = player.getX();
        double py = player.getY() + player.getEyeHeight();
        double pz = player.getZ();
        Iterator<BlockPos> it = placed.iterator();
        while (it.hasNext()) {
            BlockPos pos = it.next();
            double dx = pos.getX() + 0.5 - px;
            double dy = pos.getY() + 0.5 - py;
            double dz = pos.getZ() + 0.5 - pz;
            if (dx * dx + dy * dy + dz * dz > reachSq) {
                it.remove();
            }
        }
    }
}
