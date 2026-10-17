---
name: memory-tasks-own-modules
description: Making each loader module declare core's own submodules with implementation, keeping the loader and Fabric API compileOnly, without changing a jar, and releasing 0.13.1.
---

# Task: loader modules implement their own submodules

**Status:** done

## The request

On 2026-10-17 the owner asked: "Make each mod in the `platforms/` directory implement its own
submodules (e.g., api, common, client, tools). […] if a mod uses an external dependency (e.g.,
Fabric API), use `compileOnly` for that as well, since it will be provided at runtime." The plan
(`plans/own-modules-implementation.md` in the project's shared files) covers core, chat and
template, and the owner had already asked for every branch to be opened as a pull request and
rebase-merged in order.

## What master looks like

- Since 0.11.1 ([`compile-only.md`](compile-only.md)) the Fabric, Forge and NeoForge modules
  declare `tool`, `api`, `common`, `shared` and `client` as `compileOnly` with a `runtimeOnly`
  twin, and Fabric Loader, Fabric API and Forge the same way.
- The jar task merges core's own modules by their outputs; no configuration decides what a jar
  carries.

## Decisions

- **Core's own modules become `implementation`.** It is what the two lines did together: the
  loader modules are never published, so the scope only reaches the compile class path and the
  dev client's.
- **Fabric Loader, Fabric API and Forge stay `compileOnly`**, keeping the `runtimeOnly` line
  `runClient` needs: the game provides them, and plain `compileOnly` would start the dev client
  without them.
- **The jars do not change.** Every entry of the three loader jars is compared with master.
- **A patch release, 0.13.1:** a build change, no API change.

## Tasks

| # | Title | Branch | PR |
|---|---|---|---|
| 1 | Task record | `chore/own-modules-plan` | #67 |
| 2 | implementation for own modules | `build/own-modules` | #68 |
| 3 | Release 0.13.1 | `release/0.13.1` | #69 |

### Task 1 — chore/own-modules-plan

This record, and its row in the memory index.

### Task 2 — build/own-modules

The Fabric, Forge and NeoForge modules declare `tool`, `api`, `common`, `shared` and `client` with
`implementation` and nothing else; Fabric Loader, Fabric API and Forge stay `compileOnly`, with
the `runtimeOnly` line `runClient` needs, and NeoForge stays undeclared because ModDevGradle
provides it. `./gradlew build` is green, and the three loader jars hold exactly the entries, at
the same sizes, of the v0.13.0 release jars.

### Task 3 — release/0.13.1

`project-version` is `0.13.1`, a patch release: a build change, no API change, and jars with the
same entries as 0.13.0's. `wiki/logs/0/13/1/` says players and mod authors need do nothing; the
logs index has its row, and the repository rule, map, state and overview name the version. The
`PR` column is filled.
