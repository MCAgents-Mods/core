---
name: memory-tasks-forge-pack-metadata
description: Adding pack.mcmeta to core's Forge jar, without which Forge 26.2 refuses the jar's resources and crashes on world creation, and releasing 0.5.2 with it.
---

# Task: Forge pack metadata

**Status:** done once the pull requests below merge and the Publish run tags `v0.5.2`

## The request

On 2026-10-12 the `agents` thread reported that Forge 26.2 refuses the resources of a mod jar
that has no `pack.mcmeta`. The game shows a warning screen at start-up and crashes when a
world is created ("Can not retrieve LootModifierManager until resources have loaded once").
`MCAgentsCoreForge` up to 0.5.1 has no `pack.mcmeta`, so any Forge game with core installed
crashes on a new world. A copy of core's jar with the file below added loaded a world
cleanly:

```json
{"pack":{"description":"MCAgentsCore resources","max_format":107,"min_format":[107,1]}}
```

The template repository has the same gap and is fixed alongside, in its own pull requests.

## What the loaders do

Checked in the loader jars this build compiles against:

- **Forge `26.2-65.1.3`.** `net.minecraftforge.resource.ResourcePackLoader` reads each mod
  file's metadata with vanilla `Pack.readPackMetadata` and raises
  `fml.modloading.brokenresources` when it finds none. Forge's own `pack.mcmeta` declares
  `min_format` `[107, 1]` and `max_format` `107`.
- **NeoForge `26.2.0.90`.** `ResourcePackLoader.readWithOptionalMeta` builds the metadata
  when a mod has no `pack.mcmeta`.
- **Fabric API `0.157.0+26.2`.** `ModPackResourcesUtil.getMetadataPack` does the same for
  Fabric mods.

So only the Forge jar needs the file.

## Decisions

The owner was away and had asked for the recommended option to be taken on every question.

- **Release 0.5.2.** A patch: one resource file is added to one jar, and no API changes. The
  alternative was 0.6.0.
- **Forge only.** A `pack.mcmeta` in the NeoForge or Fabric jar would change nothing and
  would be one more format number to keep in step with the game.
- **The formats are Forge's own.** They change with the Minecraft version, so moving
  `minecraft` in `gradle/libs.versions.toml` means reading them again from that Forge
  build's `pack.mcmeta`.

## Tasks

| # | Title | Branch | PR |
|---|---|---|---|
| 1 | Task record | `chore/forge-pack-metadata-plan` | #17 |
| 2 | Add pack.mcmeta to the Forge jar | `fix/forge-pack-metadata` | #18 |
| 3 | Release 0.5.2 | `release/0.5.2` | #19 |

### Task 1 — chore/forge-pack-metadata-plan

This record, and its row in the memory index.

### Task 2 — fix/forge-pack-metadata

`platforms/forge/src/main/resources/pack.mcmeta` holds the content above, laid out like the
`agents` repository's copy; `processResources` expands only `META-INF/mods.toml`, so the
file reaches the jar root unchanged. `wiki/information/modules.md` lists it in the Forge
module, and the repository map and the `minecraft` comment in `gradle/libs.versions.toml`
say its formats follow Forge's own and move with the Minecraft version. NeoForge and Fabric
are unchanged. The same content in a copy of the 0.5.1 Forge jar loaded a world in game (the
`agents` thread's check). A local build of the jar was blocked: Maven Central answered 429
while Gradle resolved Fabric API, so the Publish workflow's `./gradlew build` is the first
full build of this change.

### Task 3 — release/0.5.2

`project-version` is `0.5.2`, a patch: one resource file is added to the Forge jar. The 0.5.2
changelog and its logs-index row are new; the overview, the repository map, the repository
rule and the state file name 0.5.2, and the rule's `wiki/logs/` range runs to `0/5/2`.
Merging this branch is the release: the Publish workflow finds no `v0.5.2` tag, builds and
publishes the six modules, and tags the commit.

The local build got through Maven Central's rate limit after task 2 merged:
`./gradlew :platforms:forge:jar` on task 2's commit passes, and the jar holds `pack.mcmeta`
at its root, unchanged, beside `META-INF/mods.toml`.
