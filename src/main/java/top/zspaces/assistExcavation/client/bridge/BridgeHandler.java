package top.zspaces.assistExcavation.client.bridge;

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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import top.zspaces.assistExcavation.client.Common;

/**
 * Auto-bridge (scaffold): each tick, if the block under your feet is air, place a block there
 * so you can sprint across gaps without falling.
 *
 * Placement uses a "silent rotation" — it only sends rotation packets to the server so its
 * look-direction check passes; the client camera does not actually turn. That packet is the
 * part anti-cheat (Grim/NCP/Vulcan) can detect.
 */
@Environment(EnvType.CLIENT)
public class BridgeHandler {
    private static final Minecraft client = Minecraft.getInstance();

    // Order in which we look for a support face: horizontal first (bridge out from where you
    // came), then down, then up.
    private static final Direction[] SUPPORT_ORDER = {
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.DOWN, Direction.UP
    };

    public static void handleBridge() {
        if (!Common.isAutoBridge()) {
            return;
        }
        if (client.player == null || client.level == null || client.gameMode == null) {
            return;
        }
        LocalPlayer player = client.player;
        ClientLevel level = client.level;
        MultiPlayerGameMode gm = client.gameMode;

        // The block under your feet. If it's already solid ground, nothing to bridge.
        BlockPos target = player.blockPosition().below();
        BlockState targetState = level.getBlockState(target);
        if (!targetState.canBeReplaced()) {
            return;
        }

        // Find a placeable solid block in the hotbar.
        Inventory inv = player.getInventory();
        int blockSlot = findBlockSlot(inv, level, target);
        if (blockSlot < 0) {
            return; // no usable building blocks — can't help, you fall as normal
        }

        // Find a support face: a neighbour of `target` whose face toward `target` is sturdy.
        Direction supportDir = null;
        for (Direction d : SUPPORT_ORDER) {
            BlockPos against = target.relative(d);
            if (level.getBlockState(against).isFaceSturdy(level, against, d.getOpposite())) {
                supportDir = d;
                break;
            }
        }
        if (supportDir == null) {
            return; // nothing to place against
        }

        BlockPos against = target.relative(supportDir);
        Direction clickedFace = supportDir.getOpposite(); // the face of `against` facing `target`
        // Hit location = centre of the shared face between `target` and `against`.
        Vec3 hitVec = Vec3.atCenterOf(target).add(
                supportDir.getStepX() * 0.5,
                supportDir.getStepY() * 0.5,
                supportDir.getStepZ() * 0.5);
        BlockHitResult hit = new BlockHitResult(hitVec, clickedFace, against, false);

        // Compute the yaw/pitch that look at the hit point (for the silent rotation).
        Vec3 eye = player.getEyePosition();
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

        // 1) Silent rotation: send packets only, don't move the camera.
        player.connection.send(new ServerboundMovePlayerPacket.Rot(spoofYaw, spoofPitch, onGround, horizCol));
        // 2) Switch to the building block.
        if (blockSlot != prevSlot) {
            inv.setSelectedSlot(blockSlot);
            player.connection.send(new ServerboundSetCarriedItemPacket(blockSlot));
        }
        // 3) Place.
        gm.useItemOn(player, InteractionHand.MAIN_HAND, hit);
        player.swing(InteractionHand.MAIN_HAND);
        // 4) Restore the held slot.
        if (blockSlot != prevSlot) {
            inv.setSelectedSlot(prevSlot);
            player.connection.send(new ServerboundSetCarriedItemPacket(prevSlot));
        }
        // 5) Restore the real look direction.
        player.connection.send(new ServerboundMovePlayerPacket.Rot(realYaw, realPitch, onGround, horizCol));
    }

    /**
     * Find the first placeable "full solid block" in the hotbar (slots 0..8).
     * Returns the slot index, or -1 if none.
     */
    private static int findBlockSlot(Inventory inv, ClientLevel level, BlockPos at) {
        for (int slot = 0; slot < Inventory.SELECTION_SIZE; slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof BlockItem blockItem) {
                BlockState state = blockItem.getBlock().defaultBlockState();
                if (state.isCollisionShapeFullBlock(level, at)) {
                    return slot;
                }
            }
        }
        return -1;
    }
}
