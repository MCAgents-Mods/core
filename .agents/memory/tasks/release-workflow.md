---
name: memory-tasks-release-workflow
description: Adding the Release workflow, which attaches the Forge, Fabric and NeoForge mod jars to the GitHub release of each v{version} tag, and releasing 0.6.1 with it.
---

# Task: Release workflow

**Status:** done once the pull requests below merge and the Release run makes `v0.6.1`

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
| 1 | Task record | `chore/release-workflow-plan` | #29 |
| 2 | Release workflow | `ci/release` | #30 |
| 3 | Release 0.6.1 | `release/0.6.1` | #31 |

### Task 1 — chore/release-workflow-plan

This record, and its row in the memory index.

### Task 2 — ci/release

`.github/workflows/release.yml`, as described above: job `build` (contents read) finds the
tag and its release, checks out the tag, checks its `project-version`, runs
`./gradlew assemble`, takes `MCAgentsCore{Forge,Fabric,Neoforge}-{version}.jar` from
`platforms/{forge,fabric,neoforge}/build/libs/` and uploads them as `mod-jars`; job `release`
(contents and actions write) writes the notes, creates the release with
`gh release create --verify-tag`, marks it latest only when it is the version on `master`, and
deletes the artifact. Its concurrency group, `github-release`, is not Publish's `release`, so
a queued Release run never cancels a queued Publish run. `publish.yml` is unchanged.

The setup page gains "How the jars reach the Releases page"; the overview, the repository map,
the repository rule and the state file now name two workflows. The rule's sentence "The only
workflow is `.github/workflows/publish.yml`" had become false. It is corrected here, as was
done when Publish landed, and listed as a finding for the owner to keep or revert.

Checked locally: `actionlint` 1.7.12 with shellcheck passes on both workflows. Each step's
script was run from the workflow file: the tag check against the real remote gives
`v0.6.0`, `latest=true`, `release=true` with no input, `latest=false` for `v0.5.2`, a notice
for a missing tag, and an error for `0.6.0`; the notes step turns the 0.6.0 changelog's
`../../../../api/…` links into `https://github.com/MCAgents-Mods/core/blob/v0.6.0/wiki/api/…`
and falls back to generated notes when a version has no changelog; the jar step takes one
jar per loader, skips the sources jars, and fails on a missing jar, two jars, or a tag whose
`project-version` differs. The first real run is the Release run after this branch merges,
which should make the `v0.6.0` release.

### Task 3 — release/0.6.1

`project-version` is `0.6.1`, a patch: the workflow changes no jar. The 0.6.1 changelog and its
logs-index row are new; the overview, the repository map, the repository rule and the state
file name 0.6.1, and the rule's `wiki/logs/` range runs to `0/6/1`. Merging this branch is the
release: Publish finds no `v0.6.1` tag, builds, publishes and tags the commit, and Release then
makes the `v0.6.1` release with the three jars.

The first real run came from merging #30: Publish found `v0.6.0` and stopped, and Release
(run 38405107504) built the tagged commit and created the `v0.6.0` release, marked latest,
with `MCAgentsCoreForge-0.6.0.jar`, `MCAgentsCoreFabric-0.6.0.jar` and
`MCAgentsCoreNeoforge-0.6.0.jar` attached and the 0.6.0 changelog as its notes, its links
pointing at the tagged files. The run's `mod-jars` artifact was deleted; the run lists none.
