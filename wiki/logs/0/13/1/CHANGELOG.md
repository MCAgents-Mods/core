# 0.13.1

Released: 2026-10-17

A build change only: each loader module now declares core's own modules with `implementation`,
and what the game provides stays `compileOnly`. The loader jars carry exactly what 0.13.0's did.

## Upgrading

- **Players: nothing to do.** The `v0.13.1` release carries
  `MCAgentsCoreForge-0.13.1+26.3.jar`, `MCAgentsCoreFabric-0.13.1+26.3.jar` and
  `MCAgentsCoreNeoforge-0.13.1+26.3.jar`, with the same contents as 0.13.0's; replacing the jar
  is optional. Mods built on core need no update.
- **Nothing to change in a build.** No API changes, and the published modules' dependencies are
  unchanged.

## Changed

- **The loader modules implement core's own modules.** Fabric, Forge and NeoForge declare
  `tool`, `api`, `common`, `shared` and `client` with `implementation`, since each is part of the
  mod the jar ships, in place of the `compileOnly` and `runtimeOnly` pair 0.11.1 introduced.
  Fabric Loader, Fabric API and Forge stay `compileOnly`, because the game provides them, with
  the `runtimeOnly` line the development client needs. What a jar carries is still decided only
  by its `jar` task, which merges core's own modules. The loader modules are never published, so
  neither scope reaches anything else.
