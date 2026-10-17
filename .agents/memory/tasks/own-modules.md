---
name: memory-tasks-own-modules
description: Making each loader module declare core's own submodules with implementation, keeping the loader and Fabric API compileOnly, without changing a jar, and releasing 0.13.1.
---

# Task: loader modules implement their own submodules

**Status:** in progress

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
| 1 | Task record | `chore/own-modules-plan` | |
| 2 | implementation for own modules | `build/own-modules` | |
| 3 | Release 0.13.1 | `release/0.13.1` | |

### Task 1 — chore/own-modules-plan

This record, and its row in the memory index.
