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
