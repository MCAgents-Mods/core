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

### Task 2 — refactor/bukkit-removal

Deleted `platforms/bukkit/{core,spigotmc,papermc,foliamc}` and `platforms/engine` —
the Bukkit plugins, `YamlTokenStore`, `CoreCommand`, every `plugin.yml` and
`config.yml`, and the shaded universal jar — along with the Spigot, Paper and Folia
coordinates. The per-family group qualification in the root `build.gradle` went with
them: it existed only because two families each had a module named `core`.

Docs and instructions now describe a mods-only repository: `README.md`, `AGENTS.md`,
the overview, modules, setup, mod-sides, provider-guide and API pages, the repository
map, `rules/repository.md`, `knowledge/minecraft-platform.md` (Bukkit and Folia
removed; threading restated for the client and server threads), and the state file.
The request timeout section now names `LlmCredentials.DEFAULT_TIMEOUT`, the only
timeout left — `request_timeout_seconds` was read by the deleted Bukkit config only.
Released changelogs under `wiki/logs/` are history and were not touched.

`./gradlew build` passes with seven modules.

### Task 3 — refactor/core-namespace

Every package moved from `io.github.mcagents.core` to `io.github.mcagentsmods.core`,
directories with them. Gradle identity: `project-group=io.github.mcagentsmods`,
`git-org-name=MCAgents-Mods`, root project `core`, so the jars and the published
artifacts are `core-api` and `core-common`. The POM's project URL now points at the
GitHub repository instead of the old website.

Runtime names a player can see were deliberately left alone: the shared credential file
`mcagents.json` and the `mcagents.side` system property. Released changelogs and older
task records keep the old names as history. The mod packages still sit under
`…core.mods`; task 4 moves them into `…core.shared`.

### Task 4 — refactor/platform-layout

`platforms/mods/{core,client,server}` became one module, `platforms/shared`, package
`…core.shared`, with the two physical sides as the `…shared.client` and
`…shared.server` packages. The guarantee the old module boundary gave — neither half
links the other — is now `SideSeparationTest`, which reads the module's sources and
fails if a side imports the other or if shared code imports either. `ModBootstrap`
gained a package-private `load(side, className)` so the missing-half failure is still
tested now that both halves are always on the test classpath.

`CoreRuntime` in `platforms/shared` is the one start and stop every loader calls, so the
three entry points stay a few lines each.

Three loader modules, each with its own toolchain and its own jar carrying `api`,
`common` and `platforms/shared`:

| Module | Toolchain | Loader | Entry point | Shutdown hook |
|---|---|---|---|---|
| `platforms/forge` | ForgeGradle 7.0.40 | Forge 26.2-65.1.3 | `CoreForgeMod` | `GameShuttingDownEvent` |
| `platforms/neoforge` | ModDevGradle 2.0.148 | NeoForge 26.2.0.90 | `CoreNeoForgeMod` | `GameShuttingDownEvent` |
| `platforms/fabric` | Loom 1.17.19 | Fabric 0.19.3, API 0.157.0+26.2 | `CoreFabricMod` (server), `CoreFabricClient` (client) | `SERVER_STOPPED`, `CLIENT_STOPPING` |

All three declare mod id `mcagents_core` and Minecraft `[26.2,26.3)`. Minecraft 26.2 is
recorded in `knowledge/minecraft-platform.md`; the "no decided version" note is gone.
`run/` and `run-data/` are ignored for the loaders' dev runs.

**Not proven:** none of the three jars has been loaded in a real game. They compile
against each loader's real API and their manifests name the classes that exist, nothing
more. The `/agents` client commands are still not wired into a command tree.

`./gradlew build` passes: 82 tests, all three mod jars produced.
