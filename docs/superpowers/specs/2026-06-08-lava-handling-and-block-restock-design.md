# Lava handling cleanup + block restock — design

Date: 2026-06-08

## Problem

In the Nether (and anywhere with lava), the assist-excavation mod thrashes:

1. **Re-mining its own placements.** `LavaGuardHandler.tryGuard()` caps a lava
   source with a block from the hotbar. Nothing records that the block was
   placed by the mod, so on the next tick the lava source is gone, the guard
   does nothing, and `ExcavationHandler`'s BFS mines the fresh cap (it is just
   an in-range breakable block) — re-exposing the lava. The guard re-caps, and
   the cycle repeats: place → mine → place → mine. The same can happen to
   `BridgeHandler` blocks in sphere/vein modes, which can reach blocks under or
   around the player.

2. **Runs dry too easily.** Both `LavaGuardHandler.findBlockSlot` and
   `BridgeHandler.findBlockSlot` only scan the hotbar (slots 0–8). When the
   hotbar runs out of building blocks, both features stop helping even though
   the main inventory is full of netherrack.

## Goals

- Excavation must never mine a block the mod itself placed (lava caps **and**
  bridge blocks).
- When the hotbar has no usable building block, pull one down from the main
  inventory (netherrack being the common Nether case), for both LavaGuard and
  AutoBridge — mirroring the existing tool-restock behavior.

## Non-goals

- No change to the silent-rotation / server-safe placement mechanics.
- No broader refactor of the duplicated placement code between the two
  handlers (only the block-finding path is unified, since that is what changes).

## Design

### 1. Placed-block registry

New shared class `PlacedBlockRegistry` in
`com.b0btheskull.assistExcavation.client.excavation`.

State:
- `HashSet<BlockPos>` of positions the mod placed this session.
- A reference to the last-seen `ClientLevel` (or its identity) so the set can
  be cleared on world change / disconnect.

API:
- `record(BlockPos pos)` — add a placed position. Called by `LavaGuardHandler`
  and `BridgeHandler` immediately after a successful placement.
- `isProtected(BlockPos pos)` — true if `pos` is in the set.
- `prune(LocalPlayer player)` / lazy maintenance — drop entries that are out of
  the server interaction range. An out-of-reach block cannot be mined anyway,
  so it no longer needs protection; this bounds the set to roughly the reach
  volume without needing a tick clock. Reuses the same reach test as
  `ExcavationHandler.isWithinServerReach`.
- `onLevelMaybeChanged(ClientLevel level)` — if the level instance differs from
  the last seen, clear the set. Called once per excavation tick.

Lifecycle:
- Persists across mining on/off toggles (caps stay protected while you keep
  working near them).
- Cleared on world change / disconnect.
- Out-of-reach entries pruned each excavation tick.

### 2. `ExcavationHandler` wiring

- In `tryMine`, after the existing protection checks, return `MINE_NONE` when
  `PlacedBlockRegistry.isProtected(pos)`.
- In `isBreakableCandidate`, return false when
  `PlacedBlockRegistry.isProtected(pos)`, so the preview overlay matches what
  is actually mined.
- Once per `handleExcavation` tick (before mining), call
  `PlacedBlockRegistry.onLevelMaybeChanged(client.level)` and
  `PlacedBlockRegistry.prune(player)`.

### 3. Block restock — `BlockSupply`

New shared helper `BlockSupply` in
`com.b0btheskull.assistExcavation.client.excavation`, replacing the private
`findBlockSlot` methods in both handlers:

```
int findOrRestockBlockSlot(LocalPlayer player, ClientLevel level, BlockPos at)
```

Logic:
1. Scan hotbar (0–8) for a placeable full block (`isCollisionShapeFullBlock`),
   preferring a non-flammable one (`!state.ignitedByLava()`), falling back to a
   flammable full block. (This is LavaGuard's current preference, now applied to
   the bridge too — harmless, and avoids flammable bridges.)
2. If the hotbar has none, and `Common.isRestockBlocks()` is on, and
   `!Common.isServerSafe()`, and the player's `containerMenu` is the
   `inventoryMenu`: scan main inventory (9–35) for a suitable full block (same
   non-flammable preference), find an **empty hotbar slot**, and issue a
   `ContainerInput.SWAP` to move the stack into that slot (button = the empty
   hotbar index). Return that slot.
3. Otherwise return -1 (no usable block, or no empty hotbar slot to receive a
   restock) — callers behave exactly as they do today.

Both `LavaGuardHandler` and `BridgeHandler` call `BlockSupply.findOrRestockBlockSlot`
in place of their local `findBlockSlot`, and call `PlacedBlockRegistry.record`
after a successful placement.

### 4. Config

Add to `Common`:
- `private static boolean restockBlocks = true;`
- `isRestockBlocks()` / `setRestockBlocks(boolean)`.

Persist via `ConfigManager` (same pattern as `restockTools`) and expose a
"Restock blocks" toggle in `AssistExcavationConfigScreen`, next to the existing
"Restock tools" toggle.

## Testing

Unit-testable, pure logic:
- `PlacedBlockRegistry`: record/isProtected, out-of-reach pruning, clear on
  level change.
- `BlockSupply` slot selection: hotbar-first, non-flammable preference,
  empty-hotbar-slot requirement, gating on `restockBlocks` / `serverSafe` /
  active container.

Not unit-testable (Minecraft client glue), verified in-game:
- Actual `useItemOn` placement and `ContainerInput.SWAP` execution.
- End-to-end: cap lava in the Nether, confirm the cap is not re-mined; empty the
  hotbar of blocks and confirm netherrack is pulled from the inventory for both
  LavaGuard and AutoBridge.
