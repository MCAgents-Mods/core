---
name: memory-tasks-publish-workflow
description: Adding the Publish GitHub Actions workflow, which tests, publishes and tags a release when a new project-version lands on master, and shipping 0.5.0 through it.
---

# Task: Publish workflow

**Status:** in progress

## The request

The owner asked, on 2026-10-12, for a `.github/workflows/publish.yml` in this repository,
in `chat` and in `template`, from a YAML he supplied: on every push to `master` (and by
hand), read `project-version` from `gradle.properties`, stop if the `v{version}` tag already
exists on the remote, and otherwise build and test, publish to GitHub Packages, and only
then create and push the tag. Every task list must end in a `release/{version}` branch, and
the pull requests are rebase-merged in order. The owner was away and asked for questions to
be settled by picking the recommended option.

`chat` and `template` get their workflow in their own task lists. Template's copies build
against core's tags, so this repository goes first.

## Decisions

- **The version stays `0.5.0`, and the release branch is `release/0.5.0`.** 0.5.0 was
  approved on 2026-10-11 but never tagged or published, so the workflow's first run is
  what ships it. Bumping to 0.5.1 would leave 0.5.0 a version that never existed as a
  package.
- **`v0.5.0` tags the commit that adds the workflow,** because that is the first push to
  `master` that carries it. The release task after it changes Markdown only, so the jars
  are the same either way.
- **The YAML is kept step for step.** Only its header comment changes, to name the six
  modules this repository publishes. The build needs nothing else: core depends on no
  other MCAgents repository.
- **The "no CI pipeline" sentences are corrected in the workflow task,** including the one
  in `.agents/rules/repository.md`, which that file says changes by proposal. The owner's
  standing instruction was to pick the recommended option; the change is reported as a
  finding too.

## Tasks

| # | Title | Branch | PR |
|---|---|---|---|
| 1 | Task record | `chore/publish-workflow-plan` | |
| 2 | Publish workflow | `ci/publish` | |
| 3 | Release 0.5.0 | `release/0.5.0` | |

### Task 1 — chore/publish-workflow-plan

This record, and its row in the memory index.
