---
name: memory-tasks-artifact-ids
description: Dropping the repeated mcagents- from every published artifact id, publishing only the library modules and never the loader jars, and releasing 0.9.0 with it.
---

# Task: shorter artifact ids, library modules only

**Status:** done once the pull requests below merge and Publish puts `core-*` 0.9.0 on GitHub
Packages; agents, chat and the template follow in their own repositories

## The request

On 2026-10-13 the owner wrote, to start after the Minecraft 26.3 work (0.7.0), and on
2026-10-16 moved it after the latest-Minecraft-only work (0.8.0): "make only module 'api,
common, shared, And include module tool in mod agents too' to be publish. Don't publish
platform forge, fabric, neoforge. It seem we have duplicate in artifact id. Update it to be
likes this: group name stay the same. before: mcagents-{id}-{module} … new: {id}-{module}. e.g.
chat-api, chat-common, agents-api, agents-tool". agents, chat and the template follow in their
own pull requests, after core is published. The owner was away and had asked for the
recommended option to be taken on every question. The plan is `plans/artifact-ids.md` in the
project's shared files.

## What changes in core

| Module | Before | After |
|---|---|---|
| `api` | `mcagents-core-api` | `core-api` |
| `common` | `mcagents-core-common` | `core-common` |
| `platforms/shared` | `mcagents-core-shared` | `core-shared` |
| `platforms/client` | `mcagents-core-client` | `core-client` |
| `platforms/{fabric,forge,neoforge}` | `mcagents-core-{loader}` | not published |

The group stays `io.github.mcagentsmods`. Jar names, mod ids, packages and project names do
not change. The loader jars are still built by every build and attached to each GitHub release
by the Release workflow.

## Decisions

- **`client` is published too**, as `core-client`, though the
  owner's list names only api, common and shared: agents and chat compile against it and read
  it from GitHub Packages in their Publish runs, so without it neither could release. It is a
  library, not a loader jar. The other options were to stop publishing it or to fold it into
  `shared`, which may not hold Minecraft types.
- **Only the artifact id rule changes;** core's project names (`:api`, `:platforms:client`)
  stay, since every downstream `settings.gradle` substitutes by those paths.
- **A loader module's `publishing` block is deleted,** so nothing is left that could publish
  it; it still builds its jar for the Release workflow.
- **`core-common-test-fixtures` replaces `mcagents-core-common-test-fixtures`** as the extra
  test-fixture capability; Gradle's own `common-test-fixtures` stays.
- **Old packages are left alone.** Nothing is deleted from GitHub Packages.
- **Release 0.9.0**, a minor: every coordinate a consumer resolves changes.

## Tasks

| # | Title | Branch | PR |
|---|---|---|---|
| 1 | Task record | `chore/artifact-ids-plan` | #40 |
| 2 | Library modules only | `build/publish-scope` | #41 |
| 3 | Shorter artifact ids | `build/artifact-ids` | #42 |
| 4 | Release 0.9.0 | `release/0.9.0` | #43 |

### Task 1 — chore/artifact-ids-plan

This record, and its row in the memory index.

### Task 2 — build/publish-scope

The `publishing` block is gone from `platforms/{fabric,forge,neoforge}/{loader}.gradle`, so
`./gradlew publish` sends only `api`, `common`, `shared` and `client`. The loader subprojects
still build their jars, which the Release workflow picks up from `platforms/{loader}/build/libs/`
unchanged. The Publish header, the root build comment, the dependency, setup and modules pages
and the repository map now say the loader jars go to GitHub releases only. Artifact ids are
still `mcagents-core-*` here; task 3 renames them.

### Task 3 — build/artifact-ids

The `artifactId` rule in the root build is `${rootProject.name}-${project.name}`, so core
publishes `core-api`, `core-common`, `core-shared` and `core-client`, and common's extra
test-fixture capability is `core-common-test-fixtures`. The API, module, setup and guide pages,
`settings.gradle`, the state file and two `package-info` comments name the new coordinates;
the dependency, module and test-fixture pages say what the ids were before 0.9.0. Released
changelogs, the logs index and earlier task records keep the old names, which were true when
written.

### Task 4 — release/0.9.0

`project-version` is `0.9.0`, a minor: every coordinate a consumer resolves changes. The 0.9.0
changelog and its logs-index row are new, and the rule, the repository map, the overview and
the state file say 0.9.0. Merging this branch is the release: Publish finds no `v0.9.0` tag,
publishes the library modules and tags the commit, and Release then makes the `v0.9.0`
release with the three loader jars.
