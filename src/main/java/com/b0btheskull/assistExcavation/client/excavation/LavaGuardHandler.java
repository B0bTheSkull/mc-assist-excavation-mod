package com.b0btheskull.assistExcavation.client.excavation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import com.b0btheskull.assistExcavation.client.Common;

/**
 * Lava guard: while assist mining is active and {@link Common#isLavaGuard()} is on, find the
 * nearest lava SOURCE block within the configured scan radius (and within the server's interaction
 * range, so it's actually placeable) and cap it with a full block from the hotbar. This suspends
 * mining for the tick(s) it takes to seal nearby lava, then mining resumes automatically.
 *
 * Placement reuses the auto-bridge's "silent rotation" approach: rotation packets are sent so the
 * server's look-direction check passes, but the camera doesn't actually turn. Lava blocks are
 * replaceable, so the placed block lands directly in the lava position. Server-safe mode behaves
 * like the bridge: no rotation spoof, and a placement happens only when the player is already
 * looking close enough to the target.
 */
@Environment(EnvType.CLIENT)
public class LavaGuardHandler {
    private static final Minecraft client = Minecraft.getInstance();

    /**
     * Attempt to cap the nearest reachable lava source. Returns true if a placement was made this
     * tick (the caller then suspends mining for the tick). Returns false when there is no lava to
     * cap, no placeable block, or (in server-safe mode) the player isn't looking near the target.
     */
    public static boolean tryGuard() {
        if (client.player == null || client.level == null || client.gameMode == null) {
            return false;
        }
        LocalPlayer player = client.player;
        ClientLevel level = client.level;
        MultiPlayerGameMode gm = client.gameMode;

        double serverReach = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
        BlockPos lava = findNearestLavaSource(player, level, Common.getLavaGuardRadius(), serverReach);
        if (lava == null) {
            return false;
        }

        // Need a placeable full block to cap it (pulling one from the inventory if the hotbar's
        // out). If we have none, we can't help — let mining run.
        Inventory inv = player.getInventory();
        int blockSlot = BlockSupply.findOrRestockBlockSlot(player, level, lava);
        if (blockSlot < 0) {
            return false;
        }

        // Aim at the lava block's player-facing side. Lava is replaceable, so the block lands in
        // the lava position regardless of which face we "click".
        Vec3 eye = player.getEyePosition();
        Vec3 center = Vec3.atCenterOf(lava);
        Vec3 toPlayer = eye.subtract(center);
        Direction face = Direction.getApproximateNearest(toPlayer.x, toPlayer.y, toPlayer.z);
        Vec3 hitVec = center.add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        BlockHitResult hit = new BlockHitResult(hitVec, face, lava, false);

        // Yaw/pitch that look at the hit point (for the silent rotation).
        double dx = hitVec.x - eye.x;
        double dy = hitVec.y - eye.y;
        double dz = hitVec.z - eye.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float spoofYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float spoofPitch = (float) (-Math.toDegrees(Math.atan2(dy, horiz)));

        float realYaw = player.getYRot();
        float realPitch = player.getXRot();
        boolean onGround = player.onGround();
        boolean horizCol = player.horizontalCollision;
        int prevSlot = inv.getSelectedSlot();

        // Server-safe mode: silent rotation is the detectable part. Only place if the player is
        // already looking near the target; otherwise skip (mining continues), matching the bridge.
        boolean serverSafe = Common.isServerSafe();
        boolean spoofRotation = !serverSafe;
        if (serverSafe && !isLookingNear(realYaw, realPitch, spoofYaw, spoofPitch)) {
            return false;
        }

        // 1) Silent rotation (skipped in server-safe mode).
        if (spoofRotation) {
            player.connection.send(new ServerboundMovePlayerPacket.Rot(spoofYaw, spoofPitch, onGround, horizCol));
        }
        // 2) Switch to the capping block.
        if (blockSlot != prevSlot) {
            inv.setSelectedSlot(blockSlot);
            player.connection.send(new ServerboundSetCarriedItemPacket(blockSlot));
        }
        // 3) Place into the lava, and remember the cap so the excavator never mines it back open.
        gm.useItemOn(player, InteractionHand.MAIN_HAND, hit);
        player.swing(InteractionHand.MAIN_HAND);
        PlacedBlockRegistry.record(lava);
        // 4) Restore the held slot.
        if (blockSlot != prevSlot) {
            inv.setSelectedSlot(prevSlot);
            player.connection.send(new ServerboundSetCarriedItemPacket(prevSlot));
        }
        // 5) Restore the real look direction (skipped in server-safe mode).
        if (spoofRotation) {
            player.connection.send(new ServerboundMovePlayerPacket.Rot(realYaw, realPitch, onGround, horizCol));
        }
        return true;
    }

    /**
     * Find the nearest lava SOURCE block within {@code radius} (Euclidean, from the eye) that is
     * also within {@code serverReach} so it can actually be placed on. Returns null if none.
     */
    private static BlockPos findNearestLavaSource(LocalPlayer player, ClientLevel level,
                                                  int radius, double serverReach) {
        Vec3 eye = player.getEyePosition();
        BlockPos origin = player.blockPosition();
        double radiusSq = (double) radius * radius;
        double reachSq = serverReach * serverReach;

        BlockPos best = null;
        double bestDistSq = Double.MAX_VALUE;

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);

                    double cx = cursor.getX() + 0.5 - eye.x;
                    double cy = cursor.getY() + 0.5 - eye.y;
                    double cz = cursor.getZ() + 0.5 - eye.z;
                    double distSq = cx * cx + cy * cy + cz * cz;
                    if (distSq > radiusSq || distSq > reachSq || distSq >= bestDistSq) {
                        continue;
                    }

                    BlockState state = level.getBlockState(cursor);
                    FluidState fluid = state.getFluidState();
                    if (!fluid.isSource() || !fluid.is(FluidTags.LAVA)) {
                        continue;
                    }

                    best = cursor.immutable();
                    bestDistSq = distSq;
                }
            }
        }
        return best;
    }

    // Whether the player's real look direction is within a small tolerance of the placement angle.
    private static boolean isLookingNear(float realYaw, float realPitch, float wantYaw, float wantPitch) {
        float dy = Math.abs(Mth.degreesDifference(realYaw, wantYaw));
        float dp = Math.abs(Mth.degreesDifference(realPitch, wantPitch));
        return dy <= 15.0f && dp <= 15.0f;
    }
}
