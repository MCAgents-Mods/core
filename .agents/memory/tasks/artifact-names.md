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
