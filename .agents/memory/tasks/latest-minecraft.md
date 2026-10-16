---
name: memory-tasks-latest-minecraft
description: Dropping Minecraft 26.2 so core supports only the latest release (26.3), removing the Minecraft suffix from project and artifact ids, and releasing 0.8.0.
---

# Task: only the latest Minecraft version

**Status:** in progress

## The request

On 2026-10-16 the owner wrote: "Make it support only latest minecraft version, right now it is
"26.3". we will not have another version support … but still keep suffix. e.g. "+26.3"", and
then: "Also delete "+26.3" in artifact id too. Since we only support latest version", with
`agents-api`, `core-common`, `agents-tool` and `chat-common` as examples, and this work first,
before the artifact-id and Forge-for-chat work. agents, chat and the template follow in their
own pull requests once core is published. The plan is `plans/latest-minecraft-only.md` in the
project's shared files.

## Decisions

- **Only the latest Minecraft release, permanently.** See
  [`../decisions/minecraft-latest-only.md`](../decisions/minecraft-latest-only.md).
- **The per-version layout stays, with 26.3 as its only entry.** `platforms/{module}/26.3/`
  keeps its `build.gradle`, `RectangleSubmitter`, `AttackListener` and Forge `pack.mcmeta`, so
  moving to the next release renames a folder rather than rewriting the build.
- **Jar names keep the game's version** (`MCAgentsCoreFabric-0.8.0+26.3.jar`); **project and
  artifact ids lose it**: `:platforms:client` publishes `mcagents-core-client`, and the loaders
  `mcagents-core-{fabric,neoforge,forge}`. Dropping the `mcagents-` prefix is the next task
  chain's (artifact ids), not this one's.
- **Release 0.8.0**, a minor: a supported game version and the published ids change.
- Old 26.2 and `-26.3` packages on GitHub Packages and old release jars are left as they are.

## Tasks

| # | Title | Branch | PR |
|---|---|---|---|
| 1 | Task record | `chore/latest-minecraft-plan` | |
| 2 | Only Minecraft 26.3 | `build/latest-minecraft` | |
| 3 | Unversioned project and artifact ids | `build/unversioned-ids` | |
| 4 | Release 0.8.0 | `release/0.8.0` | |

### Task 1 — chore/latest-minecraft-plan

This record, the decision record, and their rows in the memory index.

### Task 2 — build/latest-minecraft

Every `platforms/{module}/26.2/` folder is gone: the four `build.gradle` files, 26.2's
`RectangleSubmitter` and `AttackListener`, and its Forge `pack.mcmeta` (format 107).
`settings.gradle` lists `['26.3']` alone, and the catalog lost its `-26-2` block. The comments
in `settings.gradle`, the catalog, `platforms/minecraft.gradle`, `forge.gradle`, the two
version classes, `build.gradle` and both workflows now describe one version, the latest
release, and how moving to the next one replaces it. `release.yml` needs no logic change: it
already collects `platforms/{loader}/*/build.gradle`, so it attaches three jars. Docs updated:
README, `wiki/information/{modules,overview}.md`, `wiki/environments/setup.md`,
`wiki/api/dependency.md`, the repository map and the state file. The instruction files
`.agents/rules/repository.md` and `.agents/knowledge/minecraft-platform.md` still name 26.2;
they are findings for the owner, not edited here.

### Task 3 — build/unversioned-ids

`settings.gradle` holds one `minecraftVersion` instead of a list and includes each
Minecraft-facing module as `:platforms:{module}`, with its project directory still the version
folder `platforms/{module}/26.3`. So the published ids are `mcagents-core-client` and
`mcagents-core-{fabric,neoforge,forge}`, without `-26.3`, while the jars keep
`+26.3` from `platforms/minecraft.gradle`. The loader builds name the client module
`:platforms:client`; dev clients are `./gradlew :platforms:{loader}:runClient`. Docs updated:
`wiki/api/{dependency,hologram,mod-runtime}.md`, `wiki/environments/setup.md`,
`wiki/information/modules.md`, the repository map, and the `build.gradle` comment.
agents and chat substitute these project paths for the published ids in their composite
builds, so they move to the new names in their own `build/unversioned-ids`.
