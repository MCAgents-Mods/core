# Mod Runtime

How core starts inside a game. Each loader's entry point calls `CoreRuntime.start(side,
gameDirectory)` once and `CoreRuntime.stop()` at shutdown; core then starts the client half or
the dedicated-server half, and never links the other. A mod built on core does not call any
of this, because the core mod does it. Read this page to write a loader entry point, or to
check which side code is running on. Everything here is in `core-shared`, under
`io.github.mcagentsmods.core.shared`, except the hologram's hooks, which are in
`core-client`.

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
| `stop()` | Stops the running half. Safe to call more than once. |
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

## The hologram's hooks

`…core.client.hologram.HologramClient`, in `core-client`. A loader's client entry
point connects its own events to these; a mod opens holograms through
[`Holograms`](hologram.md) instead. Every method runs on the client thread.

| Member | When the loader calls it |
|---|---|
| `KEY_CATEGORY` | The id of the *MCAgents Core* category in *Controls*, `mcagents_core:main`. The loader registers the category its own way. |
| `createMoveKey(KeyMapping.Category)` | First thing in client start-up, before `CoreRuntime.start`: every loader refuses a key once the game's options exist. Returns the move key, Z, for the loader to register. |
| `onEndTick(Minecraft)` | At the end of every client tick. Carries, lets go of or closes the open hologram. |
| `submit(LevelRenderState, SubmitNodeCollector)` | Once a frame, while the level's features are gathered. Draws the open hologram. |
| `onAttack()` | When the attack key fires. `true` means the hologram took the click, and the loader cancels the attack and the arm swing. |
| `onScroll(double)` | When the mouse wheel would change the hotbar slot, with the wheel's vertical movement. `true` means the hologram took it, and the loader leaves the slot alone. |

| Loader | Where its client half starts | Drawing hook |
|---|---|---|
| Fabric | `CoreFabricClient`, the `client` entry point | `LevelRenderEvents.COLLECT_SUBMITS` |
| NeoForge | `CoreNeoForgeClient`, `@Mod(dist = Dist.CLIENT)` | `SubmitCustomGeometryEvent` |
| Forge | `CoreForgeClient.start()`, called by `CoreForgeMod` on the client only | A mixin on `LevelRenderer.submitFeatures`, named in the jar's `MixinConfigs` manifest attribute, because Forge has no event for drawing in the level |

Each loader reaches `core-client` only from that client half, so a dedicated server
never links a client class.

## The agent layer's start-up

`…core.client.agent`, in `core-client`. Each loader's client half also registers the agents
menu's key and starts the agent layer, after `CoreRuntime.start`; a mod built on core calls
none of it. What the menu and the key do is on [`buttons.md`](buttons.md).

```java
// What each loader's client half does, beside the hologram's hooks:
KeyMappingHelper.registerKeyMapping(AgentsKeys.create(category));   // with the move key, first
ClientTickEvents.END_CLIENT_TICK.register(AgentsKeys::onEndTick);
AgentsRuntime.start(FabricLoader.getInstance().getGameDir());       // FMLPaths.GAMEDIR on NeoForge and Forge
```

| Member | What it does |
|---|---|
| `AgentsKeys.create(KeyMapping.Category)` | Builds the key that shows the agents menu, `key.mcagents_core.agents`, bound to G, in core's category beside the move key. Called first in client start-up, for the same reason. |
| `AgentsKeys.onEndTick(Minecraft)` | Shows or hides the menu when the key was pressed. Called at the end of every client tick. |
| `AgentsRuntime.start(Path gameDirectory)` | Creates `{game_dir}/MCAgents/agents/`, then loads and applies the player's settings through `Agents.start` ([`settings.md`](settings.md)). Only the first call does anything. A failure at either step is logged and the game carries on, with the default settings if they could not be loaded. A `null` directory throws `NullPointerException`. |
| `AgentsRuntime.paths()` | `Optional<AgentsPaths>`: the layout on disk, once `start` has run; empty before. |

`AgentsPaths`, in `core-common` under `…core.common.agent`, is that layout. It knows nothing
about Minecraft, so a test hands it a temporary directory.

| Member | What it is |
|---|---|
| `DATA_DIRECTORY` | `MCAgents`, the directory every MCAgents mod keeps its files under, inside the game directory. |
| `AGENTS_DIRECTORY` | `agents`, the agent layer's own directory inside it. The agents mod used the same one before it became part of core, so a player's settings carried over. |
| `AgentsPaths.under(Path gameDirectory)` | Static. Describes the layout under the game directory. Creates nothing; a `null` directory throws `NullPointerException`. |
| `dataDirectory()` | `{game_dir}/MCAgents/agents`. |
| `settingsFile()` | `{game_dir}/MCAgents/agents/settings.properties`, what the player set in game. |
| `createDataDirectory()` | Creates the data directory and its parents. Safe to repeat. Throws `IOException`. |
