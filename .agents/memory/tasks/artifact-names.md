---
name: memory-tasks-artifact-names
description: Renaming every core module's jar base name to MCAgentsCore{Module} and its Maven artifact id to mcagents-core-{module}, and publishing all six modules.
---

# Task: artifact names

**Status:** in progress

## The request

The owner asked, on 2026-10-11, for every module's base name and artifact id to follow
one pattern, in this repository and in `chat`:

| Module | Base name | Artifact id |
|---|---|---|
| `api` | `MCAgentsCoreApi` | `mcagents-core-api` |
| `common` | `MCAgentsCoreCommon` | `mcagents-core-common` |
| `platforms/shared` | `MCAgentsCoreShared` | `mcagents-core-shared` |
| `platforms/forge` | `MCAgentsCoreForge` | `mcagents-core-forge` |
| `platforms/fabric` | `MCAgentsCoreFabric` | `mcagents-core-fabric` |
| `platforms/neoforge` | `MCAgentsCoreNeoforge` | `mcagents-core-neoforge` |

## Decisions

- **All six modules publish.** An artifact id was named for each, so `shared` and the
  three loader modules gained a publication. A loader publication is the mod jar alone,
  with no dependency list, because the jar already carries `api`, `common` and `shared`.
- **No version change.** `project-version` stays `0.5.0`; renaming the coordinates is a
  breaking change for a consumer, and the version is the owner's call.

## Tasks

| # | Title | Branch | PR |
|---|---|---|---|
| 1 | Task record | `chore/artifact-names-plan` | |
| 2 | Rename base names and artifact ids | `refactor/artifact-names` | |

`chat` merges after this repository, since it depends on the new coordinates.

### Task 2 — refactor/artifact-names

Each module sets its own `base.archivesName`; the root `build.gradle` sets every
publication's `artifactId` to `mcagents-${rootProject.name}-${project.name}`, and no longer
limits publishing to `api` and `common`. `shared` publishes `components.java`; each loader
publishes `artifact jar`.

The 0.5.0 changelog was corrected rather than left as history: 0.5.0 is neither tagged nor
published, so the coordinates it ships under are the new ones. The modules, setup, API and
provider pages and the state file name the new coordinates.

`./gradlew build publishToMavenLocal` passes into a scratch repository: six POMs, the loader
POMs with no dependencies, and `MCAgentsCore{Module}-0.5.0.jar` in each `build/libs/`.
