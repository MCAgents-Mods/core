---
name: memory-tasks-code-cleanup
description: Splitting core's two largest mixed-purpose classes, registering the /agents command on dedicated servers behind permission level 4, and releasing 0.15.0.
---

# Task: code cleanup and command permissions

**Status:** in progress

## The request

On 2026-10-30 the owner asked for every mod repository to be cleaned up — optimised code,
one responsibility per file, better structure — and for the commands to be audited, with a
permission level on any command a player should not be able to use. Core goes first because
the template and chat build on it. The owner asked for the work to start right after
planning, for open questions to be settled by picking the recommended option, for the first
branch to be `plan/{slug}` and the last `release/{version}`, and for every branch to be
opened as a pull request, rebase-merged in order and checked in CI.

## What master looks like (0.14.0)

- No loader registers a command. `ModTokenCommands` holds the `/agents` logic and
  `ServerEntrypoint` checks every action against `ServerCommandAuthority` (level 4), but
  nothing reaches either from the game, so a dedicated server owner cannot manage keys in
  game.
- `MCAgentsProvider` (653 lines) is both the vendor facade and the token-rotation retry loop.
- `FormPanelView` (785 lines) is both the form's rows and the drop-down choice list.

## Decisions

- **`/agents` is registered on dedicated servers only**, on Fabric, NeoForge and Forge, with
  `.requires` at permission level 4 so it is hidden from and refused to everyone else, and
  every call still goes through `ServerEntrypoint`'s authority check. No client command: a
  player owns their keys and manages them on the Settings hologram.
- **Classes are split in place**, without moving any public type to a new package, so no mod
  that compiles against core breaks.
- **A minor release, 0.15.0**, for the new server command.

## Tasks

| # | Title | Branch | PR |
|---|---|---|---|
| 1 | Task record | `plan/code-cleanup` | |
| 2 | Token rotation out of the provider | `refactor/token-rotation` | |
| 3 | Choice list out of the form panel | `refactor/form-choices` | |
| 4 | `/agents` on dedicated servers | `feat/server-command` | |
| 5 | Release 0.15.0 | `release/0.15.0` | |

The template (`plan/code-cleanup`, `docs/command-permissions`, `release/0.0.0`) and chat
(`plan/code-cleanup`, `refactor/wallet-http`, `refactor/mcp-watcher`, `release/0.15.1`)
follow in that order.

### Task 1 — plan/code-cleanup

This record, and its row in the memory index.

### Task 2 — refactor/token-rotation

The rotation retry moved out of `MCAgentsProvider` into the package-private
`common/TokenRotation`: `chat` (pool lookup and the early refusal), `attempt` (reject on an
auth failure, rotate on a rate limit, retry while credentials remain), `checkUsable`,
`exhausted` and `unwrap`. `MCAgentsProvider.chat` delegates to it; every public method and
every message is unchanged. The provider is down from 653 to 517 lines.

### Task 3 — refactor/form-choices

The drop-down a form field opens moved out of `FormPanelView` into the package-private
`client/hologram/ChoiceList`: where the list stands, which choice is under a point, its
scrollbar and how it is drawn. `FormPanelView` keeps deciding when the list opens and closes
and what a pick does (`openList`, `closeList`, `pick`). Nothing drawn or clicked changes.
`FormPanelView` is down from 785 to 710 lines.

### Task 4 — feat/server-command

- New module `platforms/server` (`…core.server`), compiled against vanilla Minecraft through
  Loom like `platforms/client`, bundled into each loader jar and not published.
  `AgentsCommand.register(dispatcher)` builds `/agents`, `/agents status`, `/agents reload` and
  `/agents {platform} token add|remove …`.
- Guarded twice: `requires` at `CommandCaller.OWNER_LEVEL` (the source's permission set read as
  0–4), and every call through `ServerEntrypoint`, whose authority refuses the same callers.
  Replies go to the caller alone (`sendSuccess(…, false)`).
- `ServerEntrypoint.removableHandles(caller, platform)` feeds `remove`'s suggestions, empty for
  a caller who may not remove a key; covered by `ServerEntrypointTest`.
- Registered on dedicated servers only: Fabric's `CoreFabricMod` (already server-only) through
  `CommandRegistrationCallback`, NeoForge's `CoreNeoForgeMod` and Forge's `CoreForgeMod` through
  `RegisterCommandsEvent` when the side is the dedicated server.
- Docs: `wiki/api/token-commands.md`, `wiki/information/modules.md`, `mod-sides.md`, the README
  and the repository map.
