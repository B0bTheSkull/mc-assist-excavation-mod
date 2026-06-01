package com.b0btheskull.assistExcavation.client.excavation;

import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;
import com.b0btheskull.assistExcavation.client.Common;
import com.b0btheskull.assistExcavation.client.config.HotKey.AssistExcavationKeyBindings;

import java.util.*;

@Environment(EnvType.CLIENT)
public class ExcavationHandler {
    private static final Minecraft client = Minecraft.getInstance();

    // tryMine result: nothing mined / started a multi-tick break / broke in one hit (instant)
    private static final int MINE_NONE = 0;
    private static final int MINE_PROGRESS = 1;
    private static final int MINE_INSTANT = 2;

    // Max instant breaks per tick. Instant blocks (grass, dirt, sand, ...) go through
    // startDestroyBlock, which sets no destroyDelay in survival, so several can be broken
    // in one tick. The cap keeps us from flooding the server with break packets at once
    // (strict anti-cheat may flag that).
    private static final int MAX_INSTANT_PER_TICK = 16;

    // The block currently being mined, and the inter-block delay counter.
    private static BlockPos currentMiningPos = null;
    private static int delayCounter = 0;

    // The hotbar slot held before auto tool-switch began; restored when excavation is
    // turned off. -1 means there is nothing to restore.
    private static int originalSlot = -1;

    // BFS neighbour directions
    private static final Direction[] DIRECTIONS = {
            Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };

    public static void handleExcavation() {
        // If assist mining is off, reset all state and bail.
        if (!AssistExcavationKeyBindings.isExcavationEnabled()) {
            resetState();
            return;
        }

        // Make sure we have a usable client/world/player.
        if (client.player == null || client.level == null || client.gameMode == null) {
            return;
        }
        LocalPlayer player = client.player;
        MultiPlayerGameMode im = client.gameMode;

        // Read config.
        int delayTicks = Common.getDelayTicks();
        int reach = Common.getReach();
        int mode = Common.getExcavationMode(); // 0=rect, 1=sphere

        // Continue the block we're already mining.
        if (currentMiningPos != null) {
            // If it left range, drop it.
            if (!isWithinReach(currentMiningPos, player, reach, mode) || !isWithinServerReach(currentMiningPos, player)) {
                currentMiningPos = null;
            } else {
                // Still in range: keep advancing the break progress.
                BlockState state = client.level.getBlockState(currentMiningPos);
                if (state.isAir()) {
                    // Finished.
                    currentMiningPos = null;
                    if (delayTicks > 0) {
                        delayCounter = delayTicks;
                        return;
                    }
                    // delay 0: fall through and start the next block this same tick (optimization #1)
                } else {
                    // NOTE: never switch tools mid-break. continueDestroyBlock's sameDestroyTarget
                    // requires the held item to match the one we started with; switching resets
                    // progress to zero.
                    im.continueDestroyBlock(currentMiningPos, Direction.UP);
                    player.swing(InteractionHand.MAIN_HAND);
                    return;
                }
            }
        }

        // Inter-block delay.
        if (delayCounter > 0) {
            delayCounter--;
            return;
        }

        // Start a fresh BFS round.
        performBfsMining(player, im, reach, mode);
    }

    private static void performBfsMining(LocalPlayer player,
                                         MultiPlayerGameMode im,
                                         int reach, int mode) {
        BlockPos origin = player.blockPosition();

        // Local BFS structures
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>(); // ArrayDeque instead of LinkedList

        // Seed: the five neighbour directions.
        for (Direction d : DIRECTIONS) {
            BlockPos nb = origin.relative(d);
            if (isWithinReach(nb, player, reach, mode) && isWithinServerReach(nb, player)) {
                visited.add(nb);
                queue.add(nb);
            }
        }

        // Per-tick instant-break cap: with "fast instant-break" on, clear several (optimization #2);
        // off, fall back to one per tick.
        int instantCap = Common.isFastInstantBreak() ? MAX_INSTANT_PER_TICK : 1;

        // BFS: nearest first.
        int instantBroken = 0;
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();

            // Try to mine it.
            int result = tryMine(pos, player, im);
            if (result == MINE_PROGRESS) {
                // A hard block that takes multiple ticks: start it, stop for this tick;
                // handleExcavation() keeps advancing its progress on later ticks.
                player.swing(InteractionHand.MAIN_HAND);
                return;
            }
            if (result == MINE_INSTANT) {
                // Instant break: keep clearing more instant blocks this tick (optimization #2)
                // up to the cap.
                instantBroken++;
                if (instantBroken >= instantCap) {
                    player.swing(InteractionHand.MAIN_HAND);
                    return;
                }
            }

            // Expand to the next ring.
            for (Direction d : DIRECTIONS) {
                BlockPos next = pos.relative(d);
                if (visited.contains(next)) continue;
                if (!isWithinReach(next, player, reach, mode)) continue;
                if (!isWithinServerReach(next, player)) continue; // server-reach check
                visited.add(next);
                queue.add(next);
            }
        }

        // If we broke at least one instant block this tick, swing once
        // (rather than sending a swing packet per block).
        if (instantBroken > 0) {
            player.swing(InteractionHand.MAIN_HAND);
        }
    }

    /**
     * Core mining attempt. Returns MINE_NONE / MINE_PROGRESS / MINE_INSTANT.
     */
    private static int tryMine(BlockPos pos,
                               LocalPlayer player,
                               MultiPlayerGameMode im) {
        // Re-check server reach.
        if (!isWithinServerReach(pos, player)) {
            return MINE_NONE; // outside the server-allowed range
        }

        BlockState state = Objects.requireNonNull(client.level).getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(client.level, pos) < 0) {
            return MINE_NONE; // air, or unbreakable (e.g. bedrock, hardness -1)
        }

        // Optimization #3: switch to the fastest tool for this block before starting the break
        // (only at the start — never mid-break).
        maybeSwitchTool(player, state);

        boolean started = im.startDestroyBlock(pos, Direction.UP);
        if (!started) {
            return MINE_NONE;
        }

        // startDestroyBlock breaks "one-hit" blocks immediately. If it's now air, it was instant.
        if (client.level.getBlockState(pos).isAir()) {
            return MINE_INSTANT;
        }

        // Otherwise it's a hard block whose progress accrues over multiple ticks.
        currentMiningPos = pos;
        return MINE_PROGRESS;
    }

    /**
     * Optimization #3: find the hotbar tool with the highest destroy speed for the target block
     * and switch to it. Only when Common.autoToolSwitch is on, only when a strictly better tool
     * exists, and we send the set-carried-item packet so the server uses the right tool when
     * computing break time.
     */
    private static void maybeSwitchTool(LocalPlayer player, BlockState state) {
        if (!Common.isAutoToolSwitch()) {
            return;
        }
        Inventory inv = player.getInventory();
        int current = inv.getSelectedSlot();
        float bestSpeed = inv.getItem(current).getDestroySpeed(state);
        int best = current;
        for (int slot = 0; slot < Inventory.SELECTION_SIZE; slot++) {
            float speed = inv.getItem(slot).getDestroySpeed(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                best = slot;
            }
        }
        if (best != current) {
            if (originalSlot < 0) {
                originalSlot = current; // remember the pre-switch slot so we can restore it later
            }
            inv.setSelectedSlot(best);
            player.connection.send(new ServerboundSetCarriedItemPacket(best));
        }
    }

    /**
     * Restore the held hotbar slot to what it was before auto tool-switching, when assist
     * mining is turned off.
     */
    private static void restoreTool() {
        if (originalSlot >= 0) {
            LocalPlayer player = client.player;
            if (player != null) {
                player.getInventory().setSelectedSlot(originalSlot);
                player.connection.send(new ServerboundSetCarriedItemPacket(originalSlot));
            }
            originalSlot = -1;
        }
    }

    /**
     * Unified range check.
     */
    private static boolean isWithinReach(BlockPos target,
                                         LocalPlayer player,
                                         int reach,
                                         int mode) {
        if (mode == 0) {
            return withinRect(target, player, reach);
        } else {
            return withinSphere(target, player, reach);
        }
    }

    /**
     * Whether the block is within the server-allowed mining range.
     */
    private static boolean isWithinServerReach(BlockPos target, LocalPlayer player) {
        // The server-allowed real interaction distance.
        double realReach = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
        double dx = target.getX() + 0.5 - player.getX();
        double dy = target.getY() + 0.5 - (player.getY() + player.getEyeHeight());
        double dz = target.getZ() + 0.5 - player.getZ();
        double distanceSq = dx * dx + dy * dy + dz * dz;
        return distanceSq <= realReach * realReach;
    }

    /**
     * Rectangular (Chebyshev-distance) range: at/above foot level, horizontal <= reach,
     * vertical <= reach.
     */
    private static boolean withinRect(BlockPos target,
                                      LocalPlayer player,
                                      int reach) {
        int footY = player.blockPosition().getY();
        double eyeY = player.getY() + player.getEyeHeight();
        double px = player.getX(), pz = player.getZ();
        double tx = target.getX() + 0.5, ty = target.getY() + 0.5, tz = target.getZ() + 0.5;

        if (target.getY() < footY) return false;                                   // at/above foot level
        if (Math.abs(tx - px) > reach || Math.abs(tz - pz) > reach) return false;  // horizontal
        return ty <= eyeY + reach;                                                 // vertical
    }

    /**
     * Spherical (Euclidean-distance) range: distance <= reach.
     */
    private static boolean withinSphere(BlockPos target,
                                        LocalPlayer player,
                                        int reach) {
        double px = player.getX();
        double py = player.getY() + player.getEyeHeight();
        double pz = player.getZ();
        double tx = target.getX() + 0.5;
        double ty = target.getY() + 0.5;
        double tz = target.getZ() + 0.5;
        double dx = tx - px, dy = ty - py, dz = tz - pz;
        return dx * dx + dy * dy + dz * dz <= (double) reach * reach;
    }

    /**
     * Reset all state.
     */
    private static void resetState() {
        currentMiningPos = null;
        delayCounter = 0;
        restoreTool();
    }
}
