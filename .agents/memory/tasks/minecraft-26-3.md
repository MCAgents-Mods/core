---
name: memory-tasks-minecraft-26-3
description: Building every loader jar for both Minecraft 26.2 and 26.3, one Gradle subproject per Minecraft version, and releasing 0.7.0 with it.
---

# Task: Minecraft 26.3, keeping 26.2

**Status:** in progress

## The request

On 2026-10-13 the owner wrote: "It seem minecraft 26.3 it out now. Make all mod support 26.3",
to start after the hologram work (0.6.0) and, by a later request, after the Release workflow
(0.6.1). Asked whether 26.2 should keep working, the owner picked "Also keep 26.2": every
loader jar is built twice, once per Minecraft version. agents, chat and the template follow in
their own pull requests, after core is published. The plan is
`plans/minecraft-26.3.md` in the project's shared files.

## What 26.3 needs

Every loader has a 26.3 build: Fabric Loader 0.19.5 with Fabric API 0.162.0+26.3, NeoForge
26.3.0.76 (its first stable build) and Forge 26.3-66.0.11 (latest; no 26.3 build is marked
recommended yet). Fabric Loom 1.18.4 is the first Loom for 26.3; it needs Gradle 9.7.0 or newer
and **Gradle itself running on Java 25**, where 1.17 ran on Java 21. Forge 26.3's `pack.mcmeta`
formats are 121 and 121, where 26.2's are 107 and [107, 1].

Compiling 0.6.0 against 26.3 breaks in four places, all in the hologram:

| 26.2 | 26.3 | Where |
|---|---|---|
| `InputConstants.Type.KEYSYM` | `InputConstants.Type.KEYBOARD` | the Z key, `HologramClient` |
| `PoseStack.mulPose(Quaternionfc)` | `PoseStack.rotate(Quaternionfc)` | turning panels, `OpenHologram` |
| `RenderTypes.textBackground()` with `submitCustomGeometry` | gone; `SubmitNodeCollector.submitTextBackground(...)` draws a coloured rectangle | `HologramPainter` |
| Forge `InputEvent.InteractionKeyMappingTriggered.BUS`, `isAttack()` | `InputEvent.InteractionKeyMappingTriggered.Attack.BUS` | `CoreForgeClient` |

Fabric's and NeoForge's hooks and the Forge `LevelRenderer.submitFeatures` mixin are unchanged.

## Decisions

- **One subproject per Minecraft version.** Every module that compiles against Minecraft
  (`platforms/client` and the three loaders) becomes a folder of shared sources with one
  subproject per version, `platforms/{module}/26.2` and `platforms/{module}/26.3`. One build
  checks, tests and publishes both. The other options were a `-Pminecraft` switch with one
  build per version, and the Stonecutter plugin. The owner was asked and the recommended
  option stands.
- **Only what differs lives in a version folder:** Forge's `pack.mcmeta`, and small classes of
  the same name in both versions that shared code calls without checking a version.
- **Each jar declares its own Minecraft version only**, as before: 26.4 will break things the
  way 26.3 did.
- **Jar names and published artifact ids carry the Minecraft version:**
  `MCAgentsCoreFabric-0.7.0+26.3.jar`, `mcagents-core-fabric-26.3`, `mcagents-core-client-26.3`.
  `api`, `common` and `shared` are built and published once.
- **Release 0.7.0**, a minor: the jar names and artifact ids change.

## Tasks

| # | Title | Branch | PR |
|---|---|---|---|
| 1 | Task record | `chore/minecraft-26-3-plan` | |
| 2 | Per-version layout, 26.2 only | `build/minecraft-versions` | |
| 3 | Minecraft 26.3 | `feat/minecraft-26-3` | |
| 4 | Release 0.7.0 | `release/0.7.0` | |

### Task 1 — chore/minecraft-26-3-plan

This record, and its row in the memory index.

### Task 2 — build/minecraft-versions

`platforms/client`, `fabric`, `neoforge` and `forge` each became a folder of shared sources with
one subproject per Minecraft version, `platforms/{module}/{minecraft}`, named
`:platforms:{module}-{minecraft}` (two projects both named `26.2` were one module to Gradle and
made a circular dependency). The versions are listed once, in `settings.gradle`, and only 26.2
is in it yet. `platforms/minecraft.gradle` takes the version from the folder name, looks up the
catalog entries suffixed with it (`fabric-api-26-2`), adds the folder's own `src/`, and names the
jar `{Name}-{version}+{minecraft}.jar`. The loader metadata is expanded per version. Forge's
`pack.mcmeta` moved into `platforms/forge/26.2/`. `release.yml` attaches one jar per loader and
Minecraft version.

Checked: `./gradlew build` passes; each loader's 26.2 jar holds the same files, byte for byte, as
the v0.6.1 release's jar; the published artifact ids are `mcagents-core-{api,common,shared}` and
`mcagents-core-{client,fabric,neoforge,forge}-26.2`; actionlint is clean on both workflows; the
release step's jar collection, run locally, finds three jars for v0.6.1 and fails for v0.6.0.
