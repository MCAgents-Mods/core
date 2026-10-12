# 0.5.2

Released: 2026-10-12

The Forge jar carries `pack.mcmeta`, so Forge 26.2 loads it. Nothing else changes.

## Upgrading

- **On Forge, use 0.5.2.** With 0.5.1 or 0.5.0 installed, Forge 26.2 warns at start-up that
  core has no valid resource pack and crashes when a world is created ("Can not retrieve
  LootModifierManager until resources have loaded once"). NeoForge and Fabric are not
  affected, and no API changes.

## Fixed

- `MCAgentsCoreForge` had no `pack.mcmeta`. Forge reads one from every mod jar and refuses
  the jar's resources when it is missing; the file now declares Forge 26.2's pack formats
  (`min_format` `[107, 1]`, `max_format` `107`). NeoForge and Fabric build that metadata
  themselves, so their jars still have none.
