---
name: memory-tasks-flat-platforms
description: Giving every platform module one build.gradle and one src/ at its root, removing the 26.3/ version folders and the {module}.gradle scripts, and releasing 0.13.2.
---

# Task: one build file per platform module

**Status:** in progress

## The request

On 2026-10-18 the owner asked to "consolidate the build logic into a single `build.gradle` file
at the root of each platform module", move version-specific sources up to the module's own
`src/`, delete the `{loader}.gradle` files and the `26.3/` subdirectories, and point
`settings.gradle` at the module roots. Chat and template get the same change at the same time;
the plan (`plans/flat-platforms-core.md` in the project's shared files) covers core. The owner
had already asked for every branch to be opened as a pull request and rebase-merged in order.

## What master looks like

- Since 0.8.0 ([`latest-minecraft.md`](latest-minecraft.md)) each Minecraft-facing module is a
  folder of shared sources with one version subproject, `platforms/{module}/26.3/`, whose
  `build.gradle` applies `../{module}.gradle` and through it `../../minecraft.gradle`.
- `minecraft.gradle` reads the Minecraft version from the folder name and finds the catalog's
  entries by a `-26-3` suffix; `settings.gradle` names the version once.

## Decisions

- **`platforms/minecraft.gradle` goes too.** Its few remaining lines (the version, the next
  version for the loader metadata, the `+{minecraft}` jar name) are written into each module's
  `build.gradle`, so a module's whole build is in one file, as in chat and template.
- **The catalog names the Minecraft version**, in its `minecraft` entry; the `-26-3` suffixes go,
  since only one version is ever built. It is the one place a new release changes.
- **`platforms/client` is flattened too**, as a platform module like the others.
- **Project paths, artifact ids and jar names do not change**, and the jars keep their entries.
- **A patch release, 0.13.2:** a build change, no API change.

## Tasks

| # | Title | Branch | PR |
|---|---|---|---|
| 1 | Task record | `chore/flat-platforms-plan` | |
| 2 | One build file per platform module | `build/flat-platforms` | |
| 3 | Release 0.13.2 | `release/0.13.2` | |

### Task 1 — chore/flat-platforms-plan

This record, and its row in the memory index.

### Task 2 — build/flat-platforms

Every version folder's sources move up to `platforms/{module}/src`, each `{module}.gradle` becomes
the module's own `build.gradle` with `minecraft.gradle`'s lines written in, the catalog loses its version suffixes, `settings.gradle`
includes the module roots, and the Release workflow takes one jar from each loader module.

### Task 3 — release/0.13.2

`project-version` is `0.13.2`, with its changelog and logs-index row.
