---
name: memory-tasks-compile-only
description: Replacing every implementation dependency in the loader modules with compileOnly plus a dev-client runtimeOnly twin, without changing a jar, and releasing 0.11.1.
---

# Task: compileOnly instead of implementation

**Status:** in progress

## The request

On 2026-10-17 the owner asked: "Did you use implement in each mods repo? if yes then use
compileOnly instead […] Check first if true then plan and work immediately." The plan
(`plans/compile-only.md` in the project's shared files) covers core, chat and template, and
the owner had already asked for every branch to be opened as a pull request and rebase-merged
in order.

## What the check found

- Core's published modules (`tool`, `api`, `common`, `shared`, `client`) declare no
  `implementation`: `api project(':tool')` is a compile dependency between two published
  modules, and everything else is `compileOnly`.
- The loader modules (`platforms/{fabric,forge,neoforge}`) used `implementation` for core's own
  modules, which the jar task merges by their outputs rather than through the configuration,
  and for Fabric Loader, Fabric API and Forge, which the game provides.
- No MCAgents mod used `implementation` for core: chat and template compile against it with
  `compileOnlyApi` and `compileOnly`.

## Decisions

- **Every loader-module `implementation` becomes `compileOnly` with a `runtimeOnly` twin.** The
  dev client runs from that class path, and plain `compileOnly` would leave it without Fabric
  Loader, Fabric API and core's own classes. The loader modules are never published, so
  `runtimeOnly` reaches the dev client and nothing else.
- **The jars do not change.** Every entry of the three loader jars is compared before and after.
- **A patch release, 0.11.1:** a build change, no API change.

## Tasks

| # | Title | Branch | PR |
|---|---|---|---|
| 1 | Task record | `chore/compile-only-plan` | |
| 2 | compileOnly in loader modules | `build/compile-only` | |
| 3 | Release 0.11.1 | `release/0.11.1` | |

### Task 1 — chore/compile-only-plan

This record, and its row in the memory index.

### Task 2 — build/compile-only

The Fabric, Forge and NeoForge modules declare `tool`, `api`, `common`, `shared` and `client`
(and, on Fabric, Fabric Loader and Fabric API; on Forge, Forge) as `compileOnly`, each with a
`runtimeOnly` twin for `runClient`, and a comment on each block says why. No Gradle file in
core declares `implementation` any more (`testImplementation` aside). `./gradlew build` is
green, and the three loader jars hold exactly the entries of the v0.11.0 release jars. In the
Fabric, NeoForge and Forge 26.3 dev clients, core loaded beside chat 0.10.0 and the template
into a world, and G then CHAT opened chat's Chat Sessions hologram with its database open.
Screenshots are in the project's shared files under `reports/compile-only/`.
