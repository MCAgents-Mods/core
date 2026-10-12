# Mod Runtime

How core starts inside a game. Each loader's entry point calls `CoreRuntime.start(side,
gameDirectory)` once and `CoreRuntime.stop()` at shutdown; core then starts the client half or
the dedicated-server half, and never links the other. A mod built on core does not call any
of this, because the core mod does it. Read this page to write a loader entry point, or to
check which side code is running on. Everything here is in `mcagents-core-shared`, under
`io.github.mcagentsmods.core.shared`.

```java
// A loader's client entry point, as core's Fabric one does it
CoreRuntime.start(PhysicalSide.CLIENT, FabricLoader.getInstance().getGameDir());
ClientLifecycleEvents.CLIENT_STOPPING.register(client -> CoreRuntime.stop());
```

Why the two halves exist, and how one is started without linking the other:
[`../information/mod-sides.md`](../information/mod-sides.md).

## CoreRuntime

| Member | Meaning |
|---|---|
| `MOD_ID` | `mcagents_core`, the id every loader manifest declares. |
| `start(PhysicalSide, Path gameDirectory)` | Installs the side, then starts the matching half. Idempotent: a second call returns the half already running. |
| `stop()` | Stops the running half and closes the [web console](web-console.md), keeping its registrations. Safe to call more than once. |
| `running()` | The running half, or `null` before `start` and after `stop`. |

Both are synchronized, and called from a loader's start-up and shutdown threads, never from a
render or tick loop.

## Which side is running

`…shared.environment`

| Type | Meaning |
|---|---|
| `PhysicalSide` | `CLIENT` (`client`) or `DEDICATED_SERVER` (`dedicated_server`). `isClient()`, `isDedicatedServer()`, and `fromName(String)`, which also accepts `server`. |
| `ModEnvironment` | `current()`, `isClient()`, `isDedicatedServer()`. The answer comes from what the loader said through `install(side)`, else the `mcagents.side` system property, else a probe for the class `net.minecraft.client.main.Main`. An unrecognised classpath reads as a dedicated server. |
| `SideGuard` | `requireClient(feature)` and `requireDedicatedServer(feature)` throw `WrongSideException` naming the feature and both sides, instead of a `NoClassDefFoundError` later. A diagnostic, not a security boundary. |
| `WrongSideException` | An `IllegalStateException`; `required()` and `actual()` name the sides. |
| `@ClientOnly`, `@ServerOnly` | Loader-neutral markers for a class that belongs to one side. Marking a class does not make it safe; keeping it in its side's package does. |

## The two halves

| Type | Meaning |
|---|---|
| `SideEntrypoint` | What a half implements: `side()`, `start(ModContext)`, `stop()`, `describe()`. |
| `ModContext(side, gameDirectory, logger, requestTimeout)` | What a half starts with. `ModContext.of(side, gameDirectory, logger)` uses the 60-second `LlmCredentials.DEFAULT_TIMEOUT`. |
| `ModBootstrap` | Loads a half by class name, so a dedicated server never links a client class. `start(ModContext)` loads and starts; `entrypointFor(side)` gives the name, `CLIENT_ENTRYPOINT` or `DEDICATED_SERVER_ENTRYPOINT`. |
| `…shared.client.ClientEntrypoint` | The client half. Opens the shared key file under the game directory and the [token commands](token-commands.md) over it. |
| `…shared.server.ServerEntrypoint` | The server half. Opens `config/mcagents.json` under the server's run directory, and checks every command against permission level 4. |

The `client` and `server` packages never import each other, and nothing else in the module
imports either; `SideSeparationTest` fails the build otherwise. Where each half keeps its key
file: [`key-file.md`](key-file.md).
