---
name: memory-tasks-release-workflow
description: Adding the Release workflow, which attaches the Forge, Fabric and NeoForge mod jars to the GitHub release of each v{version} tag, and releasing 0.6.1 with it.
---

# Task: Release workflow

**Status:** in progress

## The request

On 2026-10-13 the owner asked, for every MCAgents repository: "Create github workflows ci/cd
name "release.yml". Make it build platforms jar and upload to release with it tag. Also jar
in action must be deleted too. It must have three jar? forge, fabric, neoforge." It has to
land before the Minecraft 26.3 work touches the repository. The owner was away and had asked
for the recommended option to be taken on every question. agents, chat and template get the
same workflow in their own pull requests, after core.

## How the workflow works

- **When it runs.** After every Publish run on `master` (`workflow_run`), and by hand. The
  `v{version}` tag Publish pushes is pushed with the workflow's own token, and such a push
  starts no other workflow, so a tag-push trigger would never fire.
- **What it releases.** The version in `gradle.properties`, once its tag exists and has no
  GitHub release yet. An ordinary merge finds the release already there and stops; a
  release that failed is made by the next merge. By hand, the `tag` input releases an
  earlier tag.
- **The jars.** Built with `./gradlew assemble` from the tagged commit, one
  `platforms/{loader}/build/libs/*-{version}.jar` each for Forge, Fabric and NeoForge. They
  reach the release job as the workflow artifact `mod-jars`, which the release job deletes
  once the release holds them; it is kept one day at most if a run fails before that.
- **The notes.** The version's `wiki/logs/{Major}/{Minor}/{Patch}/CHANGELOG.md` at the tag,
  with its relative links pointed at the tagged files; pull request notes when there is none.

## Decisions

- **Release 0.6.1.** A patch: the workflow changes no jar. It keeps the release task, and the
  merge of `release/0.6.1` proves the whole chain, Publish making the tag and Release making
  its release. The alternative was no version change.
- **`v0.6.0` gets its release too.** The merge of `ci/release` starts Publish, which finds
  `v0.6.0` and stops; Release then finds `v0.6.0` without a release and makes it. Older tags
  stay without one unless released by hand.
- **The jars between the two jobs are deleted,** which is what the owner asked of the jars
  kept in Actions.

## Tasks

| # | Title | Branch | PR |
|---|---|---|---|
| 1 | Task record | `chore/release-workflow-plan` | |
| 2 | Release workflow | `ci/release` | |
| 3 | Release 0.6.1 | `release/0.6.1` | |

### Task 1 — chore/release-workflow-plan

This record, and its row in the memory index.
