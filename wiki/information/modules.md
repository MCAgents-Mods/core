# Modules

The build is a Gradle multi project defined in `settings.gradle`, with
`rootProject.name = core`. It has six modules, and every package under them is
prefixed `io.github.mcagentsmods.core`.

## The module graph

```
api                 pure Java contracts
 └── common         pure Java implementations

platforms/shared    loader-agnostic mod code, both physical sides
 ├── platforms/forge      Forge entry point     → MCAgentsCoreForge-{version}.jar
 ├── platforms/neoforge   NeoForge entry point  → MCAgentsCoreNeoforge-{version}.jar
 └── platforms/fabric     Fabric entry points   → MCAgentsCoreFabric-{version}.jar
```

## What each module holds

| Module | Package | Purpose |
|---|---|---|
| `api` | `…core.api` | Pure Java contracts: interfaces, records, enums, abstract types. `chat` and `llm` describe the per-vendor client, `model` the routed model client, `http` its transport, `web` the web console's registry, and `token` the credential storage contract. No implementation, and no platform type anywhere. |
| `common` | `…core.common` | Pure Java implementations of those contracts — `common.model` holds `OpenRouterClient`, `common.http` the JDK transport, and `common.web` the loopback web console with the page shell it serves from `mcagents_core/web/`. Still no platform type. Its `testFixtures` source set holds `StubTransport` and `StubKeySource`. |
| `platforms:shared` | `…core.shared` | The code every loader shares: `CoreRuntime` (start and stop), the machinery that decides which physical side is running, and the two sides as packages — `…shared.client` and `…shared.server`. Plain Java, no loader toolchain. |
| `platforms:forge` | `…core.forge` | Only what Forge needs: the `@Mod` class and `META-INF/mods.toml`. |
| `platforms:neoforge` | `…core.neoforge` | Only what NeoForge needs: the `@Mod` class and `META-INF/neoforge.mods.toml`. |
| `platforms:fabric` | `…core.fabric` | Only what Fabric needs: the main and client entry points and `fabric.mod.json`. |

## One jar per loader

Each loader module builds its own mod jar, and **the jars are never merged**: a
loader never loads another loader's classes, and a merged jar would carry two
loaders' worth of dead code into every game. All three declare the mod id
`mcagents_core`.

Each jar carries the classes of `api`, `common` and `platforms:shared` merged in
beside its own entry point, so it is self-contained. Those three modules are
plain Java, so they are merged rather than nested as separate mods. Gson is not
bundled: Minecraft puts it on the classpath for every loader.

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

How the right half is started without linking the other is on
[`mod-sides.md`](mod-sides.md).

## Toolchains

| Module | Gradle plugin | Loader coordinate |
|---|---|---|
| `platforms:forge` | ForgeGradle (`net.minecraftforge.gradle`) | `net.minecraftforge:forge` |
| `platforms:neoforge` | ModDevGradle (`net.neoforged.moddev`) | `net.neoforged:neoforge` |
| `platforms:fabric` | Fabric Loom (`net.fabricmc.fabric-loom`) | `net.fabricmc:fabric-loader`, `net.fabricmc.fabric-api:fabric-api` |

The three plugins are declared once in the root `build.gradle` with
`apply false`, so they share one build classloader. Every loader targets
**Minecraft 26.2**. Minecraft 26.x ships unobfuscated, so no module declares a
mappings coordinate.

`api`, `common` and `platforms:shared` use none of these toolchains. That is
what keeps them testable without a game running.

## Published artifacts

Every module carries a publication. The jar a module builds is named
`MCAgentsCore{Module}-{version}.jar`; its Maven coordinates are:

| Module | Jar base name | Coordinates |
|---|---|---|
| `api` | `MCAgentsCoreApi` | `io.github.mcagentsmods:mcagents-core-api` |
| `common` | `MCAgentsCoreCommon` | `io.github.mcagentsmods:mcagents-core-common` |
| `platforms/shared` | `MCAgentsCoreShared` | `io.github.mcagentsmods:mcagents-core-shared` |
| `platforms/forge` | `MCAgentsCoreForge` | `io.github.mcagentsmods:mcagents-core-forge` |
| `platforms/neoforge` | `MCAgentsCoreNeoforge` | `io.github.mcagentsmods:mcagents-core-neoforge` |
| `platforms/fabric` | `MCAgentsCoreFabric` | `io.github.mcagentsmods:mcagents-core-fabric` |

`mcagents-core-common` also publishes its test fixtures as a variant, so a consumer can
test against `StubTransport` and `StubKeySource` with
`testFixtures('io.github.mcagentsmods:mcagents-core-common:{version}')`. The fixtures
are never bundled into a loader jar.

The `artifactId` is qualified with `mcagents-` and the root project name
deliberately. Without it, every project in this group would publish a colliding
`io.github.mcagentsmods:api`, and a consumer could not depend on `core` and
another mod at the same time.

A loader module publishes its mod jar alone, with no dependency list: the jar
already carries `api`, `common` and `shared`, and the loader supplies the rest.
A mod compiles against `mcagents-core-api` and `mcagents-core-common`; the
loader artifacts are for a development run that needs core loaded.

## Dependency versions

Every coordinate lives in `gradle/libs.versions.toml`, so a version bump is one
change in one file:

| Catalog entry | Coordinate | Used by |
|---|---|---|
| `gson` | `com.google.code.gson:gson` | `common`, `platforms:shared` (compileOnly) |
| `minecraft` | `com.mojang:minecraft` | `platforms:fabric` |
| `fabric-loader`, `fabric-api` | Fabric | `platforms:fabric` |
| `neoforge` (version only) | `net.neoforged:neoforge` | `platforms:neoforge` |
| `forge` (version only) | `net.minecraftforge:forge` | `platforms:forge` |

## Java version

Every module compiles on **Java 25**, set once in the root `build.gradle` through
a Gradle toolchain. See [`../environments/setup.md`](../environments/setup.md).
