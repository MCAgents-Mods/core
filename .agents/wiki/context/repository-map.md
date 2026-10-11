---
name: agent-wiki-context-repository-map
description: Orientation before touching MCAgents/core — what lives where, the real build and test commands, entry points, and the gotchas.
---

# Repository Map

Read this before touching anything in `MCAgents/core`. It is the agent-facing
orientation page: **where things are, what to run, and what will bite you.** The
underlying facts are documented once for humans in `wiki/` — this page links there
rather than restating them.

## What this repository is

A **Gradle multi project build on Java 25**, root project `mcagents`, group
`io.github.mcagents`, version `0.4.0` in `gradle.properties`. It holds the shared
agent and API code that MCAgents' Minecraft mods build on.

Full description: [`../../../wiki/information/overview.md`](../../../wiki/information/overview.md).

## What lives where

| Path | What it is |
|---|---|
| `api/` | Pure Java API contracts — chat, llm, token packages. Depends on nothing in the repo. |
| `common/` | The implementation: `MCAgentsProvider`, the vendor clients, credential pooling. |
| `platforms/mods/core/` | What every mod module shares, including the side-guard machinery. |
| `platforms/mods/{client,server}/` | The two physical sides, which the loaders never mix. |
| `platforms/mods/{neoforge,fabric}/` | Loader entry points. Plain Java modules today — no loader toolchain yet. |
| `gradle/libs.versions.toml` | Every dependency coordinate. Never declare one inline in a module. |
| `.agents/` | This repository's own instructions, plus the index, agent-wiki, and memory trees. The universal conventions are **not** here — they come from the connector. |
| `wiki/` | Human documentation, including `wiki/logs/` release history. |

## Where the conventions come from

Branching, commits, pull requests, task workflow, directory placement, versioning,
memory policy, the discovery protocol, and the creators are served by the
**`lxagents-agents-base`** MCP connector, not stored here. Read
`agents://manifest.json` once to see what exists, then
`agents://index/root-index.md` to route. `.agents/` holds only what is specific to
`MCAgents/core` — see [`../../rules/repository.md`](../../rules/repository.md).

The module graph, the dependency rules, and the published coordinates are documented
once, in
[`../../../wiki/information/modules.md`](../../../wiki/information/modules.md). Read
that page rather than inferring the layout from the directory listing.

## Entry points

| Entry point | Where | Notes |
|---|---|---|
| `MCAgentsProvider` | `common/…/common/MCAgentsProvider.java` | The single public entry point, and the only class a consumer needs to read. |
| `SideEntrypoint` / `ModBootstrap` | `platforms/mods/core/…/mods/environment/` | Mod-side bootstrap and the client/server split. |

Public API surface, method by method:
[`../../../wiki/reference/api.md`](../../../wiki/reference/api.md).

## Commands that actually work here

```sh
./gradlew build                          # every module, plus tests
./gradlew test                           # tests only
./gradlew :common:test                   # one module
./gradlew test --tests '*TokenHandles*'  # one class
./gradlew publishToMavenLocal            # a release dry run
./gradlew clean
```

Never write a command into any file in this repository that you have not seen work
here. Full setup, publishing, and environment variables:
[`../../../wiki/environments/setup.md`](../../../wiki/environments/setup.md).

## Generated paths — leave them alone

* `build/` and `{module}/build/` — every artifact, report, and test result.
* `{module}/build/libs/` — the per-module jars.
* `{module}/build/reports/tests/test/index.html` — the HTML test report.
* `.gradle/` — Gradle's own state.

None of these are tracked. Do not commit them, and do not document a path under
`build/` as though it were source.

## Gotchas

* **There is no CI pipeline.** `./gradlew test` is the only thing that runs the
  tests, so run it before opening a pull request. Do not write instructions or docs
  that assume a CI workflow exists.
* **Never block the game thread.** A future completes on whichever thread finished
  the work, and a callback that touches game state must hop back onto the client or
  server thread first. See
  [`../../knowledge/minecraft-platform.md`](../../knowledge/minecraft-platform.md) —
  that file is the authority, and it is a rule, not a note.
* **`compileOnly` does not reach tests.** Modules declare `api` and `common` as
  `compileOnly`, so a module with tests must repeat them as `testImplementation` in
  its own build file.
* **The mod loaders have no coordinate yet.** `neoforge` and `fabric` are plain Java
  modules until a real loader toolchain lands. Do not add one speculatively.
* **Three constraints are load-bearing** and a change that breaks one is wrong
  however well it works: `core` is **API only** (no commands, no listeners, no
  gameplay), holds **no memory** (no conversation or per-player state), and
  **nothing blocks** (every remote call returns a `CompletableFuture`). See
  [`../../rules/repository.md`](../../rules/repository.md).
* **The `platforms/*` modules that are empty are scope guidance**, not a description
  of code that exists.

## Before you write a file

Placement is not a judgment call — run the algorithm in
`{shared}/rules/directories.md`. Four trees, and the tree
is not negotiable:

* rules → `.agents/{folder}/{file}.md` (gated)
* indexes → `.agents/index/{scope}-index.md` (**never an `INDEX.md`**)
* knowledge → `.agents/wiki/{type}/` for agents, `wiki/{folder}/` for humans
* state → `.agents/memory/{type}/` (ungated)

Current repository state, and what is in flight:
[`../../memory/state/repository-state.md`](../../memory/state/repository-state.md).
