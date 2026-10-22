---
name: memory-tasks-config-paths
description: Moving every file core keeps on disk into MCAgents/core/ — settings, the global model and the credential file — moving a player's old files there on first start, and releasing 0.14.0.
---

# Task: core's files in MCAgents/core/

**Status:** in progress

## The request

On 2026-10-22 the owner asked: "Make mods use this path for configs files.
`%{User}%/path/to/.minecraft/MCAgents/{mod}/`", with `.minecraft/MCAgents/core/` and
`.minecraft/MCAgents/chat/` as the examples, and for the template to produce it by default so
it never has to be asked for again. Chat and the template get the same change; this record is
core's part. The owner asked for the work to start right after planning, for open questions to
be settled by picking the recommended option, and for every branch to be opened as a pull
request and rebase-merged in order.

## What master looks like (0.13.2)

- `AgentsPaths` resolves `{game_dir}/MCAgents/agents/` — named for the agents mod that merged
  into core — holding `settings.properties` and `models.properties`.
- The client credential file is `mcagents.json` loose in the Minecraft directory
  (`MinecraftDirectory`); a dedicated server's is `{run_dir}/config/mcagents.json`
  (`ServerRunDirectory`).

## Decisions

- **Every file core owns lives in `{game_dir}/MCAgents/core/`**: `settings.properties`,
  `models.properties` and `mcagents.json`. `MCAGENTS_DIR` keeps naming the Minecraft directory,
  so the file is `$MCAGENTS_DIR/MCAgents/core/mcagents.json`.
- **A dedicated server follows the same rule** under its run directory:
  `{run_dir}/MCAgents/core/mcagents.json`, instead of `config/`.
- **A player's old files are moved on first start**, only when the new file does not exist yet;
  when both exist the new one wins and the old one is left where it is. The empty `agents/`
  folder is removed.
- **The move is a public helper in `common`**, `ModFiles.moveIfAbsent`, so chat and later mods
  move their own files the same way. A mod that reads the global model before core's start-up
  calls `AgentsPaths.moveLegacyFiles()` first.
- **A minor release, 0.14.0:** files move and `AgentsPaths.AGENTS_DIRECTORY` becomes
  `MOD_DIRECTORY`; no file format changes.

## Tasks

| # | Title | Branch | PR |
|---|---|---|---|
| 1 | Task record | `chore/config-paths-plan` | |
| 2 | Core's files in MCAgents/core/ | `fix/config-paths` | |
| 3 | Release 0.14.0 | `release/0.14.0` | |

Chat (`fix/config-paths`, released as 0.15.0) and the template (`fix/config-paths`, staying
0.0.0) follow in that order.

### Task 1 — chore/config-paths-plan

This record, and its row in the memory index.

### Task 2 — fix/config-paths

`AgentsPaths` resolves `MCAgents/core/` (`MOD_DIRECTORY`; `AGENTS_DIRECTORY` is gone, and
`LEGACY_DIRECTORY` names `agents`), and `moveLegacyFiles()` moves `settings.properties` and
`models.properties` across, which `AgentsRuntime.start` calls before loading either. The new
`common.file.ModFiles` does the moving for any mod. `MinecraftDirectory.resolveFile` and
`ServerRunDirectory.resolveFile` both end in `MCAgents/core/mcagents.json`; their `legacyFile`
names the old place, and `ClientEntrypoint` and `ServerEntrypoint` move the file with
`SharedTokenStore.moveLegacyFile` on start. `MinecraftDirectory.isUsable` now accepts a
directory whose nearest existing parent is writable, since `MCAgents/core/` is two levels
deep. The rule is in `.agents/rules/repository.md`; `wiki/api` and `mod-sides.md` name the
new paths. Chat must call `AgentsPaths.moveLegacyFiles()` before its own `Models.start`.
