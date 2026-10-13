---
name: memory-tasks-hologram
description: Adding the in-game hologram framework to core, removing the web console, and releasing 0.6.0 with it.
---

# Task: Hologram

**Status:** in progress

## The request

On 2026-10-12 the owner asked, as task 2 of two: "Get rid of website, and in game interact
only. Make it have in game hologram and we can interact in game only." The picture he gave
shows one panel: `MCAGENTS` centred at the top, a grid of five buttons per row with three rows
showing, a scrollbar on the right, and the footer "Hold Z to Move | Auto dismiss beyond 2
Chunks". In his words: a new row when there are more than five buttons, scrolling when there
are many rows; a button opens its hologram at a fixed place, which the player moves by aiming
at it and holding Z and fixes by letting go; and it disappears once the player is more than
two chunks from it — "player spawn it at chunk 1 then if player is at chunk 4 then make it
disappear. We count chunk 2 and 3 to be in area". "Make this GUI hologram function to be in
mod core."

Later that day he added a second picture for chat: three panels side by side in the same
style — Sessions (a scrollable list, the chosen one highlighted), Conversation (a scrollable
`<Player>` / `<Agent>` transcript) and User Prompt (a text box and a SEND button) — and asked
for it to be planned and built too. Core supplies the panels; chat fills them in its own
release.

## Decisions

The owner was away and had asked for the recommended option to be taken on every question.
The full plan, with the agents, chat and template halves, is kept outside this repository.

- **Drawn in the world each frame,** through each loader's render hook: Fabric's
  `LevelRenderEvents.COLLECT_SUBMITS`, NeoForge's `SubmitCustomGeometryEvent`, and on Forge,
  which has no world-render event in 26.2, a mixin on `LevelRenderer.submitFeatures`. A
  scratch copy proved the Forge mixin draws a panel and its text before any of this started.
- **Aim and left-click** presses a button; the left click is cancelled while the crosshair is
  on a hologram, so the block behind it is not mined. The mouse wheel scrolls rows while aiming
  at a panel, and the scrollbar is clickable.
- **Two chunks are counted from the chunk the hologram stands in now,** after any Z move, on
  either axis: placed in chunk 1, it stays while the player is in chunks 2 and 3 and goes in
  chunk 4. A hologram also goes when the player changes dimension.
- **Labels are upper case,** as in the picture.
- **Each hologram has a small X** in its top corner, besides the two-chunk rule.
- **A hologram is one or more panels placed and moved together.** Core offers four panel kinds:
  a button grid (the menu), a selectable list, a scrolling transcript and a text box with a
  send button. Several panels stand side by side, the outer ones turned towards the player.
- **Typing goes through an invisible screen:** clicking a text box starts it, the text appears
  on the hologram, Enter sends, Shift+Enter is a new line and Escape stops.
- **Core holds no conversation.** A panel reads its rows, lines and selection through
  suppliers the calling mod passes in, so the transcript stays in chat's store. What core keeps
  is the open holograms and the text being typed, in memory only, gone when the hologram closes.
- **A new module, `platforms/client`,** holds the drawing and the input, compiled against
  vanilla Minecraft through Loom and bundled into each loader jar like the other modules. The
  plan had `platforms/shared` compile against Minecraft instead, but
  `.agents/knowledge/minecraft-platform.md` says `platforms:shared` must never name a
  Minecraft type; a new module keeps that true. Its artifact is `mcagents-core-client`, and it
  arrives with the hologram rather than on a branch of its own, so it never stands empty.
- **One hologram at a time.** Opening a hologram closes the one already open, so the menu
  gives way to the chat hologram it opens and two never stand in the same spot.
- **Dev clients first.** None of core's jars has been loaded in a game yet, and the hologram
  can only be checked in one, so a branch of its own gives each loader a development client
  before any hologram code lands.
- **Core opens no hologram itself.** The agents mod's G key opens the menu; core registers
  only the Z key that moves an open hologram.
- **The web console goes:** `api/web`, `common/web`, its page shell and its tests, and the
  pages that document it. Removing a public API before 1.0 is a minor, so the release is 0.6.0.

**Rule set aside.** `.agents/rules/repository.md` says core is "API only … never a feature a
player uses directly". The owner's explicit instruction to put the hologram in core outranks
it. Core still adds nothing to the game on its own: nothing appears until a mod opens a
hologram. The rule's update is proposed to the owner rather than edited here.

## Tasks

| # | Title | Branch | PR |
|---|---|---|---|
| 1 | Task record | `chore/hologram-plan` | |
| 2 | Dev clients | `build/dev-clients` | |
| 3 | Hologram | `feat/hologram` | |
| 4 | Hologram panels | `feat/hologram-panels` | |
| 5 | Web console removal | `refactor/web-console-removal` | |
| 6 | Release 0.6.0 | `release/0.6.0` | |

### Task 1 — chore/hologram-plan

This record, and its row in the memory index.

### Task 2 — build/dev-clients

`./gradlew :platforms:{fabric,neoforge,forge}:runClient` each start a development client with
core loaded. Fabric needed nothing: Loom gives every Fabric module a client run. NeoForge got
a run and loads the bundled modules as part of `mcagents_core` through the mod's source sets.
Forge got a run that starts from `build/devMod/`, one directory holding every bundled module,
because Forge treats the directory holding `mods.toml` as the whole mod; its class path swap
captures the object factory up front so the configuration cache keeps working, where calling
`files(...)` inside the task action failed it. All three reached the title screen on
2026-10-13 with `mcagents_core 0.5.3` loaded. `wiki/environments/setup.md` documents the
commands.

### Task 3 — feat/hologram

The new module `platforms/client` compiles against vanilla Minecraft through Loom, publishes
`mcagents-core-client`, and is bundled into each loader jar beside `api`, `common` and
`platforms/shared`. A mod describes a hologram with the records in `api.hologram`
(`Hologram`, the sealed `HologramPanel`, `ButtonGrid`, `HologramButton`) and opens it with
`Holograms`; each loader's client half connects its tick, drawing, attack and wheel events to
`HologramClient`. The arithmetic that places a hologram, finds what the crosshair is on, lays
out the grid and applies the two-chunk rule is in `common.hologram`, so it is tested without
a game, the owner's chunk 1 to chunk 4 example among the tests.

On Forge the hologram is drawn by a mixin on `LevelRenderer.submitFeatures`. The jar names
the mixin configuration in its `MixinConfigs` manifest attribute; the dev client, which runs
from a directory with no manifest, passes it as `--mixin.config`.

Checked by hand on 2026-10-13 in each loader's dev client, with a scratch harness, never
committed, that opened an 18-button demo hologram. On all three the panel matched the
picture; a left click ran the button's action without hitting the block behind; the wheel
scrolled rows without changing the hotbar slot; holding Z carried the hologram and letting
go left it; the X closed it; and, placed in chunk -33, it stayed while the player stood in
-32 and -31 and closed at -30. Forge's built jar was then run alone from `run/mods/`, without
`--mixin.config` or the development classes, and drew the hologram, so the manifest
attribute loads the mixin in a real install.

`wiki/api/hologram.md` and `wiki/api/hologram-panels.md` are new and routed from the API
index; `dependency.md`, `mod-runtime.md`, `modules.md`, `mod-sides.md`, the overview, the
README, the setup page, the Publish workflow's header, the repository map and the state file
name the seventh module. No dedicated server was started. `.agents/rules/repository.md` and
`.agents/knowledge/minecraft-platform.md` still describe six modules and are proposed to the
owner rather than edited.

### Task 4 — feat/hologram-panels

Three more panel kinds join the button grid in `api.hologram`: `SelectList` (rows to pick
from, the chosen one highlighted, up to three buttons under it), `Transcript` (entries of
`TranscriptEntry`, each a `<speaker>` line and its text wrapped under it) and `PromptInput` (a
box to type into and a send button). They are tall panels, 160 by 230 pixels, drawn with a
title band and a sunken box with its own scrollbar, so three stand side by side as the chat
picture shows. Each reads the calling mod's suppliers every frame, so core holds none of the
conversation; what it holds is the text being typed, in memory, until the hologram closes.

Clicking a prompt's box opens `TypingScreen`, a screen that draws nothing over the world but
one line at the top, and takes the keys: Enter sends and stops typing, Shift+Enter starts a
new line, Escape or a click stops typing and keeps the text. Opening the chat hologram showed
the arc too wide at arm's length, so a hologram now opens at least three quarters of its
width away, where it was 0.62; the menu, 2.5 blocks wide, still opens 2.4 blocks away.

The arithmetic is in `common.hologram`, tested: `ScrollWindow` (a window over rows or lines
that can follow the end, and its scrollbar), `TextWrap` and `TextLine` (wrapping at spaces,
cutting long words, keeping line breaks), `TextBuffer` (the caret, editing, cleaning pasted
text and the length limit, never half a character) and `ColumnLayout` with `PanelArea` (where
the box, rows, scrollbar and bottom buttons are, so a click lands where it is drawn).

Checked by hand on 2026-10-13 with the scratch harness, which added a chat demo: on Fabric, the
menu's Chat button replaced the menu with the chat hologram; a row click chose the row; the
wheel scrolled the list; a long row ended in dots; typing, Shift+Enter, Enter and the send
button sent the text and emptied the box; a send while not ready did nothing and Escape kept
the text; the transcript followed new entries and stayed put once scrolled up; Z carried the
three panels together; and the X closed them. On NeoForge and Forge the chat hologram drew
and a typed prompt was sent.

`wiki/api/hologram-panels.md` documents the four kinds and the typing keys. The plan's task 3
listed a guide page too; none was written, because the two API pages open with the examples a
guide would carry.

### Task 5 — refactor/web-console-removal

The web console is gone: `api.web`, `common.web`, the page shell under
`mcagents_core/web/`, their tests, `CoreRuntime.stop()` closing the console, and the pages
`wiki/api/web-console.md` and `wiki/guides/web-pages.md`. Nothing in core listens on a port.
The loader manifests describe core as the model client and the holograms, and the docs that
named the console no longer do; the 0.5.0 change log and earlier records keep it as history.

MCAgents chat 0.3.0, already published, declares `mcagents_core >= 0.5.1` and opens the
console from its console button, so with core 0.6.0 it would fail at that button. Core 0.6.0
therefore declares chat (mod id `mcagents`) below 0.4.0 incompatible: `breaks` on Fabric and
an `incompatible` dependency with a reason on NeoForge, the two loaders chat ships for.
Checked on 2026-10-13 with a stand-in `mcagents` 0.3.0 jar in each dev client's `run/mods/`:
Fabric stopped with "MCAgents Core is incompatible with any version before 0.4.0 of mod
mcagents", NeoForge with the reason text, and NeoForge without the stand-in loaded a world.
`wiki/api/dependency.md` says so under *In game*.
