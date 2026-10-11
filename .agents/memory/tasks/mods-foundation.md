---
name: memory-tasks-mods-foundation
description: Turning core into the foundation for MCAgents-Mods — Bukkit removed, the io.github.mcagentsmods namespace, one jar per mod loader, the shared language model client, and the web page registry.
---

# Task: core becomes the mods foundation

**Status:** in progress — task 1 of 8

## The request

The owner moved this repository into the `MCAgents-Mods` organization, which holds
Minecraft mods only, and asked for four things across `core` and `chat`:

1. Delete every Bukkit project and lay core out as `api/`, `common/` and
   `platforms/{shared, forge, neoforge}`, where `shared` holds the code the loaders share
   and **each loader builds its own jar**. Packages become
   `io.github.mcagentsmods.core.{api, common, shared, forge, neoforge}`.
2. Make `chat` use core's code to talk to OpenRouter, so a later mod does not write the
   same client again.
3. Move the client web console's server out of `chat` into core; `chat` registers its
   pages into the core mod instead, so any mod can add pages.
4. A security review: memory that is never released, exploits, duplicate code.

## Decisions taken with the owner, 2026-10-11

- **Fabric stays.** `chat` is a Fabric-only mod, so core also builds
  `platforms/fabric` — without a Fabric jar `chat` has no core to call or register into.
  The owner chose this over porting `chat` or bundling core inside it.
- **Minecraft 26.2**, the version `chat` already targets. It settles the version
  question `knowledge/minecraft-platform.md` left open.
- **Gradle identity:** group `io.github.mcagentsmods`, root project `core`, published
  artifacts `core-api` and `core-common`, `git-org-name=MCAgents-Mods`.
- **Mod id `mcagents_core`.** `chat` keeps `mcagents`.
- **Version 0.5.0** for this release, approved by the owner.
- **`.agents/plans/` is ignored** from task 1, as `chat` already does; approved with the
  plan.

## Tasks

| # | Title | Scope | Branch | PR |
|---|---|---|---|---|
| 1 | Task record | This file, its index row, the `.agents/plans/` ignore | `chore/mods-foundation-plan` | |
| 2 | Remove Bukkit | `platforms/bukkit/*`, `platforms/engine`, the Spigot, Paper and Folia coordinates, and the docs describing them | `refactor/bukkit-removal` | |
| 3 | New namespace | `io.github.mcagents.core` → `io.github.mcagentsmods.core`, group, root name, org property | `refactor/core-namespace` | |
| 4 | Platform layout | `platforms/{shared,forge,neoforge,fabric}`, loader toolchains, one jar per loader | `refactor/platform-layout` | |
| 5 | Shared language model client | Tool calls, embeddings, prompt-cache hints, retries, provider routes, one HTTP transport | `feat/shared-llm-client` | |
| 6 | Web registry | The loopback web host and `WebRegistry`, the shared page shell | `feat/web-registry` | |
| 7 | Security review | Fixes for confirmed findings | `fix/security-review` | |
| 8 | Release | `0.5.0`, changelog, indexes, closing this record | `release/0.5.0` | |

`chat` carries the other half under the same branch names for tasks 5–7, and its pull
requests merge after this repository's.

### Task 1 — chore/mods-foundation-plan

Created this record and its row in `memory-index.md`, and added `.agents/plans/` to
`.gitignore` so the working plan can never be committed. Baseline before any change:
`./gradlew build` passes on `master` at `f8f7eef`.
