# 0.11.1

Released: 2026-10-17

A build change only: no Gradle file in core declares `implementation` any more. The loader
jars carry exactly what 0.11.0's did.

## Upgrading

- **Players: nothing to do.** The `v0.11.1` release carries
  `MCAgentsCoreForge-0.11.1+26.3.jar`, `MCAgentsCoreFabric-0.11.1+26.3.jar` and
  `MCAgentsCoreNeoforge-0.11.1+26.3.jar`, with the same contents as 0.11.0's; replacing the jar
  is optional. Mods built on core need no update.
- **Nothing to change in a build.** No API changes, and the published modules' dependencies are
  unchanged.

## Changed

- **The loader modules compile with `compileOnly`.** Fabric, Forge and NeoForge declare core's
  own modules (`tool`, `api`, `common`, `shared`, `client`) and their loader (Fabric Loader and
  Fabric API, or Forge) as `compileOnly`, each with a `runtimeOnly` twin so the development
  client still finds them. What a jar carries is decided only by its `jar` task, which merges
  core's own modules, never by a configuration. The loader modules are never published, so
  `runtimeOnly` reaches the development client and nothing else.
