# 0.14.0

Released: 2026-10-22

Every file core keeps on a player's disk is now in its own folder, `MCAgents/core/`, as every
MCAgents mod keeps its files in `MCAgents/{mod}/`. The first start after the update moves the
old files there, so no setting, model or key is lost.

## Upgrading

- **Players: replace the core jar.** The `v0.14.0` release carries
  `MCAgentsCoreForge-0.14.0+26.3.jar`, `MCAgentsCoreFabric-0.14.0+26.3.jar` and
  `MCAgentsCoreNeoforge-0.14.0+26.3.jar`; install the one for your loader. On the first start
  core moves `MCAgents/agents/settings.properties` and `MCAgents/agents/models.properties` into
  `MCAgents/core/`, removes the emptied `agents/` folder, and moves `mcagents.json` from the
  game directory into `MCAgents/core/`. A file already in `MCAgents/core/` is never
  overwritten; its old copy is then left where it was. Chat 0.15.0 needs this release.
- **Server owners:** the key file moves from `config/mcagents.json` to
  `MCAgents/core/mcagents.json` under the server's folder, the same way, on the first start.
- **`MCAGENTS_DIR` still names the Minecraft directory;** the key file is now
  `$MCAGENTS_DIR/MCAgents/core/mcagents.json`.
- **Mods built on core:** `AgentsPaths.AGENTS_DIRECTORY` is gone; use `MOD_DIRECTORY` (`core`),
  or `LEGACY_DIRECTORY` (`agents`) for the old place. A mod that calls `Models.start` before
  core's start-up should call `AgentsPaths.under(gameDirectory).moveLegacyFiles()` first.

## Added

- **`ModFiles`** in `core.common.file`: `moveIfAbsent(legacy, target)` moves a file or a
  directory into a mod's own folder when it is not there yet, and `deleteIfEmpty(directory)`.
  Any mod moving its files into `MCAgents/{mod}/` uses it. See
  [`wiki/api/mod-runtime.md`](../../../../api/mod-runtime.md).
- **`AgentsPaths.moveLegacyFiles()`** and **`legacyDirectory()`**, which `AgentsRuntime.start`
  calls before loading the settings and the global model.
- **`MinecraftDirectory.legacyFile`**, **`ServerRunDirectory.legacyFile`** and
  **`SharedTokenStore.moveLegacyFile`**, which the client and server halves call on start.

## Changed

- **`AgentsPaths`** resolves `{game_dir}/MCAgents/core/`; `MOD_DIRECTORY` replaces
  `AGENTS_DIRECTORY`.
- **The key file** is `MCAgents/core/mcagents.json` on both sides: `MinecraftDirectory.resolveFile`
  and `ServerRunDirectory.resolveFile`. `ServerRunDirectory.CONFIG_FOLDER` is now
  `LEGACY_FOLDER`.
- **`MinecraftDirectory.isUsable`** accepts a directory that does not exist yet when its nearest
  existing parent can be written, since `MCAgents/core/` is two levels deep.
