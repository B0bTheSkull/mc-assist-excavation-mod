package com.b0btheskull.assistExcavation.client.excavation;

import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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

    // Excavation modes.
    private static final int MODE_RECT = 0;
    private static final int MODE_SPHERE = 1;
    private static final int MODE_TUNNEL = 2;
    private static final int MODE_VEIN = 3;

    // Max instant breaks per tick. Instant blocks (grass, dirt, sand, ...) go through
    // startDestroyBlock, which sets no destroyDelay in survival, so several can be broken
    // in one tick. The cap keeps us from flooding the server with break packets at once
    // (strict anti-cheat may flag that).
    private static final int MAX_INSTANT_PER_TICK = 16;

    // Upper bound on how many block outlines the preview overlay will gather per frame.
    private static final int PREVIEW_CAP = 512;

    // The block currently being mined, and the inter-block delay counter.
    private static BlockPos currentMiningPos = null;
    private static int delayCounter = 0;

    // The hotbar slot held before auto tool-switch began; restored when excavation is
    // turned off. -1 means there is nothing to restore.
    private static int originalSlot = -1;

    // BFS neighbour directions (rect/sphere/tunnel). No DOWN: we never dig below foot level.
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

        // Lava guard: before mining anything, cap a nearby lava source if one is in range. When it
        // acts, suspend mining for this tick (and any in-progress break) so we seal lava first.
        if (Common.isLavaGuard() && LavaGuardHandler.tryGuard()) {
            return;
        }

        // Read config.
        int delayTicks = Common.getDelayTicks();
        int reach = Common.getReach();
        int mode = Common.getExcavationMode();

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

        // Start a fresh round.
        if (mode == MODE_VEIN) {
            performVeinMining(player, im, reach);
        } else {
            performBfsMining(player, im, reach, mode);
        }
    }

    private static void performBfsMining(LocalPlayer player,
                                         MultiPlayerGameMode im,
                                         int reach, int mode) {
        BlockPos origin = player.blockPosition();

        // Local BFS structures
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        // Seed: the five neighbour directions.
        for (Direction d : DIRECTIONS) {
            BlockPos nb = origin.relative(d);
            if (isWithinReach(nb, player, reach, mode) && isWithinServerReach(nb, player)) {
                visited.add(nb);
                queue.add(nb);
            }
        }

        // Per-tick instant-break cap. Server-safe mode forces one-per-tick to avoid packet
        // bursts; otherwise "fast instant-break" clears several, off falls back to one.
        int instantCap = instantCap();

        // BFS: nearest first.
        int instantBroken = 0;
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();

            int result = tryMine(pos, player, im, null);
            if (result == MINE_PROGRESS) {
                player.swing(InteractionHand.MAIN_HAND);
                return;
            }
            if (result == MINE_INSTANT) {
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
                if (!isWithinServerReach(next, player)) continue;
                visited.add(next);
                queue.add(next);
            }
        }

        if (instantBroken > 0) {
            player.swing(InteractionHand.MAIN_HAND);
        }
    }

    /**
     * Vein mode: seed from the block under the crosshair and only break blocks of the same type,
     * spreading through the full 26-neighbour adjacency (so diagonally-touching ore is included).
     * Still bounded by reach and the server's interaction range, keeping it legitimate.
     */
    private static void performVeinMining(LocalPlayer player,
                                          MultiPlayerGameMode im,
                                          int reach) {
        HitResult hit = client.hitResult;
        if (!(hit instanceof BlockHitResult bhr) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos seed = bhr.getBlockPos();
        BlockState seedState = Objects.requireNonNull(client.level).getBlockState(seed);
        if (seedState.isAir()) {
            return;
        }
        Block target = seedState.getBlock();

        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        visited.add(seed);
        queue.add(seed);

        int instantCap = instantCap();
        int instantBroken = 0;

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();

            if (isWithinSphere(pos, player, reach) && isWithinServerReach(pos, player)) {
                int result = tryMine(pos, player, im, target);
                if (result == MINE_PROGRESS) {
                    player.swing(InteractionHand.MAIN_HAND);
                    return;
                }
                if (result == MINE_INSTANT) {
                    instantBroken++;
                    if (instantBroken >= instantCap) {
                        player.swing(InteractionHand.MAIN_HAND);
                        return;
                    }
                }
            }

            // Expand to all 26 neighbours that are the same block type and still in range.
            for (BlockPos next : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
                BlockPos np = next.immutable();
                if (np.equals(pos) || visited.contains(np)) continue;
                if (!isWithinSphere(np, player, reach) || !isWithinServerReach(np, player)) continue;
                if (!client.level.getBlockState(np).is(target)) continue;
                visited.add(np);
                queue.add(np);
            }
        }

        if (instantBroken > 0) {
            player.swing(InteractionHand.MAIN_HAND);
        }
    }

    /**
     * Compute the set of blocks the current mode would target right now, for the preview overlay.
     * Mirrors the targeting filters (range, server-reach, protection, vein seed) but never mines.
     * Bounded by {@link #PREVIEW_CAP} so a huge sphere/vein can't tank the frame.
     */
    public static List<BlockPos> computePreviewTargets() {
        List<BlockPos> out = new ArrayList<>();
        if (!AssistExcavationKeyBindings.isExcavationEnabled()) {
            return out;
        }
        if (client.player == null || client.level == null) {
            return out;
        }
        LocalPlayer player = client.player;
        int reach = Common.getReach();
        int mode = Common.getExcavationMode();

        if (mode == MODE_VEIN) {
            HitResult hit = client.hitResult;
            if (!(hit instanceof BlockHitResult bhr) || hit.getType() != HitResult.Type.BLOCK) {
                return out;
            }
            BlockPos seed = bhr.getBlockPos();
            BlockState seedState = client.level.getBlockState(seed);
            if (seedState.isAir()) {
                return out;
            }
            Block target = seedState.getBlock();
            Set<BlockPos> visited = new HashSet<>();
            Queue<BlockPos> queue = new ArrayDeque<>();
            visited.add(seed);
            queue.add(seed);
            while (!queue.isEmpty() && out.size() < PREVIEW_CAP) {
                BlockPos pos = queue.poll();
                if (isWithinSphere(pos, player, reach) && isWithinServerReach(pos, player)
                        && isBreakableCandidate(pos, target)) {
                    out.add(pos);
                }
                for (BlockPos next : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
                    BlockPos np = next.immutable();
                    if (np.equals(pos) || visited.contains(np)) continue;
                    if (!isWithinSphere(np, player, reach) || !isWithinServerReach(np, player)) continue;
                    if (!client.level.getBlockState(np).is(target)) continue;
                    visited.add(np);
                    queue.add(np);
                }
            }
            return out;
        }

        // Rect/Sphere/Tunnel: BFS over the in-range region, collecting breakable candidates.
        BlockPos origin = player.blockPosition();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        for (Direction d : DIRECTIONS) {
            BlockPos nb = origin.relative(d);
            if (isWithinReach(nb, player, reach, mode) && isWithinServerReach(nb, player)) {
                visited.add(nb);
                queue.add(nb);
            }
        }
        while (!queue.isEmpty() && out.size() < PREVIEW_CAP) {
            BlockPos pos = queue.poll();
            if (isBreakableCandidate(pos, null)) {
                out.add(pos);
            }
            for (Direction d : DIRECTIONS) {
                BlockPos next = pos.relative(d);
                if (visited.contains(next)) continue;
                if (!isWithinReach(next, player, reach, mode)) continue;
                if (!isWithinServerReach(next, player)) continue;
                visited.add(next);
                queue.add(next);
            }
        }
        return out;
    }

    /** Whether a position holds a block this mod would actually break (range checks done by caller). */
    private static boolean isBreakableCandidate(BlockPos pos, Block requiredBlock) {
        BlockState state = Objects.requireNonNull(client.level).getBlockState(pos);
        if (state.isAir() || state.getBlock() instanceof LiquidBlock
                || state.getDestroySpeed(client.level, pos) < 0) {
            return false;
        }
        if (requiredBlock != null && !state.is(requiredBlock)) {
            return false;
        }
        return !isProtectedBlock(state);
    }

    /** Per-tick instant-break cap, honouring server-safe and fast-instant-break settings. */
    private static int instantCap() {
        if (Common.isServerSafe()) {
            return 1;
        }
        return Common.isFastInstantBreak() ? MAX_INSTANT_PER_TICK : 1;
    }

    /**
     * Core mining attempt. Returns MINE_NONE / MINE_PROGRESS / MINE_INSTANT.
     * When {@code requiredBlock} is non-null (vein mode), only that block type is mined.
     */
    private static int tryMine(BlockPos pos,
                               LocalPlayer player,
                               MultiPlayerGameMode im,
                               Block requiredBlock) {
        if (!isWithinServerReach(pos, player)) {
            return MINE_NONE;
        }

        BlockState state = Objects.requireNonNull(client.level).getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(client.level, pos) < 0) {
            return MINE_NONE; // air, or unbreakable (e.g. bedrock, hardness -1)
        }

        // Never try to "mine" a liquid block: you can't break lava/water by mining, and attempting
        // it just makes the excavator swing at running lava sources instead of skipping past them.
        if (state.getBlock() instanceof LiquidBlock) {
            return MINE_NONE;
        }

        // Vein mode: skip anything that isn't the seeded block.
        if (requiredBlock != null && !state.is(requiredBlock)) {
            return MINE_NONE;
        }

        // Block protection: never break protected blocks (containers/spawners) or blacklisted ids.
        if (isProtectedBlock(state)) {
            return MINE_NONE;
        }

        // Ensure we're holding a usable, efficient tool for this block: switch within the hotbar to
        // the fastest above-threshold tool, and if the held tool is worn out with no hotbar backup,
        // pull a fresh one from the main inventory. Stops only when no usable tool remains anywhere.
        if (!ensureUsableTool(player, state)) {
            return MINE_NONE;
        }

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

    /** Whether a block must never be auto-mined (block-entity protection or user blacklist). */
    private static boolean isProtectedBlock(BlockState state) {
        if (Common.isProtectBlockEntities() && state.hasBlockEntity()) {
            return true;
        }
        Set<String> blacklist = Common.getBlockBlacklist();
        if (blacklist.isEmpty()) {
            return false;
        }
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        return blacklist.contains(id);
    }

    /**
     * Make sure the player is holding a usable, efficient tool for {@code state}. Switches within
     * the hotbar to the fastest above-threshold tool first; if the durability guard is on and the
     * held tool is still worn out (no fresh hotbar tool), optionally pulls a fresh tool from the
     * main inventory. Returns false only when no usable tool remains, so the caller stops rather
     * than break the player's last good tool.
     */
    private static boolean ensureUsableTool(LocalPlayer player, BlockState state) {
        maybeSwitchTool(player, state);

        int threshold = Common.getDurabilityThreshold();
        if (threshold <= 0) {
            return true; // durability guard off: never blocks mining
        }

        Inventory inv = player.getInventory();
        if (!isBelowThreshold(inv.getItem(inv.getSelectedSlot()), threshold)) {
            return true; // held tool (or bare hand) is fine
        }

        // Held tool is worn out and the hotbar has no fresh replacement: try the main inventory.
        if (Common.isRestockTools() && !Common.isServerSafe() && restockFromInventory(player, state)) {
            return !isBelowThreshold(inv.getItem(inv.getSelectedSlot()), threshold);
        }
        return false;
    }

    /**
     * Pull the best fresh mining tool from the main inventory (slots 9..35) into the held hotbar
     * slot via a silent container SWAP, so mining continues after a tool wears out. Only considers
     * damageable tools above the durability threshold and picks the fastest for this block. Returns
     * true if a swap happened. This is detectable inventory manipulation, so callers gate it behind
     * Server-Safe mode.
     */
    private static boolean restockFromInventory(LocalPlayer player, BlockState state) {
        // The player inventory menu must be the active container (no other screen open) for the
        // SWAP to be accepted by the server.
        if (client.gameMode == null || player.containerMenu != player.inventoryMenu) {
            return false;
        }
        int threshold = Common.getDurabilityThreshold();
        Inventory inv = player.getInventory();

        int bestSlot = -1;
        float bestSpeed = 1.0f; // require better than a bare hand before bothering to swap
        for (int slot = Inventory.SELECTION_SIZE; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack candidate = inv.getItem(slot); // 9..35 = the main inventory (non-hotbar)
            if (candidate.isEmpty() || !candidate.isDamageableItem()) {
                continue; // only restock actual (damageable) tools
            }
            if (isBelowThreshold(candidate, threshold)) {
                continue; // skip near-broken replacements
            }
            float speed = candidate.getDestroySpeed(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = slot;
            }
        }
        if (bestSlot < 0) {
            return false; // no suitable fresh tool in the inventory
        }

        // In the player inventory menu, main-inventory slots map 1:1 to container slots (9..35);
        // SWAP exchanges that slot with the held hotbar slot (button = hotbar index 0..8).
        client.gameMode.handleContainerInput(player.inventoryMenu.containerId, bestSlot,
                inv.getSelectedSlot(), ContainerInput.SWAP, player);
        return true;
    }

    /** A damageable item is "below threshold" when its remaining durability is at/under it. */
    private static boolean isBelowThreshold(ItemStack stack, int threshold) {
        if (stack.isEmpty() || !stack.isDamageableItem()) {
            return false;
        }
        int remaining = stack.getMaxDamage() - stack.getDamageValue();
        return remaining <= threshold;
    }

    /**
     * Optimization #3: find the hotbar tool with the highest destroy speed for the target block
     * and switch to it. Honours the tool-safety guard: never switches away from an enchanted
     * (Silk Touch / Fortune) tool when that protection is on, and never switches to a tool whose
     * remaining durability is at/below the threshold.
     */
    private static void maybeSwitchTool(LocalPlayer player, BlockState state) {
        if (!Common.isAutoToolSwitch()) {
            return;
        }
        Inventory inv = player.getInventory();
        int current = inv.getSelectedSlot();

        // Protect enchanted tools: don't switch away from Silk Touch / Fortune.
        if (Common.isProtectEnchanted() && hasSilkOrFortune(inv.getItem(current))) {
            return;
        }

        int threshold = Common.getDurabilityThreshold();
        // If the held tool is itself worn out, disqualify its speed (-1) so we switch to the best
        // fresh tool available — even a slower one — instead of finishing off a near-dead tool.
        boolean currentExhausted = threshold > 0 && isBelowThreshold(inv.getItem(current), threshold);
        float bestSpeed = currentExhausted ? -1.0f : inv.getItem(current).getDestroySpeed(state);
        int best = current;
        for (int slot = 0; slot < Inventory.SELECTION_SIZE; slot++) {
            ItemStack candidate = inv.getItem(slot);
            if (candidate.isEmpty()) {
                continue; // don't switch to an empty hand; the restock path handles no-tool cases
            }
            if (threshold > 0 && isBelowThreshold(candidate, threshold)) {
                continue; // never switch to a near-broken tool
            }
            float speed = candidate.getDestroySpeed(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                best = slot;
            }
        }
        if (best != current) {
            if (originalSlot < 0) {
                originalSlot = current;
            }
            inv.setSelectedSlot(best);
            player.connection.send(new ServerboundSetCarriedItemPacket(best));
        }
    }

    /** Whether a stack carries Silk Touch or Fortune. */
    private static boolean hasSilkOrFortune(ItemStack stack) {
        if (stack.isEmpty() || client.level == null) {
            return false;
        }
        var enchants = client.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> silk = enchants.getOrThrow(Enchantments.SILK_TOUCH);
        Holder<Enchantment> fortune = enchants.getOrThrow(Enchantments.FORTUNE);
        return EnchantmentHelper.getItemEnchantmentLevel(silk, stack) > 0
                || EnchantmentHelper.getItemEnchantmentLevel(fortune, stack) > 0;
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
     * Unified range check, dispatched by mode.
     */
    private static boolean isWithinReach(BlockPos target,
                                         LocalPlayer player,
                                         int reach,
                                         int mode) {
        return switch (mode) {
            case MODE_RECT -> withinRect(target, player, reach);
            case MODE_TUNNEL -> withinTunnel(target, player, reach);
            // Vein uses sphere bounds but is driven by performVeinMining; sphere is also the
            // default fallback.
            default -> isWithinSphere(target, player, reach);
        };
    }

    /**
     * Whether the block is within the server-allowed mining range.
     */
    private static boolean isWithinServerReach(BlockPos target, LocalPlayer player) {
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

        if (target.getY() < footY) return false;
        if (Math.abs(tx - px) > reach || Math.abs(tz - pz) > reach) return false;
        return ty <= eyeY + reach;
    }

    /**
     * Spherical (Euclidean-distance) range: distance <= reach.
     */
    private static boolean isWithinSphere(BlockPos target,
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
     * Directional tunnel range for branch mining: a shaft running in the player's horizontal
     * look direction. {@code reach} sets the shaft length. Cross-section is 1-wide x 2-tall by
     * default, or 3x3 when {@link Common#isTunnel3x3()} is on. The shaft starts at foot level so
     * the floor under the player stays intact.
     */
    private static boolean withinTunnel(BlockPos target,
                                        LocalPlayer player,
                                        int reach) {
        Direction facing = player.getDirection(); // horizontal: N/S/E/W
        BlockPos origin = player.blockPosition();

        int relX = target.getX() - origin.getX();
        int relY = target.getY() - origin.getY();
        int relZ = target.getZ() - origin.getZ();

        // Forward distance along the facing axis, and sideways offset along the perpendicular axis.
        Direction side = facing.getClockWise();
        int forward = relX * facing.getStepX() + relZ * facing.getStepZ();
        int sideways = relX * side.getStepX() + relZ * side.getStepZ();

        int halfWidth = Common.isTunnel3x3() ? 1 : 0;
        int maxHeight = Common.isTunnel3x3() ? 2 : 1; // 0..2 (3 tall) or 0..1 (2 tall)

        if (forward < 1 || forward > reach) return false;
        if (Math.abs(sideways) > halfWidth) return false;
        return relY >= 0 && relY <= maxHeight;
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
