# Modules

The build is a Gradle multi project defined in `settings.gradle`, with
`rootProject.name = core`. It has seven modules, and every package under
them is prefixed `io.github.mcagentsmods.core`.

## The module graph

```
api          pure Java contracts
 └── common  pure Java implementations

platforms/mods/core          shared mod code, both physical sides
 ├── platforms/mods/client       client-only half
 ├── platforms/mods/server       dedicated-server-only half
 ├── platforms/mods/neoforge     NeoForge entry point
 └── platforms/mods/fabric       Fabric entry point
```

## What each module holds

| Module | Package | Purpose |
|---|---|---|
| `api` | `…core.api` | Pure Java contracts: interfaces, records, enums, abstract types, and the `token` package holding the credential storage contract. No implementation, and no platform type anywhere. |
| `common` | `…core.common` | Pure Java implementations of those contracts. Still no platform type. |
| `platforms:mods:core` | `…core.mods` | The code every mod module shares, including the machinery that decides which physical side is running. |
| `platforms:mods:client` | `…core.mods.client` | The client-only half: the player's own credential file, the client commands, anything drawn on screen. |
| `platforms:mods:server` | `…core.mods.server` | The dedicated-server-only half: the server's credential file, and the checks that decide who may do what. |
| `platforms:mods:neoforge` | `…core.neoforge` | Only what NeoForge needs on top of `platforms:mods:core`. |
| `platforms:mods:fabric` | `…core.fabric` | Only what Fabric needs on top of `platforms:mods:core`. |

## Why the mod family has two sides

A mod runs in two physically different places — a client with a window and a
player in front of it, and a dedicated server with neither — and the classes
that exist differ between them. Client-only code reached on a dedicated server
is not a logic error; it is a `NoClassDefFoundError` that takes the server down.

So the mod family splits by **physical side** rather than by loader:
`platforms:mods:client` and `platforms:mods:server` never depend on each other,
and neither is named by type from anywhere else. The loaders — NeoForge and
Fabric — sit beside them and differ only in how they announce an entry point,
which is a much smaller difference than the one between the two sides.

How the right half is started without linking the other is on
[`mod-sides.md`](mod-sides.md).

## Why the dependencies are `compileOnly`

Every platform module declares `api`, `common`, and `platforms:mods:core` as
**`compileOnly`**. They compile against those classes but never bundle them, so
the mod jar that ships them carries exactly one copy of each class instead of one
copy per module that happened to depend on it.

## Published artifacts

Only `api` and `common` carry a publication:

| Module | Coordinates |
|---|---|
| `api` | `io.github.mcagentsmods:core-api` |
| `common` | `io.github.mcagentsmods:core-common` |

The `artifactId` is qualified with the root project name deliberately. Without
it, every MCAgents project in this group would publish a colliding
`io.github.mcagentsmods:api`, and a consumer could not depend on `core` and another
project at the same time.

The platform modules are distributed as mod artifacts rather than libraries, so
they are not published to a Maven repository.

## Dependency versions

Coordinates live in `gradle/libs.versions.toml`, so a version bump is one change
in one file:

| Catalog entry | Coordinate | Used by |
|---|---|---|
| `gson` | `com.google.code.gson:gson` | `common`, the mod family |

Gson is `compileOnly` because Minecraft itself puts it on the classpath for every
mod loader, so no jar here carries a copy of it.

No module under `platforms/mods` declares a **mod loader coordinate yet**.
Resolving `net.neoforged:neoforge` or the Fabric loader requires a toolchain
(ModDevGradle, Loom) that remaps Minecraft as part of the build. Those are
introduced together with the first real loader code rather than scaffolded
ahead of it. Everything the mod family holds today is plain Java, which is also
what keeps it testable without a game running.

## Java version

Every module compiles on **Java 25**, set once in the root `build.gradle` through
a Gradle toolchain. See [`../environments/setup.md`](../environments/setup.md).
