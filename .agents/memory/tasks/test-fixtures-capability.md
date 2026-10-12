---
name: memory-tasks-test-fixtures-capability
description: Fixing the capability name on mcagents-core-common's published test fixtures, which 0.5.0 shipped as common-test-fixtures, and releasing 0.5.1 with the fix.
---

# Task: test-fixtures capability

**Status:** done once the pull requests below merge and the Publish run tags `v0.5.1`

## The request

On 2026-10-12, minutes after the Publish workflow shipped 0.5.0 (see
[`publish-workflow.md`](publish-workflow.md)), the `chat` thread reported that a consumer
cannot use core's published test fixtures. `mcagents-core-common-0.5.0.module` gives the
`testFixturesApiElements` and `testFixturesRuntimeElements` variants the capability
`io.github.mcagentsmods:common-test-fixtures`, because Gradle derives it from the project
name `common`, not from the `artifactId` rule. A consumer writing
`testFixtures('io.github.mcagentsmods:mcagents-core-common:0.5.0')`, the form
`wiki/information/modules.md` documents, asks for `mcagents-core-common-test-fixtures` and
fails with "Unable to find a variant … with the requested capability". The composite build
hides it, because there the fixtures come from the project itself.

## Decisions

- **Release 0.5.1.** A published version cannot be overwritten, so the fix ships as a
  patch release. The owner was away and had asked for the recommended option to be
  picked; the alternative was to keep 0.5.0 and have each consumer request
  `common-test-fixtures` by hand.
- **Both capabilities.** The fixture variants keep `common-test-fixtures` and add
  `mcagents-core-common-test-fixtures`, so a consumer that already worked around 0.5.0
  keeps working. The fix is the `chat` thread's verified snippet.

## Tasks

| # | Title | Branch | PR |
|---|---|---|---|
| 1 | Task record | `chore/test-fixtures-capability-plan` | #14 |
| 2 | Name the test fixtures after the artifact | `fix/test-fixtures-capability` | #15 |
| 3 | Release 0.5.1 | `release/0.5.1` | #16 |

### Task 1 — chore/test-fixtures-capability-plan

This record, and its row in the memory index.

### Task 2 — fix/test-fixtures-capability

`common/build.gradle` adds `mcagents-${rootProject.name}-${project.name}-test-fixtures` as an
outgoing capability of both test-fixtures configurations, so the generated module metadata
lists `common-test-fixtures` and `mcagents-core-common-test-fixtures` on each. Checked: a
scratch consumer outside any composite build, depending on
`testFixtures('io.github.mcagentsmods:mcagents-core-common:0.5.0')` from a local repository
that `publishToMavenLocal` filled, compiles a class using `StubTransport` with the fix and
fails to resolve `mcagents-core-common` without it. `wiki/information/modules.md` says the
notation works from 0.5.1.

### Task 3 — release/0.5.1

`project-version` is `0.5.1`, approved as a patch: only published metadata changes. The
0.5.1 changelog and its logs-index row are new; the overview, the repository map, the
repository rule and the state file name 0.5.1, and the rule's `wiki/logs/` range now runs to
`0/5/1` (it had stopped at `0/4/0`). Merging this branch is the release: the Publish workflow
finds no `v0.5.1` tag, publishes the six modules and tags the commit.
