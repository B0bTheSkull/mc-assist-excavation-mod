# Assist Excavation — Minecraft 26.1.2 (Fabric)

A port of [zhwuyuhehe/assist-excavation](https://github.com/zhwuyuhehe/assist-excavation)
(originally MC 1.21.4, Yarn mappings) to **Minecraft 26.1.2 / Fabric**, which uses
**Mojang official mappings**. Plus a few extra mining/movement features.

Client-side only.

## Features

**Assist mining** — toggle a BFS auto-miner that breaks blocks around you within a
configurable reach. Original behaviour from upstream, ported intact.

Added in this fork:

- **No inter-block gap** — when a block finishes, the next one starts the same tick
  (instead of idling a tick). Always on.
- **Fast instant-break** — clears every instant-breakable block in range each tick
  (grass, dirt, sand, …), capped at 16/tick. Toggleable.
- **Auto tool switch** — switches to the fastest hotbar tool for the target block
  before breaking it (and syncs the slot to the server). Toggleable.
  Note: ranks by break speed only, so it can switch away from a Silk Touch / Fortune
  tool — turn it off if you care about enchanted drops.
- **Auto-bridge (scaffold)** — places a block under your feet as you move so you can
  sprint across gaps without falling. Auto-picks a solid block from your hotbar and
  switches back. Toggleable. **See the warning below.**

## Keybinds (defaults)

| Action            | Key       |
|-------------------|-----------|
| Toggle assist     | `J`       |
| Toggle auto-bridge| `K`       |
| Open config       | unbound (use Mod Menu, or bind it in Controls) |

Defaults were chosen to avoid conflicts with a heavily-bound setup; rebind freely in
Options → Controls (they appear under *Miscellaneous*). All toggles are also in the
config screen (Mod Menu → Assist Excavation).

## ⚠️ Auto-bridge and anti-cheat

The auto-bridge places blocks while you look anywhere by sending a **silent rotation**
to the server (the camera doesn't actually turn). On a vanilla / no-anti-cheat server
or in singleplayer this is accepted. On servers running Grim/NCP/Vulcan it is a
**detectable technique** and can get you flagged or banned. Use it only where you
accept that risk. The upstream mod's stated goal is to respect server rules; this fork
deliberately adds a feature that does not, so it's off by default.

## Building

Requires JDK 25 (Gradle auto-provisions it via the toolchain) and the Mod Menu jar:

1. Drop a `modmenu-*.jar` (the 26.1.2 build) into `libs/` — it's a compile-only
   dependency and is git-ignored, not redistributed here.
2. `./gradlew build`
3. The remapped jar lands in `build/libs/assist-excavation-<version>.jar`.

## Credits & license

Original mod © zhwuyuhehe, licensed Apache-2.0. This port and its additions are
distributed under the same [Apache-2.0 license](LICENSE). See upstream:
<https://github.com/zhwuyuhehe/assist-excavation>.
