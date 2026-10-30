---
name: logs-index
description: Index of versioned change logs under wiki/logs/ — one row per version directory, newest first.
---

# Logs Index

**Scope:** `wiki/logs/`
**Parent:** [project-wiki-index](project-wiki-index.md)

Every version directory is `wiki/logs/{Major}/{Minor}/{Patch}/`, numeric segments
only — no `v` prefix, no zero padding. A version directory holds `CHANGELOG.md` by
default, and may hold `MIGRATION.md`, `BREAKING.md`, `UPGRADE.md`, or `NOTES.md`
beside it.

**Creating a new version directory is a version claim** and requires user approval —
see `{shared}/rules/versioning.md`.

## Versions

| Version | Summary | Files |
|---|---|---|
| [`0/10/0/`](../../wiki/logs/0/10/0/CHANGELOG.md) | The agents mod folded in: the `tool` module, the agent, the agents menu on G and its settings, under core's packages; `mcagents_agents` refused. | `CHANGELOG.md` |
| [`0/10/1/`](../../wiki/logs/0/10/1/CHANGELOG.md) | Typing into a hologram's prompt works on Minecraft 26.3: the typing screen asks the game for text input. | `CHANGELOG.md` |
| [`0/11/0/`](../../wiki/logs/0/11/0/CHANGELOG.md) | Several holograms stand at once, up to 8, each aimed, carried and closed on its own; new `PickList` panel and buttons under a `Transcript`. | `CHANGELOG.md` |
| [`0/11/1/`](../../wiki/logs/0/11/1/CHANGELOG.md) | Build only: the loader modules compile with `compileOnly` (plus a `runtimeOnly` for the dev client) instead of `implementation`; the jars are unchanged. | `CHANGELOG.md` |
| [`0/12/0/`](../../wiki/logs/0/12/0/CHANGELOG.md) | Pages with fields as holograms: the `Form` panel and its rows, typing into any box, `Hologram.onClose`, and SETTINGS as a Settings hologram. | `CHANGELOG.md` |
| [`0/13/0/`](../../wiki/logs/0/13/0/CHANGELOG.md) | Each request goes to the provider the player picked, at its own address; `ModelChoice`, `ModelProvider`, and the global model (`GlobalModels`, `Models`) every mod falls back to. | `CHANGELOG.md` |
| [`0/13/1/`](../../wiki/logs/0/13/1/CHANGELOG.md) | Build only: the loader modules declare core's own modules with `implementation`, while Fabric Loader, Fabric API and Forge stay `compileOnly`; the jars are unchanged. | `CHANGELOG.md` |
| [`0/14/0/`](../../wiki/logs/0/14/0/CHANGELOG.md) | Every file core keeps is in `MCAgents/core/`: settings, the global model and the key file, a server's too, moved there from their old places on first start; `ModFiles` moves any mod's files the same way. | `CHANGELOG.md` |
| [`0/15/0/`](../../wiki/logs/0/15/0/CHANGELOG.md) | `/agents` on dedicated servers, hidden from and refused to everyone below permission level 4; the token rotation and the form drop-down split into classes of their own. | `CHANGELOG.md` |
| [`0/13/2/`](../../wiki/logs/0/13/2/CHANGELOG.md) | Build only: each platform module is one folder with one `build.gradle` and one `src/`, with no `26.3/` subproject, and the catalog names the Minecraft version; the jars are unchanged. | `CHANGELOG.md` |
| [`0/9/0/`](../../wiki/logs/0/9/0/CHANGELOG.md) | Artifact ids drop the repeated `mcagents-`, as in `core-api`, and only the library modules are published; the loader jars go to GitHub releases only. | `CHANGELOG.md` |
| [`0/8/0/`](../../wiki/logs/0/8/0/CHANGELOG.md) | Only the latest Minecraft release, 26.3: the 26.2 jars are gone and artifact ids no longer carry the Minecraft version. | `CHANGELOG.md` |
| [`0/7/0/`](../../wiki/logs/0/7/0/CHANGELOG.md) | Minecraft 26.3 beside 26.2: every loader jar built once per Minecraft version, with the version in jar names and artifact ids. | `CHANGELOG.md` |
| [`0/6/1/`](../../wiki/logs/0/6/1/CHANGELOG.md) | The Release workflow attaches the Forge, Fabric and NeoForge jars to each version's GitHub release; no code changes. | `CHANGELOG.md` |
| [`0/6/0/`](../../wiki/logs/0/6/0/CHANGELOG.md) | Holograms: panels a mod opens in the world, aimed at and clicked, carried with Z and gone beyond two chunks; the web console is removed. | `CHANGELOG.md` |
| [`0/5/3/`](../../wiki/logs/0/5/3/CHANGELOG.md) | The API documentation moves to `wiki/api/`, one page per scope; no code changes. | `CHANGELOG.md` |
| [`0/5/2/`](../../wiki/logs/0/5/2/CHANGELOG.md) | The Forge jar carries `pack.mcmeta`, so Forge 26.2 loads core's resources and creates worlds. | `CHANGELOG.md` |
| [`0/5/1/`](../../wiki/logs/0/5/1/CHANGELOG.md) | `mcagents-core-common`'s test fixtures resolve from the published artifacts; nothing in the jars changes. | `CHANGELOG.md` |
| [`0/5/0/`](../../wiki/logs/0/5/0/CHANGELOG.md) | Core becomes the mods foundation: Bukkit removed, one jar per loader, the shared model client and web console, and the Publish workflow that ships it. | `CHANGELOG.md` |
| [`0/4/0/`](../../wiki/logs/0/4/0/CHANGELOG.md) | The request timeout becomes configurable, and stays the only timeout in MCAgents. | `CHANGELOG.md` |
| [`0/3/0/`](../../wiki/logs/0/3/0/CHANGELOG.md) | The command becomes /agents and can add or revoke tokens in game, with masked handles in tab completion. | `CHANGELOG.md` |
| [`0/2/0/`](../../wiki/logs/0/2/0/CHANGELOG.md) | Credentials move into core entirely: its own config.yml, the shared mcagents.json, and the /mcagents command. | `CHANGELOG.md` |
| [`0/1/0/`](../../wiki/logs/0/1/0/CHANGELOG.md) | First functional release: the agent API, four vendors, a loadable Bukkit plugin, and credential pooling. | `CHANGELOG.md` |
