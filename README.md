# Assist Excavation — Minecraft 26.1.2 (Fabric)

A port of [zhwuyuhehe/assist-excavation](https://github.com/zhwuyuhehe/assist-excavation)
(originally MC 1.21.4, Yarn mappings) to **Minecraft 26.1.2 / Fabric**, which uses
**Mojang official mappings**. Plus a few extra mining/movement features.

Client-side only.

## Features

**Assist mining** — toggle a BFS auto-miner that breaks blocks around you within a
configurable reach. Original behaviour from upstream, ported intact.

**Excavation modes** — pick the shape the auto-miner targets:

- **Rectangle** — Chebyshev box at/above foot level (the original behaviour).
- **Sphere** — Euclidean radius around your eyes.
- **Tunnel** — a shaft in your look direction for branch mining; switch between a
  1×2 and a 3×3 cross-section.
- **Vein** — only breaks blocks matching the one under your crosshair (classic
  veinminer), spreading through diagonally-touching neighbours.

All modes still respect the server's real interaction range, so nothing is mined
beyond where you could legitimately reach.

Other features (all toggleable unless noted):

- **No inter-block gap** — when a block finishes, the next one starts the same tick
  (instead of idling a tick). Always on.
- **Fast instant-break** — clears every instant-breakable block in range each tick
  (grass, dirt, sand, …), capped at 16/tick.
- **Auto tool switch** — switches to the fastest hotbar tool for the target block
  before breaking it (and syncs the slot to the server). Liquid blocks (lava/water)
  are never targeted, so the miner won't swing uselessly at a lava source.
- **Tool-safety guard** — a durability threshold that stops the miner from finishing
  off a near-broken tool, plus a *Protect Silk/Fortune* option that never auto-switches
  away from a Silk Touch or Fortune tool.
- **Auto-restock tools** — when the held tool hits the durability threshold and no
  fresh tool is left in the hotbar, pulls a suitable replacement tool out of your main
  inventory (a silent inventory swap) and keeps mining, instead of just stopping.
- **Lava Guard** — while assist mining, suspends mining to cap the nearest lava
  *source* block within a configurable radius with a block from your hotbar (preferring
  a non-flammable one), so you don't dig yourself into a burn. Off by default.
- **Block protection** — never breaks containers, spawners, and other block-entities
  (toggleable), plus a user block-id blacklist (e.g. `minecraft:bedrock`) edited in
  the config file.
- **Mining preview overlay** — draws a green outline around every block the active
  mode would break, so you can see the target region before it goes.
- **Server-safe mode** — one switch that disables the detectable techniques: it caps
  instant-breaks to one per tick and stops the auto-bridge from sending silent-rotation
  packets (it will only place when you are already looking near the spot).
- **Auto-bridge (scaffold)** — places a block under your feet as you move so you can
  sprint across gaps without falling. Auto-picks a solid block from your hotbar and
  switches back. **See the warning below.**

Settings persist across restarts (saved to `config/assist-excavation.json`).

## Keybinds (defaults)

| Action            | Key       |
|-------------------|-----------|
| Toggle assist     | `J`       |
| Toggle auto-bridge| `K`       |
| Toggle Lava Guard | unbound   |
| Open config       | unbound (use Mod Menu, or bind it in Controls) |

Defaults were chosen to avoid conflicts with a heavily-bound setup; rebind freely in
Options → Controls, where all of this mod's binds are grouped under their own
**Assist Excavation** category. All toggles are also in the config screen
(Mod Menu → Assist Excavation).

## ⚠️ Detectable techniques and anti-cheat

A few features rely on packet tricks that anti-cheats (Grim/NCP/Vulcan) can flag:

- **Auto-bridge** and **Lava Guard** place blocks while you look anywhere by sending a
  **silent rotation** to the server (the camera doesn't actually turn).
- **Auto-restock tools** sends an **inventory swap** click with no inventory screen open.

On a vanilla / no-anti-cheat server or in singleplayer these are accepted. Elsewhere
they may get you flagged or banned — use them only where you accept that risk.
**Server-Safe mode** disables all of these: it caps instant-breaks to one per tick,
skips the restock inventory swap, and only places (bridge/lava) when you're already
looking near the spot so no silent rotation is sent.

## Building

Requires JDK 25 (Gradle auto-provisions it via the toolchain) and the Mod Menu jar:

1. Drop a `modmenu-*.jar` (the 26.1.2 build) into `libs/` — it's a compile-only
   dependency and is git-ignored, not redistributed here.
2. `./gradlew build`
3. The remapped jar lands in `build/libs/assist-excavation-<version>.jar`.

## Credits & license

This 26.1.2 port is maintained by [B0bTheSkull](https://github.com/B0bTheSkull/mc-assist-excavation-mod).
**Report issues here:** <https://github.com/B0bTheSkull/mc-assist-excavation-mod/issues>.

Based on the original **Assist Excavation** mod by
[zhwuyuhehe](https://github.com/zhwuyuhehe/assist-excavation) © zhwuyuhehe,
licensed Apache-2.0. This port and its additions are distributed under the same
[Apache-2.0 license](LICENSE).
