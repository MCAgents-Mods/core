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
`io.github.mcagentsmods`, version `0.6.1` in `gradle.properties`. It holds the shared
agent and API code that MCAgents' Minecraft mods build on.

Full description: [`../../../wiki/information/overview.md`](../../../wiki/information/overview.md).

## What lives where

| Path | What it is |
|---|---|
| `api/` | Pure Java API contracts — chat, llm, token, model, http and hologram packages. Depends on nothing in the repo. |
| `common/` | The implementation: `MCAgentsProvider`, the vendor clients, credential pooling, the routed `OpenRouterClient` (`common.model`), `JdkHttpTransport` (`common.http`), and the hologram's placement, layout, scrolling and text arithmetic (`common.hologram`). |
| `common/src/testFixtures/` | `StubTransport` and `StubKeySource`, published for consumers' tests. |
| `platforms/shared/` | What every loader shares: `CoreRuntime`, the side-guard machinery, and the client and server halves as packages. Plain Java. |
| `platforms/client/` | The holograms: drawing, aiming, clicks, the wheel, the Z key and the two-chunk range, against vanilla Minecraft through Loom, and the `en_us` lang file for the key and the footer. Client only; no loader type. |
| `platforms/{forge,neoforge,fabric}/` | One entry point and one manifest per loader, a client half that wires the hologram to the loader's events, and Forge's drawing mixin; each with its own toolchain and its own mod jar. |
| `platforms/{client,forge,neoforge,fabric}/{minecraft}/` | One Gradle subproject per Minecraft version, `:platforms:{module}-{minecraft}`: a `build.gradle` that applies the toolchain and `../{module}.gradle`, and a `src/` with only what differs for that version. The shared sources are in `platforms/{module}/src/`. |
| `platforms/minecraft.gradle` | What every version subproject shares: its version from the folder name, the catalog lookups by version suffix, the two source folders, and the `+{minecraft}` jar name. |
| `gradle/libs.versions.toml` | Every dependency coordinate. Never declare one inline in a module. |
| `.agents/` | This repository's own instructions, plus the index, agent-wiki, and memory trees. The universal conventions are **not** here — they come from the connector. |
| `wiki/` | Human documentation, including `wiki/api/` (one page per public API scope) and `wiki/logs/` release history. |
| `.github/workflows/publish.yml` | The Publish workflow: on a push to `master` whose `project-version` has no `v{version}` tag yet, build, test, publish every module, then tag. |
| `.github/workflows/release.yml` | The Release workflow: after each Publish run on `master`, if `v{version}` has no GitHub release yet, build the Forge, Fabric and NeoForge jars from the tag and attach them to a new release, then delete the run's copy of the jars. |

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
| `CoreRuntime` | `platforms/shared/…/shared/` | What each loader's entry point calls to start and stop core. |
| `SideEntrypoint` / `ModBootstrap` | `platforms/shared/…/shared/environment/` | Mod-side bootstrap and the client/server split. |
| `Holograms` | `platforms/client/…/client/hologram/` | What a mod calls to open and close a hologram. |
| `HologramClient` | `platforms/client/…/client/hologram/` | What each loader's client half connects its tick, render, attack and scroll events to. |

Public API surface, one page per scope, method by method: `wiki/api/`, routed by
[`../../index/project-wiki-api-index.md`](../../index/project-wiki-api-index.md).

## Commands that actually work here

```sh
./gradlew build                          # every module, plus tests
./gradlew test                           # tests only
./gradlew :common:test                   # one module
./gradlew test --tests '*TokenHandles*'  # one class
./gradlew publishToMavenLocal            # a release dry run
./gradlew :platforms:fabric-26.2:runClient    # a dev client (also neoforge, forge)
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

* **Nothing checks a pull request.** Both workflows, Publish and Release, run only on
  `master`, after a merge, so run `./gradlew test` before opening a pull request. Do not describe a pull request check that does not exist.
* **Merging a new `project-version` publishes it.** The Publish workflow tags and
  ships the version the moment it reaches `master`, and a published version can never
  be overwritten — the version bump belongs in the release task alone.
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
* **The Forge jar needs `pack.mcmeta`.** Forge 26.2 refuses the resources of a mod jar
  without one, shows a warning at start-up, and crashes when a world is created; NeoForge
  and Fabric build the metadata themselves. Its `min_format` and `max_format` copy Forge's
  own `pack.mcmeta` and change with the Minecraft version, so each version keeps its own
  in `platforms/forge/{minecraft}/src/main/resources/`, copied from that Forge build.
* **Each Minecraft version is a subproject, not a switch.** A Minecraft-facing module is
  built once per version in `settings.gradle`'s list, from shared sources plus that
  version's folder. Code that differs between versions goes in a class of the same name
  in every version folder; shared code never checks the version. Adding a version is a
  catalog block, a list entry, and a folder per module.
* **The client and server halves share a module.** Nothing outside
  `…shared.client` may import it, and the same for `…shared.server`;
  `SideSeparationTest` fails the build otherwise.
* **`platforms/client` is reached only from a loader's client half.** Naming it from
  a loader's common entry point links Minecraft's client classes on a dedicated
  server. Its testable logic belongs in `common.hologram`, which needs no game.
* **Forge has no level-render event in 26.2.** The hologram is drawn by a mixin on
  `LevelRenderer.submitFeatures`, which the jar names in its `MixinConfigs` manifest
  attribute and the dev client passes as `--mixin.config`. Renaming the mixin
  configuration means changing both.
* **A dev client is the only way to see a hologram.** `runClient` per loader and
  Minecraft version; nothing in the tests draws one.
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
