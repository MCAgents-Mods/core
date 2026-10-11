---
name: agent-wiki-context-repository-map
description: Orientation before touching MCAgents-Mods/core — what lives where, the real build and test commands, entry points, and the gotchas.
---

# Repository Map

Read this before touching anything in `MCAgents-Mods/core`. It is the agent-facing
orientation page: **where things are, what to run, and what will bite you.** The
underlying facts are documented once for humans in `wiki/` — this page links there
rather than restating them.

## What this repository is

A **Gradle multi project build on Java 25**, root project `core`, group
`io.github.mcagentsmods`, version `0.4.0` in `gradle.properties`. It holds the shared
agent and API code that MCAgents' Minecraft mods build on.

Full description: [`../../../wiki/information/overview.md`](../../../wiki/information/overview.md).

## What lives where

| Path | What it is |
|---|---|
| `api/` | Pure Java API contracts — chat, llm, token, model and http packages. Depends on nothing in the repo. |
| `common/` | The implementation: `MCAgentsProvider`, the vendor clients, credential pooling, the routed `OpenRouterClient` (`common.model`), `JdkHttpTransport` (`common.http`), and the web console (`common.web`, its shell under `common/src/main/resources/mcagents_core/web/`). |
| `common/src/testFixtures/` | `StubTransport` and `StubKeySource`, published for consumers' tests. |
| `platforms/shared/` | What every loader shares: `CoreRuntime`, the side-guard machinery, and the client and server halves as packages. Plain Java. |
| `platforms/{forge,neoforge,fabric}/` | One entry point and one manifest per loader, each with its own toolchain and its own mod jar. |
| `gradle/libs.versions.toml` | Every dependency coordinate. Never declare one inline in a module. |
| `.agents/` | This repository's own instructions, plus the index, agent-wiki, and memory trees. The universal conventions are **not** here — they come from the connector. |
| `wiki/` | Human documentation, including `wiki/logs/` release history. |

## Where the conventions come from

Branching, commits, pull requests, task workflow, directory placement, versioning,
memory policy, the discovery protocol, and the creators are served by the
**`lxagents-agents-base`** MCP connector, not stored here. Read
`agents://manifest.json` once to see what exists, then
`agents://index/root-index.md` to route. `.agents/` holds only what is specific to
`MCAgents-Mods/core` — see [`../../rules/repository.md`](../../rules/repository.md).

The module graph, the dependency rules, and the published coordinates are documented
once, in
[`../../../wiki/information/modules.md`](../../../wiki/information/modules.md). Read
that page rather than inferring the layout from the directory listing.

## Entry points

| Entry point | Where | Notes |
|---|---|---|
| `MCAgentsProvider` | `common/…/common/MCAgentsProvider.java` | One registered client per vendor. |
| `OpenRouterClient` | `common/…/common/model/OpenRouterClient.java` | The routed client: tools, embeddings, caching, retries, key rotation. What `MCAgents-Mods/chat` calls. |
| `CoreWebRegistry` | `common/…/common/web/CoreWebRegistry.java` | Where mods register web pages; `shared()` is the game's one instance, `open()` starts the console. |
| `CoreRuntime` | `platforms/shared/…/shared/` | What each loader's entry point calls to start and stop core. |
| `SideEntrypoint` / `ModBootstrap` | `platforms/shared/…/shared/environment/` | Mod-side bootstrap and the client/server split. |

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
* **The first build is slow.** NeoForge and Forge decompile Minecraft on the first
  build of a machine; that is minutes, not a hang.
* **The client and server halves share a module.** Nothing outside
  `…shared.client` may import it, and the same for `…shared.server`;
  `SideSeparationTest` fails the build otherwise.
* **Three constraints are load-bearing** and a change that breaks one is wrong
  however well it works: `core` is **API only** (no gameplay, even though it ships
  as a mod jar), holds **no memory** (no conversation or per-player state), and
  **nothing blocks** (every remote call returns a `CompletableFuture`). See
  [`../../rules/repository.md`](../../rules/repository.md).

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
