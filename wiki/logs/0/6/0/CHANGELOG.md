# 0.6.0

Released: 2026-10-13

Core draws holograms: panels a mod opens in the world in front of the player, who aims at
them and clicks instead of opening a screen. The web console is gone. Holograms are how
MCAgents mods talk to the player now.

## Upgrading

- **The web console is gone.** `api.web` (`WebRegistry`, `WebModule`, `WebConsole`,
  `WebConsoleException`, `WebRequest`, `WebResponse`, `NavLink`) and `common.web`
  (`CoreWebRegistry`) are removed. A mod that registered pages has nothing to register them
  into. It opens a hologram instead; see [`wiki/api/hologram.md`](../../../../api/hologram.md).
- **MCAgents chat older than 0.4.0 does not load beside this core.** Those versions open the
  console, so Fabric and NeoForge refuse to start and name the mod to update. Update chat to
  0.4.0, or keep core 0.5.3 until then.
- **To open holograms, add `mcagents-core-client`** as `compileOnly` in a module that compiles
  against Minecraft 26.2; see [`wiki/api/dependency.md`](../../../../api/dependency.md). A mod
  that only calls the model client changes nothing.

## Added

- **Holograms.** The records in `api.hologram` describe one: `Hologram`, a set of up to three
  panels, and the panel kinds `ButtonGrid` (a menu, five buttons a row, three rows showing),
  `SelectList`, `Transcript` with `TranscriptEntry`, and `PromptInput`. `Holograms` opens,
  toggles and closes them, one at a time.
- **What the player does with one.** Aims and left-clicks a button or row without hitting the
  block behind; scrolls a panel with the mouse wheel; holds Z, rebindable under *Controls*,
  *MCAgents Core*, to carry the hologram and lets go to leave it; clicks its X to close it. It
  closes on its own once the player is more than two chunks from it, or changes dimension.
- **Typing into a prompt** through a screen that draws nothing over the world: Enter sends,
  Shift+Enter starts a new line, Escape or a click stops and keeps the text.
- **`platforms/client`**, a new module compiled against vanilla Minecraft, published as
  `mcagents-core-client` (jar `MCAgentsCoreClient`) and bundled in every loader jar. It holds
  the drawing and the input; the arithmetic is in `common.hologram`, tested without a game.
- **A development client per loader**: `./gradlew :platforms:{fabric,neoforge,forge}:runClient`.
- [`wiki/api/hologram.md`](../../../../api/hologram.md) and
  [`wiki/api/hologram-panels.md`](../../../../api/hologram-panels.md).

## Changed

- The Forge jar carries a mixin that draws the hologram, named in its `MixinConfigs` manifest
  attribute; Forge 26.2 has no world-render event.
- `CoreRuntime.stop()` stops the running half and nothing else.
- Each loader manifest describes core as the model client and the holograms.

## Removed

- The web console: `api.web`, `common.web`, the page shell under `mcagents_core/web/`, and
  the pages `wiki/api/web-console.md` and `wiki/guides/web-pages.md`.
