package com.b0btheskull.assistExcavation.client.excavation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import com.b0btheskull.assistExcavation.client.Common;

/**
 * Shared block sourcing for LavaGuard and AutoBridge: find a placeable full block in the hotbar
 * (preferring non-flammable, so a lava cap or bridge doesn't catch fire), and — when the hotbar has
 * none — optionally restock one from the main inventory into a free hotbar slot via a silent
 * container SWAP, mirroring the tool-restock path. In the Nether this naturally pulls netherrack,
 * which is non-flammable and abundant, so the guard/bridge keep working after the hotbar runs dry.
 */
@Environment(EnvType.CLIENT)
public final class BlockSupply {
    private BlockSupply() {
    }

    /**
     * Return a hotbar slot (0..8) holding a placeable full block to put at {@code at}, preferring a
     * non-flammable block. If none is in the hotbar and block-restock is enabled (and not in
     * server-safe mode, with the inventory menu active), pull a suitable block down from the main
     * inventory into an empty hotbar slot and return that slot. Returns -1 if no block is available
     * anywhere, or there is no empty hotbar slot to receive a restock.
     */
    public static int findOrRestockBlockSlot(LocalPlayer player, ClientLevel level, BlockPos at) {
        Inventory inv = player.getInventory();

        int hotbarSlot = findHotbarBlockSlot(inv, level, at);
        if (hotbarSlot >= 0) {
            return hotbarSlot;
        }

        // Hotbar is out of usable blocks — try to restock from the main inventory.
        if (!Common.isRestockBlocks() || Common.isServerSafe()) {
            return -1;
        }
        return restockFromInventory(player, level, at);
    }

    /**
     * Find a placeable full block in the hotbar (0..8), preferring a non-flammable one and falling
     * back to a flammable full block. Returns the slot, or -1 if none.
     */
    private static int findHotbarBlockSlot(Inventory inv, ClientLevel level, BlockPos at) {
        int flammableFallback = -1;
        for (int slot = 0; slot < Inventory.SELECTION_SIZE; slot++) {
            ItemStack stack = inv.getItem(slot);
            if (!isFullBlock(stack, level, at)) {
                continue;
            }
            if (isFlammable(stack)) {
                if (flammableFallback < 0) {
                    flammableFallback = slot; // last resort
                }
                continue;
            }
            return slot; // non-flammable full block — ideal
        }
        return flammableFallback;
    }

    /**
     * Pull the best full block from the main inventory (slots 9..35) into an empty hotbar slot via a
     * silent container SWAP, preferring a non-flammable block. Returns the receiving hotbar slot, or
     * -1 if there is no suitable block or no empty hotbar slot. Detectable inventory manipulation, so
     * callers gate it behind restock-blocks / server-safe.
     */
    private static int restockFromInventory(LocalPlayer player, ClientLevel level, BlockPos at) {
        Minecraft client = Minecraft.getInstance();
        // The player inventory menu must be the active container for the SWAP to be accepted.
        if (client.gameMode == null || player.containerMenu != player.inventoryMenu) {
            return -1;
        }
        Inventory inv = player.getInventory();

        int emptyHotbar = firstEmptyHotbarSlot(inv);
        if (emptyHotbar < 0) {
            return -1; // nowhere to put a restocked stack
        }

        int bestSlot = -1;
        boolean bestNonFlammable = false;
        for (int slot = Inventory.SELECTION_SIZE; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack candidate = inv.getItem(slot); // 9..35 = main inventory (non-hotbar)
            if (!isFullBlock(candidate, level, at)) {
                continue;
            }
            boolean nonFlammable = !isFlammable(candidate);
            if (bestSlot < 0 || (nonFlammable && !bestNonFlammable)) {
                bestSlot = slot;
                bestNonFlammable = nonFlammable;
                if (nonFlammable) {
                    break; // can't do better than a non-flammable full block
                }
            }
        }
        if (bestSlot < 0) {
            return -1;
        }

        // In the player inventory menu, main-inventory slots map 1:1 to container slots (9..35);
        // SWAP exchanges that slot with the hotbar slot named by the button (0..8), so the block
        // lands in the empty hotbar slot and we hold it ready to place.
        client.gameMode.handleContainerInput(player.inventoryMenu.containerId, bestSlot,
                emptyHotbar, ContainerInput.SWAP, player);
        return emptyHotbar;
    }

    private static int firstEmptyHotbarSlot(Inventory inv) {
        for (int slot = 0; slot < Inventory.SELECTION_SIZE; slot++) {
            if (inv.getItem(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    /** Whether {@code stack} is a block item whose default state is a full collision cube at {@code at}. */
    private static boolean isFullBlock(ItemStack stack, ClientLevel level, BlockPos at) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        BlockState state = blockItem.getBlock().defaultBlockState();
        return state.isCollisionShapeFullBlock(level, at);
    }

    /** Whether {@code stack}'s block can be set alight by adjacent lava. */
    private static boolean isFlammable(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem blockItem) {
            return blockItem.getBlock().defaultBlockState().ignitedByLava();
        }
        return false;
    }
}
