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
| [`0/5/1/`](../../wiki/logs/0/5/1/CHANGELOG.md) | `mcagents-core-common`'s test fixtures resolve from the published artifacts; nothing in the jars changes. | `CHANGELOG.md` |
| [`0/5/0/`](../../wiki/logs/0/5/0/CHANGELOG.md) | Core becomes the mods foundation: Bukkit removed, one jar per loader, the shared model client and web console, and the Publish workflow that ships it. | `CHANGELOG.md` |
| [`0/4/0/`](../../wiki/logs/0/4/0/CHANGELOG.md) | The request timeout becomes configurable, and stays the only timeout in MCAgents. | `CHANGELOG.md` |
| [`0/3/0/`](../../wiki/logs/0/3/0/CHANGELOG.md) | The command becomes /agents and can add or revoke tokens in game, with masked handles in tab completion. | `CHANGELOG.md` |
| [`0/2/0/`](../../wiki/logs/0/2/0/CHANGELOG.md) | Credentials move into core entirely: its own config.yml, the shared mcagents.json, and the /mcagents command. | `CHANGELOG.md` |
| [`0/1/0/`](../../wiki/logs/0/1/0/CHANGELOG.md) | First functional release: the agent API, four vendors, a loadable Bukkit plugin, and credential pooling. | `CHANGELOG.md` |
