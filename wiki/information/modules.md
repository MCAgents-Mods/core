# Modules

The build is a Gradle multi project defined in `settings.gradle`, with
`rootProject.name = core`. It has eight modules, and every package under them is
prefixed `io.github.mcagentsmods.core`. The four that compile against Minecraft
are built for the one Minecraft version core supports, the latest release; see
[The Minecraft version](#the-minecraft-version).

## The module graph

```
tool                pure Java tools: the registry, the worker pool, the executor
 └── api            pure Java contracts
      └── common    pure Java implementations

platforms/shared    loader-agnostic mod code, both physical sides
platforms/client    loader-agnostic client code against vanilla Minecraft
 ├── platforms/forge      Forge entry point     → MCAgentsCoreForge-{version}+{minecraft}.jar
 ├── platforms/neoforge   NeoForge entry point  → MCAgentsCoreNeoforge-{version}+{minecraft}.jar
 └── platforms/fabric     Fabric entry points   → MCAgentsCoreFabric-{version}+{minecraft}.jar
```

Each loader module depends on both `platforms/shared` and `platforms/client`, and
`platforms/client` compiles against `api` and `common`.

## What each module holds

| Module | Package | Purpose |
|---|---|---|
| `tool` | `…core.tool` | Tools in pure Java: what a tool is (`ToolSpec`, `ToolSchema`, `ToolArguments`, `ToolResult`), the registry every mod adds tools to, the worker pool that runs them, and the executor that runs one turn's calls. It compiles against the JDK alone, so plain Java can use it without the rest of core. See [`tools.md`](tools.md). |
| `api` | `…core.api` | Pure Java contracts: interfaces, records, enums, abstract types. `chat` and `llm` describe the per-vendor client, `model` the routed model client, `http` its transport, `token` the credential storage contract, `hologram` what a hologram shows, `agent` what a mod asks core's agent (`Agent`, `AgentRequest`, `AgentAnswer`), and `button` a button on the agents menu. No implementation, and no platform type anywhere. |
| `common` | `…core.common` | Pure Java implementations of those contracts — `common.model` holds `OpenRouterClient`, `common.http` the JDK transport, `common.hologram` the arithmetic that places, aims at, lays out and scrolls a hologram, and wraps and edits its text, and `common.agent` the agent layer: `Agents`, where a mod finds the shared tool registry, worker pool, menu buttons, the player's settings and an agent, and `ToolAgent`, which answers a prompt with the tools it needs. Still no platform type. Its `testFixtures` source set holds `StubTransport` and `StubKeySource`. |
| `platforms:shared` | `…core.shared` | The code every loader shares: `CoreRuntime` (start and stop), the machinery that decides which physical side is running, and the two sides as packages — `…shared.client` and `…shared.server`. Plain Java, no loader toolchain. |
| `platforms:client` | `…core.client` | What core draws in the game and the input that drives it: `…client.hologram` draws the open hologram, takes the player's clicks, wheel and move key, and closes it out of range, and `Holograms` is what a mod calls; `…client.agent` is the agents menu, the G key that shows it, its settings screen, the start-up that loads the player's settings, and `ClientThreadTools`. Compiled against vanilla Minecraft through Loom; no loader type. Client only. |
| `platforms:forge` | `…core.forge` | Only what Forge needs: the `@Mod` class, `META-INF/mods.toml`, and `pack.mcmeta` (in the Minecraft version's folder), without which Forge refuses the jar's resources and crashes when a world is created. NeoForge and Fabric build that metadata themselves. |
| `platforms:neoforge` | `…core.neoforge` | Only what NeoForge needs: the `@Mod` class and `META-INF/neoforge.mods.toml`. |
| `platforms:fabric` | `…core.fabric` | Only what Fabric needs: the main and client entry points and `fabric.mod.json`. |

## The Minecraft version

Core supports **only the latest Minecraft release, today 26.3**, and never an older one
beside it. `platforms/client` and the three loader modules compile against Minecraft, so
each is built for that version. The version is listed once, in `settings.gradle`. Each
of those modules is a folder of shared sources with a Gradle subproject for the version:

```
platforms/fabric/
├── fabric.gradle        the module's build, applied by the version subproject
├── src/                 the sources that do not depend on the Minecraft version
└── 26.3/                the subproject :platforms:fabric
    ├── build.gradle     applies the loader plugin, then ../fabric.gradle
    └── src/             only what is particular to 26.3, often nothing
```

The subproject's folder is named for its version, and the subproject for its module
alone: `:platforms:fabric`, so no project path or artifact id carries the version. It compiles the shared `src/` together with its
own, and `platforms/minecraft.gradle`, which the version subproject applies, takes the
version from the folder's name, finds its coordinates in the catalog (each entry carries
the version as a suffix, such as `fabric-api-26-3`), and names the jar
`MCAgentsCoreFabric-{version}+{minecraft}.jar`, so the jar name keeps the game's version.
The loader metadata (`fabric.mod.json`, `mods.toml`, `neoforge.mods.toml`) is expanded
from the version, so each jar declares that Minecraft version and no other.

Code whose Minecraft API changes between releases is a small class in the version
folder, which the shared code calls without ever checking the version: the hologram's
`RectangleSubmitter` and Forge's `AttackListener`. Forge's `pack.mcmeta`, whose formats
move with Minecraft, lives in the version folder too.

Moving to a new Minecraft release replaces the version: the entry in `settings.gradle`,
the catalog block, and each module's version folder are renamed and updated, and the old
version is not kept. Core 0.7.0 built for 26.2 and 26.3 side by side; from 0.8.0 core
builds for 26.3 alone.

`tool`, `api`, `common` and `platforms:shared` name no Minecraft type, so they are built once.

## One jar per loader

Each loader module builds its own mod jar, and **the jars are never merged**: a
loader never loads another loader's classes, and a merged jar would carry two
loaders' worth of dead code into every game. All three declare the mod id
`mcagents_core`.

Each jar carries the classes of `tool`, `api`, `common`, `platforms:shared` and
`platforms:client` merged in beside its own entry point, so it is self-contained.
None of those five names a loader type, so they are merged rather than nested as
separate mods. Gson is not bundled: Minecraft puts it on the classpath for every
loader. The Forge jar also names its mixin configuration in its manifest, as
`MixinConfigs`.

## Why the mod side has two physical sides

A mod runs in two physically different places — a client with a window and a
player in front of it, and a dedicated server with neither — and the classes
that exist differ between them. Client-only code reached on a dedicated server
is not a logic error; it is a `NoClassDefFoundError` that takes the server down.

So `platforms:shared` splits by **physical side** rather than by loader: the
`…shared.client` and `…shared.server` packages never import each other, and
nothing else imports either of them — `SideSeparationTest` fails the build if
that changes. The loaders sit beside `platforms:shared` and differ only in how
they announce an entry point, which is a much smaller difference than the one
between the two sides.

`platforms:client` is client only as a whole. Each loader reaches it only from
its own client half, so a dedicated server never links it.

How the right half is started without linking the other is on
[`mod-sides.md`](mod-sides.md).

## Toolchains

| Module | Gradle plugin | Loader coordinate |
|---|---|---|
| `platforms:forge` | ForgeGradle (`net.minecraftforge.gradle`) | `net.minecraftforge:forge` |
| `platforms:neoforge` | ModDevGradle (`net.neoforged.moddev`) | `net.neoforged:neoforge` |
| `platforms:fabric` | Fabric Loom (`net.fabricmc.fabric-loom`) | `net.fabricmc:fabric-loader`, `net.fabricmc.fabric-api:fabric-api` |
| `platforms:client` | Fabric Loom (`net.fabricmc.fabric-loom`) | None: vanilla Minecraft only |

The three plugins are declared once in the root `build.gradle` with
`apply false`, so they share one build classloader, and each version subproject
applies its own. Core builds for **Minecraft 26.3**, the latest release. Minecraft 26.x ships
unobfuscated, so no module declares a mappings coordinate.

`tool`, `api`, `common` and `platforms:shared` use none of these toolchains. That is
what keeps them testable without a game running. `platforms:client` uses Loom
only to compile against the game; the hologram's arithmetic lives in
`common.hologram` so that it is tested without one.

## Published artifacts

Only the library modules carry a publication: `tool`, `api`, `common`, `shared` and `client`. The
three loader modules build their mod jars but publish nothing to GitHub Packages; the Release
workflow attaches those jars to each version's GitHub release instead. The jar a module builds is named `MCAgentsCore{Module}-{version}.jar`, with
`+{minecraft}` before `.jar` for a Minecraft-facing one; its Maven coordinates are:

| Module | Jar base name | Coordinates |
|---|---|---|
| `tool` | `MCAgentsCoreTool` | `io.github.mcagentsmods:core-tool` |
| `api` | `MCAgentsCoreApi` | `io.github.mcagentsmods:core-api` |
| `common` | `MCAgentsCoreCommon` | `io.github.mcagentsmods:core-common` |
| `platforms/shared` | `MCAgentsCoreShared` | `io.github.mcagentsmods:core-shared` |
| `platforms/client/{minecraft}` | `MCAgentsCoreClient` | `io.github.mcagentsmods:core-client` |
| `platforms/forge/{minecraft}` | `MCAgentsCoreForge` | Not published: attached to the GitHub release |
| `platforms/neoforge/{minecraft}` | `MCAgentsCoreNeoforge` | Not published: attached to the GitHub release |
| `platforms/fabric/{minecraft}` | `MCAgentsCoreFabric` | Not published: attached to the GitHub release |

`{minecraft}` is the Minecraft version, `26.3`; it is in the jar's name but not in any
artifact id. Before 0.7.0 these four ids had no version suffix either and were built for
Minecraft 26.2 only; 0.7.0 published a `-26.2` and a `-26.3` id for each, side by side, and
from 0.8.0 the ids are those above again, built for 26.3. Up to 0.8.0 the loader modules were
published too, as `mcagents-core-{loader}`, and every artifact id started with `mcagents-`, as
in `mcagents-core-api`; from 0.9.0 on the ids are those above.

`core-common` also publishes its test fixtures as a variant, so a consumer can
test against `StubTransport` and `StubKeySource` with
`testFixtures('io.github.mcagentsmods:core-common:{version}')`, from 0.9.0 on (0.5.1 to
0.8.0 answer the same notation as `mcagents-core-common`).
0.5.0 published the fixtures under the capability `io.github.mcagentsmods:common-test-fixtures`
only, which that notation does not ask for. The fixtures are never bundled into a loader
jar.

The `artifactId` is qualified with the root project name deliberately. Without it,
every project in this group would publish a colliding `io.github.mcagentsmods:api`,
and a consumer could not depend on `core` and another mod at the same time. The group
already says MCAgents, so the id does not repeat it.

`core-client` declares no dependency:
Minecraft comes from the consumer's own toolchain. A mod compiles against
`core-api` and `core-common`, and the
`core-client`, built for the Minecraft version core supports, to open
holograms. A development run that needs core loaded takes the loader jar from core's GitHub
release, or builds it from a core checkout.

## Dependency versions

Every coordinate lives in `gradle/libs.versions.toml`, so a version bump is one
change in one file. The entries that move with Minecraft come in one block for the
supported version, suffixed with it (`-26-3` for 26.3):

| Catalog entry | Coordinate | Used by |
|---|---|---|
| `gson` | `com.google.code.gson:gson` | `common`, `platforms:shared` (compileOnly) |
| `fabric-loader` | `net.fabricmc:fabric-loader` | `platforms:fabric` |
| `minecraft-{v}` | `com.mojang:minecraft` | `platforms:fabric`, `platforms:client` |
| `fabric-api-{v}` | `net.fabricmc.fabric-api:fabric-api` | `platforms:fabric` |
| `neoforge-{v}` (version only) | `net.neoforged:neoforge` | `platforms:neoforge` |
| `forge-{v}` (version only) | `net.minecraftforge:forge` | `platforms:forge` |

## Java version

Every module compiles on **Java 25**, set once in the root `build.gradle` through
a Gradle toolchain. See [`../environments/setup.md`](../environments/setup.md).
